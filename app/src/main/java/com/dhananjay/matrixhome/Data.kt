package com.dhananjay.matrixhome

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** One thing on the home screen: an app or a TV input. */
data class Tile(
    val key: String,
    val label: String,
    val intent: Intent,
    val icon: ImageBitmap?,
    val isBanner: Boolean,
    val isApp: Boolean,
    val vendor: Boolean = false,
)

class City(val name: String, val lat: Double, val lon: Double)

val Cities = listOf(
    City("Delhi", 28.61, 77.21), City("Mumbai", 19.08, 72.88), City("Bengaluru", 12.97, 77.59),
    City("Chennai", 13.08, 80.27), City("Kolkata", 22.57, 88.36), City("Hyderabad", 17.39, 78.48),
    City("Pune", 18.52, 73.86), City("Ahmedabad", 23.02, 72.57), City("Jaipur", 26.91, 75.79),
    City("Lucknow", 26.85, 80.95), City("Nagpur", 21.15, 79.09), City("Bhopal", 23.26, 77.41),
)

/** Look up cities by name (Open-Meteo geocoding, free, no key). */
fun geocode(query: String): List<City> = try {
    val q = java.net.URLEncoder.encode(query.trim(), "UTF-8")
    val c = URL("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=6&language=en&format=json")
        .openConnection() as HttpURLConnection
    c.connectTimeout = 8000; c.readTimeout = 8000
    val arr = JSONObject(c.inputStream.bufferedReader().readText()).optJSONArray("results")
    (0 until (arr?.length() ?: 0)).map {
        val o = arr!!.getJSONObject(it)
        val admin = o.optString("admin1", "")
        City(
            listOf(o.getString("name"), admin, o.optString("country_code", "")).filter { s -> s.isNotBlank() }
                .joinToString(", "),
            o.getDouble("latitude"), o.getDouble("longitude"),
        )
    }
} catch (_: Exception) { emptyList() }

/** Apps seeded into Favorites on first run (only those actually installed). */
val DefaultFavorites = listOf(
    "com.netflix.ninja", "com.google.android.youtube.tv", "in.startv.hotstar",
    "com.amazon.amazonvideo.livingroom", "org.smarttube.stable", "com.sonyliv",
    "com.crunchyroll.crunchyroid", "com.lagradost.cloudstream3", "com.android.vending",
)

private val vendorPrefixes = listOf(
    "com.haier.", "com.haierubic.", "com.mediatek.", "mediatek.", "com.cvte.",
    "com.at.factory", "com.telink.", "com.dolby.",
)
private val systemAllowList = setOf(
    "com.android.vending", "com.android.tv.settings", "com.google.android.play.games",
)

fun loadApps(ctx: Context): List<Tile> {
    val pm = ctx.packageManager
    val result = LinkedHashMap<String, Tile>()
    for (category in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
        val q = Intent(Intent.ACTION_MAIN).addCategory(category)
        for (ri in pm.queryIntentActivities(q, 0)) {
            val pkg = ri.activityInfo.packageName
            if (pkg == ctx.packageName || result.containsKey(pkg)) continue
            try {
                val banner = if (category == Intent.CATEGORY_LEANBACK_LAUNCHER) ri.activityInfo.loadBanner(pm) else null
                val bmp: Bitmap = (banner ?: ri.loadIcon(pm)).toBitmap(
                    if (banner != null) 256 else 128, if (banner != null) 144 else 128,
                )
                val flags = ri.activityInfo.applicationInfo.flags
                val pureSystem = (flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                    (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0 && pkg !in systemAllowList
                val vendor = pureSystem || vendorPrefixes.any { pkg.startsWith(it) }
                result[pkg] = Tile(
                    key = pkg,
                    label = ri.loadLabel(pm).toString(),
                    intent = Intent(Intent.ACTION_MAIN).addCategory(category)
                        .setClassName(pkg, ri.activityInfo.name),
                    icon = bmp.asImageBitmap(),
                    isBanner = banner != null,
                    isApp = true,
                    vendor = vendor,
                )
            } catch (_: Exception) { }
        }
    }
    return result.values.toList()
}

fun loadInputs(ctx: Context): List<Tile> {
    val out = mutableListOf<Tile>()
    try {
        val pm = ctx.packageManager
        val live = listOf("com.mediatek.wwtv.tvcenter", "com.google.android.tv").firstNotNullOfOrNull {
            pm.getLeanbackLaunchIntentForPackage(it) ?: pm.getLaunchIntentForPackage(it)
        }
        if (live != null) out += Tile("input:live", "Live TV", live, null, false, false)
        val tim = ctx.getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager
        tim.tvInputList.filter { it.isPassthroughInput }.map {
            val label = (it.loadCustomLabel(ctx) ?: it.loadLabel(ctx)).toString()
            Tile(
                "input:${it.id}", label,
                Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(it.id)),
                null, false, false,
            )
        }.sortedBy { it.label }.let { out += it }
    } catch (_: Exception) { }
    return out
}

/** Current temperature from Open-Meteo (free, no API key). Returns e.g. "31°C · Clear". */
fun fetchWeather(city: City): String? = try {
    val url = URL(
        "https://api.open-meteo.com/v1/forecast?latitude=${city.lat}&longitude=${city.lon}" +
            "&current=temperature_2m,weather_code&timezone=auto",
    )
    val c = url.openConnection() as HttpURLConnection
    c.connectTimeout = 8000; c.readTimeout = 8000
    val cur = JSONObject(c.inputStream.bufferedReader().readText()).getJSONObject("current")
    val code = cur.getInt("weather_code")
    val desc = when (code) {
        0 -> "Clear"; 1, 2, 3 -> "Partly cloudy"; 45, 48 -> "Fog"
        in 51..57 -> "Drizzle"; in 61..67 -> "Rain"; in 71..77 -> "Snow"
        in 80..82 -> "Showers"; in 95..99 -> "Thunderstorm"; else -> ""
    }
    "${cur.getDouble("temperature_2m").toInt()}°C" + if (desc.isNotEmpty()) " · $desc" else ""
} catch (_: Exception) { null }

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("matrix_home", Context.MODE_PRIVATE)
    var theme: Int
        get() = sp.getInt("theme", 0)
        set(v) { sp.edit().putInt("theme", v).apply() }
    var use24h: Boolean
        get() = sp.getBoolean("use24h", false)
        set(v) { sp.edit().putBoolean("use24h", v).apply() }
    var city: City
        get() = City(
            sp.getString("cityName", "Delhi") ?: "Delhi",
            sp.getFloat("cityLat", 28.61f).toDouble(), sp.getFloat("cityLon", 77.21f).toDouble(),
        )
        set(v) { sp.edit().putString("cityName", v.name).putFloat("cityLat", v.lat.toFloat())
            .putFloat("cityLon", v.lon.toFloat()).apply() }
    /** "auto", "grad:<Name>" or "img:<file name>" */
    var wallpaper: String
        get() = sp.getString("wallpaper", "auto") ?: "auto"
        set(v) { sp.edit().putString("wallpaper", v).apply() }
    var idleMin: Int
        get() = sp.getInt("idle", 5)
        set(v) { sp.edit().putInt("idle", v).apply() }
    var hideVendor: Boolean
        get() = sp.getBoolean("hideVendor", true)
        set(v) { sp.edit().putBoolean("hideVendor", v).apply() }
    var seeded: Boolean
        get() = sp.getBoolean("seeded", false)
        set(v) { sp.edit().putBoolean("seeded", v).apply() }
    var favorites: List<String>
        get() = (sp.getString("favs", "") ?: "").split("\n").filter { it.isNotBlank() }
        set(v) { sp.edit().putString("favs", v.joinToString("\n")).apply() }
    var hidden: Set<String>
        get() = (sp.getString("hidden", "") ?: "").split("\n").filter { it.isNotBlank() }.toSet()
        set(v) { sp.edit().putString("hidden", v.joinToString("\n")).apply() }
    var launches: Map<String, Long>
        get() = try {
            val o = JSONObject(sp.getString("launches", "{}") ?: "{}")
            o.keys().asSequence().associateWith { o.getLong(it) }
        } catch (_: Exception) { emptyMap() }
        set(v) { sp.edit().putString("launches", JSONObject(v as Map<*, *>).toString()).apply() }
}
