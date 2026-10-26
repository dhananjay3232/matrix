package com.dhananjay.matrixhome

import android.provider.Settings
import android.view.KeyEvent as AKey
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.nativeKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class P(
    val bg: List<Color>, val text: Color, val sub: Color,
    val card: Color, val panel: Color, val accent: Color,
)

val DarkP = P(
    listOf(Color(0xFF0B1020), Color(0xFF16213E), Color(0xFF2B1B4D)),
    Color.White, Color(0xB3FFFFFF), Color(0x33FFFFFF), Color(0xFF1C2541), Color(0xFF7AA2FF),
)
val LightP = P(
    listOf(Color(0xFFF2F6FF), Color(0xFFDCE7FF), Color(0xFFCBD9FF)),
    Color(0xFF0B1020), Color(0xB30B1020), Color(0x1A000000), Color(0xFFFFFFFF), Color(0xFF2F5BFF),
)

private val IN = Locale("en", "IN")

fun gradientFor(w: String): List<Color>? = when (w) {
    "grad:Ocean" -> listOf(Color(0xFF021B33), Color(0xFF0B4F6C), Color(0xFF1B998B))
    "grad:Sunset" -> listOf(Color(0xFF2B1055), Color(0xFF7B2F6E), Color(0xFFD9695F))
    "grad:Forest" -> listOf(Color(0xFF07140E), Color(0xFF133A28), Color(0xFF2E6B45))
    "grad:Graphite" -> listOf(Color(0xFF0D0D0F), Color(0xFF1A1B1F), Color(0xFF2A2C33))
    else -> null
}

@Composable
fun HomeRoot(
    s: HomeState,
    launch: (Tile) -> Unit,
    openSettings: (String, String?) -> Unit,
    voice: () -> Unit,
) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { now = LocalDateTime.now(); delay(15_000) } }

    var lastAct by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var idle by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(5_000)
            if (s.idleMin > 0 && !idle && s.overlay is Overlay.None && s.moving == null &&
                System.currentTimeMillis() - lastAct > s.idleMin * 60_000L
            ) idle = true
        }
    }
    LaunchedEffect(s.city) { while (true) { s.refreshWeather(); delay(30 * 60_000L) } }

    val dark = when (s.theme) { 1 -> true; 2 -> false; else -> now.hour !in 7..17 }
    val img = s.wallBitmap
    val grad = gradientFor(s.wallpaper)
    val p = if (img != null || grad != null || dark) DarkP else LightP
    val timeFmt = DateTimeFormatter.ofPattern(if (s.use24h) "HH:mm" else "h:mm a", IN)
    val dateFmt = DateTimeFormatter.ofPattern("EEEE, d MMMM", IN)
    val ctx = LocalContext.current

    val fav = s.favTiles
    val recent = s.recentTiles
    val listState = rememberLazyListState()
    LaunchedEffect(s.homePressed) {
        if (s.homePressed > 0) { listState.scrollToItem(0); s.overlay = Overlay.None; s.moving = null }
    }

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(grad ?: p.bg))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        lastAct = System.currentTimeMillis(); idle = false
                    }
                }
            }
            .onPreviewKeyEvent { ev ->
                val k = ev.nativeKeyEvent.keyCode
                val down = ev.type == KeyEventType.KeyDown
                lastAct = System.currentTimeMillis()
                if (idle) { if (down) idle = false; return@onPreviewKeyEvent true }
                val mv = s.moving
                if (mv != null) {
                    if (down) when (k) {
                        AKey.KEYCODE_DPAD_LEFT -> s.moveFav(mv, -1)
                        AKey.KEYCODE_DPAD_RIGHT -> s.moveFav(mv, 1)
                        AKey.KEYCODE_DPAD_CENTER, AKey.KEYCODE_ENTER, AKey.KEYCODE_BACK -> s.moving = null
                    }
                    return@onPreviewKeyEvent k in setOf(
                        AKey.KEYCODE_DPAD_LEFT, AKey.KEYCODE_DPAD_RIGHT, AKey.KEYCODE_DPAD_UP,
                        AKey.KEYCODE_DPAD_DOWN, AKey.KEYCODE_DPAD_CENTER, AKey.KEYCODE_ENTER, AKey.KEYCODE_BACK,
                    )
                }
                if (!down || s.overlay !is Overlay.None) return@onPreviewKeyEvent false
                when (k) {
                    in AKey.KEYCODE_1..AKey.KEYCODE_9 -> { fav.getOrNull(k - AKey.KEYCODE_1)?.let(launch); true }
                    AKey.KEYCODE_PROG_RED -> { s.overlay = Overlay.Settings; true }
                    AKey.KEYCODE_PROG_GREEN -> { s.overlay = Overlay.Quick; true }
                    AKey.KEYCODE_PROG_YELLOW -> { s.inputs.firstOrNull()?.let(launch); true }
                    AKey.KEYCODE_PROG_BLUE -> { s.overlay = Overlay.Notifs; true }
                    AKey.KEYCODE_SEARCH, AKey.KEYCODE_ASSIST, AKey.KEYCODE_VOICE_ASSIST -> { voice(); true }
                    else -> false
                }
            },
    ) {
        if (img != null) {
            Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Color(0x66000000))) // keeps text readable
        }
        Column(Modifier.fillMaxSize()) {
            // ---- Top bar: clock, date, weather, shortcuts ----
            Row(
                Modifier.fillMaxWidth().padding(start = 48.dp, end = 48.dp, top = 28.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(now.format(timeFmt), color = p.text, fontSize = 44.sp, fontWeight = FontWeight.Light)
                    Text(
                        now.format(dateFmt) + if (s.weather.isNotEmpty()) "   ·   ${s.weather}" else "",
                        color = p.sub, fontSize = 16.sp,
                    )
                }
                val n = NotifStore.titles.size
                Chip(if (n > 0) "Notifications ($n)" else "Notifications", p) { s.overlay = Overlay.Notifs }
                Spacer(Modifier.width(12.dp))
                Chip("Quick settings", p) { s.overlay = Overlay.Quick }
                Spacer(Modifier.width(12.dp))
                Chip("Matrix Home settings", p) { s.overlay = Overlay.Settings }
            }

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                if (fav.isNotEmpty()) item(key = "fav") {
                    Section("Favorites", fav, p, s.moving, launch) { s.overlay = Overlay.AppMenu(it, true) }
                }
                if (recent.isNotEmpty()) item(key = "recent") {
                    Section("Recent", recent, p, null, launch) { s.overlay = Overlay.AppMenu(it, false) }
                }
                if (s.inputs.isNotEmpty()) item(key = "inputs") {
                    Section("Inputs", s.inputs, p, null, launch) { }
                }
                item(key = "apps") {
                    Section("All apps", s.visible, p, null, launch) { s.overlay = Overlay.AppMenu(it, false) }
                }
            }
        }

        // ---- Overlays ----
        val close = { s.overlay = Overlay.None }
        when (val o = s.overlay) {
            Overlay.None -> {}
            Overlay.Quick -> OptionDialog(
                "Quick settings", p,
                listOf(
                    "Wi-Fi" to { openSettings(Settings.ACTION_WIFI_SETTINGS, null); close() },
                    "Bluetooth" to { openSettings(Settings.ACTION_BLUETOOTH_SETTINGS, null); close() },
                    "Display" to { openSettings(Settings.ACTION_DISPLAY_SETTINGS, null); close() },
                    "Sound" to { openSettings(Settings.ACTION_SOUND_SETTINGS, null); close() },
                    "All settings" to { openSettings(Settings.ACTION_SETTINGS, null); close() },
                ),
                close,
            )
            Overlay.Settings -> OptionDialog(
                "Matrix Home settings", p,
                s.settingsOptions { a, pk -> openSettings(a, pk) }, close,
            )
            Overlay.CitySearch -> CitySearchDialog(p, { s.setCity(it); close() }, close)
            Overlay.Notifs -> {
                val titles = NotifStore.titles
                val granted = NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
                val items = mutableListOf<Pair<String, () -> Unit>>()
                if (!granted) items += "Allow notification access" to {
                    openSettings("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS", null); close()
                } else if (titles.isEmpty()) items += "No notifications" to { }
                titles.take(12).forEach { items += it to { } }
                items += "Close" to close
                OptionDialog("Notifications", p, items, close)
            }
            is Overlay.AppMenu -> {
                val t = o.tile
                val items = mutableListOf<Pair<String, () -> Unit>>()
                items += (if (t.key in s.favs) "Remove from favorites" else "Add to favorites") to {
                    s.toggleFav(t.key); close()
                }
                if (o.fromFavorites) items += "Move in favorites" to { s.moving = t.key; close() }
                items += "Hide from home" to { s.hide(t.key); close() }
                items += "App info" to { openSettings(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, t.key); close() }
                items += "Uninstall" to { openSettings("android.intent.action.DELETE", t.key); close() }
                items += "Cancel" to close
                OptionDialog(t.label, p, items, close)
            }
        }

        if (idle) {
            Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(now.format(timeFmt), color = Color.White, fontSize = 120.sp, fontWeight = FontWeight.Thin)
                    Text(now.format(dateFmt), color = Color(0x99FFFFFF), fontSize = 24.sp)
                }
            }
        }
    }
}

@Composable
private fun Section(
    title: String, tiles: List<Tile>, p: P, movingKey: String?,
    onClick: (Tile) -> Unit, onLong: (Tile) -> Unit,
) {
    Column {
        Text(
            title, color = p.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 52.dp, bottom = 8.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(tiles, key = { it.key }) { t ->
                TileView(t, p, movingKey == t.key, { onClick(t) }, { onLong(t) })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TileView(t: Tile, p: P, moving: Boolean, onClick: () -> Unit, onLong: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.08f else 1f, tween(120), label = "scale")
    Column(
        Modifier.width(200.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { focused = it.isFocused }
            .combinedClickable(onClick = onClick, onLongClick = onLong),
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(p.card)
                .border(
                    if (focused || moving) 3.dp else 0.dp,
                    if (moving) Color(0xFFFFC107) else p.accent,
                    RoundedCornerShape(12.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            val icon = t.icon
            if (icon != null) {
                Image(
                    icon, null,
                    modifier = if (t.isBanner) Modifier.fillMaxSize() else Modifier.fillMaxSize().padding(18.dp),
                    contentScale = if (t.isBanner) ContentScale.Crop else ContentScale.Fit,
                )
            } else {
                Text(t.label, color = p.text, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            }
        }
        Text(
            if (moving) "◀ ${t.label} ▶" else t.label,
            color = if (focused) p.text else p.sub, fontSize = 14.sp, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp, start = 4.dp),
        )
    }
}

@Composable
private fun Chip(text: String, p: P, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier.onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(50))
            .background(if (focused) p.accent else p.card)
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(text, color = if (focused) Color.White else p.text, fontSize = 15.sp, maxLines = 1)
    }
}

@Composable
private fun OptionDialog(title: String, p: P, options: List<Pair<String, () -> Unit>>, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        val first = remember { FocusRequester() }
        Column(
            Modifier.clip(RoundedCornerShape(20.dp)).background(p.panel)
                .widthIn(min = 420.dp).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, color = p.text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 10.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            options.forEachIndexed { i, (label, action) ->
                OptionRow(label, p, action, if (i == 0) Modifier.focusRequester(first) else Modifier)
            }
        }
        LaunchedEffect(Unit) { first.requestFocus() }
    }
}

@Composable
private fun OptionRow(label: String, p: P, onClick: () -> Unit, modifier: Modifier) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier.fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(10.dp))
            .background(if (focused) p.accent else Color.Transparent)
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(label, color = if (focused) Color.White else p.text, fontSize = 17.sp, maxLines = 1,
            overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CitySearchDialog(p: P, onPick: (City) -> Unit, onDismiss: () -> Unit) {
    var q by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(Cities) }
    var searched by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val field = remember { FocusRequester() }
    val search = {
        if (q.isNotBlank()) scope.launch {
            results = withContext(Dispatchers.IO) { geocode(q) }
            searched = true
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.clip(RoundedCornerShape(20.dp)).background(p.panel).widthIn(min = 460.dp).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Weather city", color = p.text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 6.dp))
            Text("Type a city name, then choose Search", color = p.sub, fontSize = 14.sp)
            BasicTextField(
                value = q, onValueChange = { q = it }, singleLine = true,
                textStyle = TextStyle(color = p.text, fontSize = 20.sp),
                cursorBrush = SolidColor(p.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { search() }),
                modifier = Modifier.fillMaxWidth().focusRequester(field)
                    .background(p.card, RoundedCornerShape(10.dp)).padding(14.dp),
            )
            OptionRow("Search", p, { search() }, Modifier)
            if (searched && results.isEmpty()) {
                Text("No city found (check internet or spelling)", color = p.sub, fontSize = 14.sp)
            }
            if (!searched) Text("Popular cities", color = p.sub, fontSize = 14.sp)
            results.take(6).forEach { c -> OptionRow(c.name, p, { onPick(c) }, Modifier) }
            OptionRow("Cancel", p, onDismiss, Modifier)
        }
        LaunchedEffect(Unit) { field.requestFocus() }
    }
}
