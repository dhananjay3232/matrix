package com.dhananjay.matrixhome

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.concurrent.thread

sealed interface Overlay {
    data object None : Overlay
    data object Quick : Overlay
    data object Settings : Overlay
    data object Notifs : Overlay
    data object CitySearch : Overlay
    data class AppMenu(val tile: Tile, val fromFavorites: Boolean) : Overlay
}

class HomeState(private val ctx: Context) {
    val prefs = Prefs(ctx)

    var apps by mutableStateOf<List<Tile>>(emptyList())
    var inputs by mutableStateOf<List<Tile>>(emptyList())
    var favs by mutableStateOf(prefs.favorites)
    var hidden by mutableStateOf(prefs.hidden)
    var launches by mutableStateOf(prefs.launches)
    var theme by mutableIntStateOf(prefs.theme)
    var use24h by mutableStateOf(prefs.use24h)
    var city by mutableStateOf(prefs.city)
    var wallpaper by mutableStateOf(prefs.wallpaper)
    var wallBitmap by mutableStateOf<ImageBitmap?>(null)
    var idleMin by mutableIntStateOf(prefs.idleMin)
    var hideVendor by mutableStateOf(prefs.hideVendor)
    var weather by mutableStateOf("")
    var homePressed by mutableIntStateOf(0)
    var overlay by mutableStateOf<Overlay>(Overlay.None)
    var moving by mutableStateOf<String?>(null)

    init {
        reloadApps()
        reloadInputs()
        loadWall()
    }

    /** Apps the user should see, A to Z. */
    val visible: List<Tile>
        get() = apps.filter { it.key !in hidden && !(hideVendor && it.vendor) }
            .sortedBy { it.label.lowercase() }

    val favTiles: List<Tile>
        get() = favs.mapNotNull { k -> visible.firstOrNull { it.key == k } }

    val recentTiles: List<Tile>
        get() = launches.entries.sortedByDescending { it.value }
            .mapNotNull { e -> visible.firstOrNull { it.key == e.key } }.take(10)

    fun reloadApps() {
        thread {
            val list = loadApps(ctx)
            apps = list
            if (!prefs.seeded && list.isNotEmpty()) {
                favs = DefaultFavorites.filter { k -> list.any { it.key == k } }
                prefs.favorites = favs
                prefs.seeded = true
            }
        }
    }

    fun reloadInputs() { thread { inputs = loadInputs(ctx) } }

    suspend fun refreshWeather() {
        val c = city
        val w = withContext(Dispatchers.IO) { fetchWeather(c) }
        weather = w?.let { "${c.name.substringBefore(",")}  $it" } ?: weather
    }

    fun setCity(c: City) {
        city = c; prefs.city = c; weather = ""
    }

    // ---- Wallpaper: gradients built in, images read from the app's wallpapers folder ----
    val wallFolder get() = ctx.getExternalFilesDir("wallpapers")

    fun wallpaperChoices(): List<String> {
        val imgs = wallFolder?.listFiles()
            ?.filter { it.extension.lowercase() in setOf("jpg", "jpeg", "png", "webp") }
            ?.map { "img:${it.name}" }?.sorted() ?: emptyList()
        return listOf("auto", "grad:Ocean", "grad:Sunset", "grad:Forest", "grad:Graphite") + imgs
    }

    fun wallpaperLabel(w: String) = when {
        w == "auto" -> "Auto gradient"
        w.startsWith("grad:") -> w.removePrefix("grad:") + " gradient"
        else -> w.removePrefix("img:")
    }

    fun nextWallpaper() {
        val c = wallpaperChoices()
        wallpaper = c[(c.indexOf(wallpaper) + 1) % c.size]
        prefs.wallpaper = wallpaper
        loadWall()
    }

    fun loadWall() {
        thread {
            wallBitmap = try {
                if (!wallpaper.startsWith("img:")) null else {
                    val f = java.io.File(wallFolder, wallpaper.removePrefix("img:"))
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(f.path, bounds)
                    var sample = 1
                    while (bounds.outWidth / sample > 1920) sample *= 2
                    BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample })
                        ?.asImageBitmap()
                }
            } catch (_: Throwable) { null }
        }
    }

    fun markLaunched(key: String) {
        launches = launches + (key to System.currentTimeMillis())
        prefs.launches = launches
    }

    fun moveFav(key: String, delta: Int) {
        val i = favs.indexOf(key)
        val j = i + delta
        if (i < 0 || j !in favs.indices) return
        val l = favs.toMutableList()
        l[i] = l[j].also { l[j] = l[i] }
        favs = l
        prefs.favorites = l
    }

    fun toggleFav(key: String) {
        favs = if (key in favs) favs - key else favs + key
        prefs.favorites = favs
    }

    fun hide(key: String) {
        hidden = hidden + key
        prefs.hidden = hidden
    }

    fun settingsOptions(open: (String, String?) -> Unit): List<Pair<String, () -> Unit>> = listOf(
        "Theme: ${listOf("Auto", "Dark", "Light")[theme]}" to { theme = (theme + 1) % 3; prefs.theme = theme },
        "Clock: ${if (use24h) "24-hour" else "12-hour"}" to { use24h = !use24h; prefs.use24h = use24h },
        "Weather city: ${city.name} (search)" to { overlay = Overlay.CitySearch },
        "Wallpaper: ${wallpaperLabel(wallpaper)}" to { nextWallpaper() },
        "Screensaver settings" to { open(android.provider.Settings.ACTION_DREAM_SETTINGS, null) },
        "Idle clock: ${if (idleMin == 0) "Off" else "$idleMin min"}" to {
            val opts = listOf(0, 2, 5, 10)
            idleMin = opts[(opts.indexOf(idleMin) + 1) % opts.size]; prefs.idleMin = idleMin
        },
        "Hide Haier / system apps: ${if (hideVendor) "On" else "Off"}" to {
            hideVendor = !hideVendor; prefs.hideVendor = hideVendor
        },
        "Show hidden apps again (${hidden.size})" to { hidden = emptySet(); prefs.hidden = hidden },
        "Reset favorites" to {
            favs = DefaultFavorites.filter { k -> apps.any { it.key == k } }; prefs.favorites = favs
        },
        "Close" to { overlay = Overlay.None },
    )
}
