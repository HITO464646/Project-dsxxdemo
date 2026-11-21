package com.projectdsx.demo

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

private val Green = Color(0xFF76B900)

@Composable
private fun Section(t: String) =
    Text(t, color = Green, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 22.dp, bottom = 6.dp))

@Composable
private fun Row2(label: String, selected: Boolean = false, onClick: () -> Unit) {
    Text(
        (if (selected) "●  " else "○  ") + label,
        color = if (selected) Color.White else Color(0xFFB0B0B0), fontSize = 16.sp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 9.dp)
    )
}

@Composable
fun SettingsScreen(
    st: AppState, installed: List<Game>, gfnReady: Boolean,
    onBack: () -> Unit, onRefresh: () -> Unit
) {
    val ctx = LocalContext.current
    var pendingIntro by remember { mutableStateOf<Console?>(null) }
    var showLogin by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val c = pendingIntro
        if (uri != null && c != null) {
            runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            st.setIntro(c, uri.toString())
        }
    }

    val soundPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            st.changeAchSound(uri.toString())
        }
    }

    if (showLogin) {
        SteamLogin(onId = { st.changeSteamId(it); showLogin = false; onRefresh() }, onClose = { showLogin = false })
        return
    }

    LazyColumn(Modifier.fillMaxSize().background(Color(0xFF101114)).padding(horizontal = 32.dp, vertical = 16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("←", color = Color.White, fontSize = 26.sp, modifier = Modifier.clickable(onClick = onBack).padding(end = 16.dp))
                Text("Ajustes · Project DSX Demo", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }

        item { Section("Arranque del menú") }
        item { Row2("Una consola distinta cada vez que abro la app", st.bootMode == "ROTATE") { st.changeBootMode("ROTATE") } }
        item { Row2("Al azar", st.bootMode == "RANDOM") { st.changeBootMode("RANDOM") } }
        item { Row2("Siempre la misma:", st.bootMode == "FIXED") { st.changeBootMode("FIXED") } }
        items(Console.values().toList()) { c ->
            Text(
                "      " + c.label, fontSize = 15.sp,
                color = if (st.fixedConsole == c.name) Green else Color(0xFF8A8A8A),
                modifier = Modifier.clickable { st.setFixed(c); st.changeBootMode("FIXED") }.padding(vertical = 5.dp)
            )
        }

        item { Section("Videos de intro (elige tu propio video por consola)") }
        items(Console.values().toList()) { c ->
            val has = st.introUris[c] != null
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(c.label, color = Color.White, modifier = Modifier.weight(1f))
                Text(
                    if (has) "Cambiar" else "Elegir video", color = Green,
                    modifier = Modifier.clickable { pendingIntro = c; picker.launch(arrayOf("video/*")) }.padding(8.dp)
                )
                if (has) Text("Quitar", color = Color(0xFFFF6B6B),
                    modifier = Modifier.clickable { st.setIntro(c, null) }.padding(8.dp))
            }
        }

        item { Section("Fondo (según un juego de Android instalado)") }
        item { Row2("Sin fondo de juego", st.wallpaperPkg == null) { st.setWallpaper(null) } }
        items(installed.filter { it.pkg != null }, key = { it.id }) { g ->
            Row2(g.title, st.wallpaperPkg == g.pkg) { st.setWallpaper(g.pkg) }
        }
        if (installed.isEmpty()) item { Text("No se encontraron juegos instalados.", color = Color(0xFF8A8A8A)) }

        item { Section("Steam") }
        item {
            Text(
                if (st.steamId != null) "Cuenta vinculada: ${st.steamId}" else "Sin cuenta vinculada",
                color = Color.White, modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        item {
            Text("Tu Steam Web API Key (steamcommunity.com/dev/apikey) — tu perfil y lista de juegos deben ser públicos:",
                color = Color(0xFFB0B0B0), fontSize = 13.sp)
            BasicTextField(
                st.steamKey, { st.changeSteamKey(it.trim()) },
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp),
                cursorBrush = SolidColor(Green),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).background(Color(0xFF1E2024)).padding(12.dp)
            )
        }
        item {
            Row {
                Text(if (st.steamId == null) "Vincular cuenta de Steam" else "Volver a vincular", color = Green,
                    modifier = Modifier.clickable { showLogin = true }.padding(8.dp))
                if (st.steamId != null) Text("Desvincular", color = Color(0xFFFF6B6B),
                    modifier = Modifier.clickable { st.changeSteamId(null); onRefresh() }.padding(8.dp))
                Text("Actualizar biblioteca", color = Color.White,
                    modifier = Modifier.clickable(onClick = onRefresh).padding(8.dp))
            }
        }

        item { Section("Logros de Google Play Juegos") }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                Text("Avisar cuando complete un logro", color = Color.White, modifier = Modifier.weight(1f))
                Switch(st.achEnabled, { st.changeAchEnabled(it) })
            }
        }
        item {
            Text(
                "Funciona leyendo las notificaciones de Google Play Juegos (mejor esfuerzo).",
                color = Color(0xFFB0B0B0), fontSize = 13.sp
            )
            Row {
                Text(
                    if (hasListenerAccess(ctx)) "✓ Acceso a notificaciones" else "Dar acceso a notificaciones", color = Green,
                    modifier = Modifier.clickable {
                        ctx.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }.padding(8.dp)
                )
                Text(
                    if (android.provider.Settings.canDrawOverlays(ctx)) "✓ Mostrar sobre otras apps" else "Permitir mostrar sobre otras apps", color = Green,
                    modifier = Modifier.clickable {
                        ctx.startActivity(
                            Intent(
                                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                android.net.Uri.parse("package:${ctx.packageName}")
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }.padding(8.dp)
                )
            }
            Row {
                Text(if (st.achSound == null) "Elegir sonido (ahora: por defecto)" else "Cambiar sonido", color = Green,
                    modifier = Modifier.clickable { soundPicker.launch(arrayOf("audio/*")) }.padding(8.dp))
                if (st.achSound != null) Text("Quitar sonido", color = Color(0xFFFF6B6B),
                    modifier = Modifier.clickable { st.changeAchSound(null) }.padding(8.dp))
                Text("Probar", color = Color.White,
                    modifier = Modifier.clickable { AchievementAlert.show(ctx, "Logro desbloqueado", "Prueba de logro — Project DSX") }.padding(8.dp))
            }
        }

        item { Section("GeForce NOW") }
        item {
            Text(
                (if (Gfn.isInstalled(ctx)) "App de GeForce NOW instalada" else "App de GeForce NOW NO instalada (se abrirá en el navegador)") +
                    "\nLista de juegos compatibles: " + (if (gfnReady) "cargada" else "sin cargar (revisa tu internet)"),
                color = Color(0xFFB0B0B0), fontSize = 14.sp
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                Text("Preguntar si quiero jugar en GeForce NOW", color = Color.White, modifier = Modifier.weight(1f))
                Switch(st.askGfn, { st.changeAskGfn(it) })
            }
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

/** Login de Steam por OpenID dentro de un WebView. Devuelve el SteamID64. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SteamLogin(onId: (String) -> Unit, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Text("✕  Cerrar", color = Color.White, modifier = Modifier.clickable(onClick = onClose).padding(14.dp))
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                        val u = r.url
                        if (u.host == "localhost" && u.path == "/dsx") {
                            u.getQueryParameter("openid.claimed_id")?.substringAfterLast("/")?.let(onId)
                            return true
                        }
                        return false
                    }
                }
                val ns = "http://specs.openid.net/auth/2.0"
                loadUrl(
                    "https://steamcommunity.com/openid/login?openid.ns=$ns&openid.mode=checkid_setup" +
                        "&openid.return_to=http://localhost/dsx&openid.realm=http://localhost" +
                        "&openid.identity=$ns/identifier_select&openid.claimed_id=$ns/identifier_select"
                )
            }
        })
    }
}
