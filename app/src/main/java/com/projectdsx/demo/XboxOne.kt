package com.projectdsx.demo

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------- Widgets: hora, batería, usuario ----------

@Composable
fun rememberClock(): String {
    val fmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    var t by remember { mutableStateOf(fmt.format(Date())) }
    LaunchedEffect(Unit) { while (true) { t = fmt.format(Date()); delay(10_000) } }
    return t
}

@Composable
fun rememberBattery(): String {
    val ctx = LocalContext.current
    var s by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            val i: Intent? = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val lvl = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val sc = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val chg = (i?.getIntExtra(BatteryManager.EXTRA_STATUS, 0) ?: 0).let {
                it == BatteryManager.BATTERY_STATUS_CHARGING || it == BatteryManager.BATTERY_STATUS_FULL
            }
            if (lvl >= 0) s = (if (chg) "⚡" else "🔋") + " ${lvl * 100 / sc}%"
            delay(30_000)
        }
    }
    return s
}

@Composable
fun ProfileAvatar(name: String, frame: Frame, size: Dp) {
    val c = frame.colors
    val border = if (frame.animated && c.size > 1) {
        val t = rememberInfiniteTransition(label = "frame")
        t.animateColor(c[0], c[1], infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "c").value
    } else c[0]
    Box(
        Modifier.size(size).border(3.dp, border, CircleShape).padding(4.dp).clip(CircleShape)
            .background(Color(0xFF2E7D32)),
        contentAlignment = Alignment.Center
    ) { Text(name.take(1).uppercase().ifBlank { "?" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value / 2.2f).sp) }
}

/** Widget compacto (hora, batería, usuario y puntos). */
@Composable
fun StatusWidget(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val ps = LocalProfiles.current
    ps.tick
    Row(
        modifier.clip(RoundedCornerShape(24.dp)).background(Color(0x99000000)).clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ProfileAvatar(ps.current ?: "?", ps.frame, 30.dp)
        Column {
            Text(ps.current ?: "Invitado", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("G ${ps.points}", color = Color(0xFFB9F6A0), fontSize = 11.sp)
        }
        Text(rememberBattery(), color = Color.White, fontSize = 12.sp)
        Text(rememberClock(), color = Color.White, fontSize = 12.sp)
    }
}

// ---------- Menú Xbox One (inicio "Resume / Discover") ----------

@Composable
private fun Tile(label: String, bg: Color, w: Dp, h: Dp, onClick: () -> Unit, sub: String? = null) {
    val view = LocalView.current
    val app = LocalApp.current
    var focused by remember { mutableStateOf(false) }
    Column(
        Modifier.size(w * app.iconScale, h * app.iconScale)
            .border(if (focused) 3.dp else 0.dp, Color(0xFFB388FF))
            .background(bg)
            .onFocusChanged { if (it.isFocused && !focused) Sfx.nav(view, app); focused = it.isFocused }
            .clickable(onClick = onClick).padding(8.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2)
        if (sub != null) Text(sub, color = Color(0xCCFFFFFF), fontSize = 11.sp, maxLines = 2)
    }
}

@Composable
fun XOneMenu(
    games: List<Game>, wp: String?, open: (Game) -> Unit,
    onProfile: () -> Unit, onSettings: () -> Unit
) {
    val ps = LocalProfiles.current
    ps.tick
    Wallpaper(wp, Brush.verticalGradient(listOf(Color(0xFF7FA6CF), Color(0xFF35603F), Color(0xFF14261A))), 0.55f)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 18.dp)) {
        // Cabecera: usuario a la izquierda, batería y hora a la derecha
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onProfile).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                ProfileAvatar(ps.current ?: "?", ps.frame, 42.dp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(ps.current ?: "Invitado", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("G  ${ps.points}", color = Color(0xFFDDEEDD), fontSize = 13.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            Text(rememberBattery() + "    " + rememberClock(), color = Color.White, fontSize = 15.sp)
        }
        Spacer(Modifier.height(14.dp))
        Text("Reanudar", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            // Columna izquierda: juego grande + 4 accesos rápidos
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                games.firstOrNull()?.let { GameCard(it, 190.dp, 190.dp, Color(0xFFB388FF), RoundedCornerShape(0.dp)) { open(it) } }
                    ?: Tile("Sin juegos", Color(0xFF333333), 190.dp, 190.dp, {}, "Instala juegos o vincula Steam")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Tile("Misiones", Color(0xDD2A2A2A), 92.dp, 62.dp, onProfile)
                    Tile("Tienda", Color(0xDD2A2A2A), 92.dp, 62.dp, onProfile)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Tile("Perfil", Color(0xDD2A2A2A), 92.dp, 62.dp, onProfile)
                    Tile("Ajustes", Color(0xDD2A2A2A), 92.dp, 62.dp, onSettings)
                }
            }
            // Columna derecha: recientes + Descubrir
            Column(Modifier.weight(1f)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    item { Tile("Ver todos", Color(0xDD2A2A2A), 96.dp, 96.dp, onProfile) }
                    items(games.drop(1), key = { it.id }) { GameCard(it, 96.dp, 96.dp, Color(0xFFB388FF), RoundedCornerShape(0.dp)) { open(it) } }
                }
                Spacer(Modifier.height(16.dp))
                Text("Descubrir", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                val totalH = (ps.totalMs / 3_600_000L)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { Tile("Misiones diarias", Color(0xFF107C10), 190.dp, 110.dp, onProfile, "Se reinician a las 12 am") }
                    item { Tile("Tus horas jugadas", Color(0xFF3949AB), 190.dp, 110.dp, onProfile, "$totalH h en total · ${ps.platinums()} platinos") }
                    item { Tile("Tienda de marcos", Color(0xFF6A1B9A), 190.dp, 110.dp, onProfile, "Tienes ${ps.points} puntos") }
                }
                Spacer(Modifier.height(18.dp))
                Text(games.firstOrNull()?.title ?: "Tus juegos", color = Color(0xFFDDEEDD), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { Tile("Logros", Color(0xFFEF6C00), 150.dp, 80.dp, onProfile) }
                    item { Tile("Capturas", Color(0xFF2E7D32), 150.dp, 80.dp, onProfile) }
                    item { Tile("Comunidad", Color(0xFFD81B60), 150.dp, 80.dp, onProfile) }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
