package com.projectdsx.demo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

private val Green = Color(0xFF9BF00B)
private val Bg = Color(0xFF101114)

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit, password: Boolean = false) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(label, color = Color(0xFFB0B0B0), fontSize = 12.sp)
        BasicTextField(
            value, onChange, singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp), cursorBrush = SolidColor(Green),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier.fillMaxWidth().background(Color(0xFF1E2024), RoundedCornerShape(6.dp)).padding(12.dp)
        )
    }
}

@Composable
private fun Btn(t: String, color: Color = Green, onClick: () -> Unit) =
    Text(t, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 10.dp))

// ---------- Inicio de sesión / crear cuenta ----------
@Composable
fun LoginScreen(ps: ProfileStore, startInCreate: Boolean, onDone: () -> Unit) {
    var create by remember { mutableStateOf(startInCreate || ps.names.isEmpty()) }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize().background(Bg).padding(horizontal = 120.dp, vertical = 20.dp)) {
        item {
            Text("PROJECT DSX DEMO", color = Green, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(if (create) "Crear cuenta" else "Iniciar sesión", color = Color.White, fontSize = 26.sp)
            Field("Usuario", user, { user = it; err = null })
            Field("Contraseña", pass, { pass = it; err = null }, password = true)
            err?.let { Text(it, color = Color(0xFFFF6B6B), modifier = Modifier.padding(vertical = 4.dp)) }
            Row {
                Btn(if (create) "Crear y entrar" else "Entrar") {
                    err = if (create) ps.create(user, pass) else ps.login(user, pass)
                    if (err == null) onDone()
                }
                if (ps.names.isNotEmpty()) Btn(if (create) "Ya tengo cuenta" else "Crear otra cuenta", Color.White) { create = !create; err = null }
            }
            if (!create && ps.names.isNotEmpty()) {
                Text("Cuentas en este dispositivo:", color = Color(0xFFB0B0B0), fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
                Row { ps.names.forEach { n -> Btn(n, Color(0xFF66C0F4)) { user = n } } }
            }
            Text("Las cuentas son locales: se guardan solo en este dispositivo.", color = Color(0xFF777777), fontSize = 11.sp, modifier = Modifier.padding(top = 14.dp))
        }
    }
}

// ---------- Perfil: misiones, horas, tienda y cuentas ----------
@Composable
fun ProfileScreen(ps: ProfileStore, installed: List<Game>, onBack: () -> Unit, onSwitch: (Boolean) -> Unit) {
    val ctx = LocalContext.current
    ps.tick
    val pkgs = remember(installed) { installed.mapNotNull { it.pkg }.toSet() }
    val titles = remember(installed) { installed.associate { (it.pkg ?: "") to it.title } }
    var minutesToday by remember { mutableIntStateOf(0) }
    var confirmDelete by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(ps.current) {
        ps.sync(ctx, pkgs)
        minutesToday = (Usage.playMs(ctx, Usage.startOfToday()).filterKeys { it in pkgs }.values.sum() / 60_000L).toInt()
    }
    val totals = ps.totals().entries.sortedByDescending { it.value }
    val milestonesDone = totals.sumOf { e -> HOUR_MILESTONES.count { (e.value / 3_600_000L) >= it } }

    LazyColumn(Modifier.fillMaxSize().background(Bg).padding(horizontal = 32.dp, vertical = 14.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("←", color = Color.White, fontSize = 26.sp, modifier = Modifier.clickable(onClick = onBack).padding(end = 16.dp))
                ProfileAvatar(ps.current ?: "?", ps.frame, 64.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(ps.current ?: "Invitado", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("G ${ps.points} puntos", color = Green)
                }
            }
            Text(
                "${ps.daysInApp} días en la app · ${ps.totalMs / 3_600_000L} h jugadas · $milestonesDone logros · ${ps.platinums()} platinos",
                color = Color(0xFFB0B0B0), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)
            )
            Field("Descripción del perfil", ps.bio, { ps.bio = it })
            if (!Usage.hasAccess(ctx)) {
                Btn("Para contar tus horas, permite el acceso de uso a la app →", Color(0xFFFFC83D)) { Usage.openSettings(ctx) }
            }
            msg?.let { Text(it, color = Color(0xFFFFC83D)) }
        }

        item { Text("Misiones diarias (se reinician a las 12 am)", color = Green, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp, bottom = 4.dp)) }
        items(MISSIONS) { m ->
            val done = ps.isClaimed(m)
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(m.title, color = Color.White)
                    Text("${minOf(minutesToday, m.minutes)}/${m.minutes} min · +${m.reward} puntos", color = Color(0xFFB0B0B0), fontSize = 12.sp)
                }
                when {
                    done -> Text("✓ Reclamado", color = Color(0xFF888888))
                    minutesToday >= m.minutes -> Btn("Reclamar") { ps.claim(m, minutesToday) }
                    else -> Text("En progreso", color = Color(0xFF888888))
                }
            }
        }

        item { Text("Tienda de marcos de perfil", color = Green, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp, bottom = 4.dp)) }
        items(FRAMES) { f ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                ProfileAvatar(ps.current ?: "?", f, 40.dp)
                Spacer(Modifier.width(12.dp))
                Text(f.name + if (f.animated) "  (animado)" else "", color = Color.White, modifier = Modifier.weight(1f))
                when {
                    ps.frameId == f.id -> Text("Equipado", color = Green)
                    ps.ownsFrame(f.id) -> Btn("Equipar") { ps.equipFrame(f) }
                    else -> Btn("Comprar · ${f.price}", Color(0xFFFFC83D)) {
                        msg = if (ps.buyFrame(f)) "¡Compraste ${f.name}!" else "No te alcanzan los puntos"
                    }
                }
            }
        }

        item { Text("Horas y logros por juego (10 puntos por hora, +50 por hito)", color = Green, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp, bottom = 4.dp)) }
        if (totals.isEmpty()) item { Text("Aún no hay horas registradas. Juega y vuelve aquí.", color = Color(0xFF888888)) }
        items(totals.take(30)) { e ->
            val h = e.value / 3_600_000.0
            val done = HOUR_MILESTONES.count { h >= it }
            Column(Modifier.padding(vertical = 5.dp)) {
                Text(titles[e.key] ?: e.key, color = Color.White)
                Text(
                    "%.1f h · logros $done/${HOUR_MILESTONES.size}: ".format(h) +
                        HOUR_MILESTONES.joinToString(" ") { if (h >= it) "✓${it}h" else "○${it}h" } +
                        if (done == HOUR_MILESTONES.size) "   🏆 PLATINO" else "",
                    color = if (done == HOUR_MILESTONES.size) Color(0xFFFFC83D) else Color(0xFFB0B0B0), fontSize = 12.sp
                )
            }
        }

        item {
            Text("Cuentas", color = Green, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp, bottom = 4.dp))
            Text("Cuentas guardadas: " + ps.names.joinToString(", "), color = Color(0xFFB0B0B0), fontSize = 13.sp)
            Row {
                Btn("Cerrar sesión / cambiar de cuenta", Color.White) { ps.logout(); onSwitch(false) }
                Btn("Crear otra cuenta", Color.White) { ps.logout(); onSwitch(true) }
            }
            Row {
                if (!confirmDelete) Btn("Eliminar esta cuenta", Color(0xFFFF6B6B)) { confirmDelete = true }
                else {
                    Btn("Sí, eliminar para siempre", Color(0xFFFF6B6B)) { ps.current?.let { ps.delete(it) }; onSwitch(false) }
                    Btn("Cancelar", Color.White) { confirmDelete = false }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

// ---------- Pantalla "Entrando…" al abrir un juego ----------
@Composable
fun LaunchOverlay(game: Game, onFinish: () -> Unit) {
    val st = LocalApp.current
    val p = remember { Animatable(0f) }
    val inf = rememberInfiniteTransition(label = "sticks")
    val ang by inf.animateFloat(0f, 6.2832f, infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart), label = "a")
    LaunchedEffect(game.id) {
        p.animateTo(1f, tween(2800, easing = LinearEasing))
        Sfx.confirm(st)
        onFinish()
    }
    val label = when { p.value < 0.35f -> "Entrando…"; p.value < 0.98f -> "Espera…"; else -> "¡Listo!" }
    Box(Modifier.fillMaxSize().background(Color(0xF2000000)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            GameCard(game, 110.dp, 110.dp, Color.White, RoundedCornerShape(10.dp)) {}
            Spacer(Modifier.height(10.dp))
            Text(game.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            Canvas(Modifier.size(150.dp, 92.dp)) {
                drawRoundRect(Color(0xFF3A3D45), cornerRadius = CornerRadius(46f, 46f))
                val r = size.height * 0.15f
                val l = Offset(size.width * 0.28f, size.height * 0.42f)
                val rt = Offset(size.width * 0.66f, size.height * 0.64f)
                val d = r * 0.45f
                drawCircle(Color(0xFF15161A), r * 1.35f, l)
                drawCircle(Color(0xFFDDDDDD), r, l + Offset(cos(ang) * d, sin(ang) * d))
                drawCircle(Color(0xFF15161A), r * 1.35f, rt)
                drawCircle(Color(0xFFDDDDDD), r, rt + Offset(cos(-ang) * d, sin(-ang) * d))
                listOf(Offset(0.8f, 0.28f) to Color(0xFFFFC83D), Offset(0.88f, 0.4f) to Color(0xFFFF4D4D),
                    Offset(0.72f, 0.4f) to Color(0xFF4DA3FF), Offset(0.8f, 0.52f) to Color(0xFF6BE675)).forEach { (o, c) ->
                    drawCircle(c, r * 0.32f, Offset(size.width * o.x, size.height * o.y))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(label, color = Green, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.width(260.dp).height(8.dp).background(Color(0xFF333333), RoundedCornerShape(4.dp))) {
                Box(Modifier.fillMaxWidth(p.value).fillMaxHeight().background(Green, RoundedCornerShape(4.dp)))
            }
        }
    }
}

// ---------- Banner dentro de la app (logro de inicio de sesión) ----------
@Composable
fun InAppBanner(banner: Pair<String, String>?, modifier: Modifier = Modifier) {
    var last by remember { mutableStateOf(banner) }
    if (banner != null) last = banner
    AnimatedVisibility(
        visible = banner != null, modifier = modifier,
        enter = slideInHorizontally { it } + fadeIn(), exit = slideOutHorizontally { it } + fadeOut()
    ) {
        Column(
            Modifier.padding(16.dp).background(Color(0xE61B2838), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text("🏆  " + (last?.first ?: ""), color = Color(0xFF66C0F4), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(last?.second ?: "", color = Color.White, fontSize = 15.sp)
        }
    }
}
