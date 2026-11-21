package com.projectdsx.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

private enum class Screen { INTRO, HOME, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val state = AppState(this)
        val console = state.pickConsole() // cada arranque decide el menú según Ajustes

        setContent {
            val ctx = LocalContext.current
            var screen by remember { mutableStateOf(Screen.INTRO) }
            var installed by remember { mutableStateOf(Installed.games(ctx)) }
            var steamGames by remember { mutableStateOf<List<Game>>(emptyList()) }
            var gfnMap by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
            var refresh by remember { mutableIntStateOf(0) }
            var gfnPrompt by remember { mutableStateOf<Game?>(null) }

            LaunchedEffect(refresh) {
                installed = Installed.games(ctx)
                gfnMap = Gfn.supportedBySteamId()
            }
            LaunchedEffect(state.steamId, state.steamKey, refresh) {
                val id = state.steamId
                steamGames = if (id != null && state.steamKey.isNotBlank()) Steam.owned(state.steamKey, id) else emptyList()
            }

            // Juegos de Steam marcados con GFN si GeForce NOW los soporta
            val games = remember(installed, steamGames, gfnMap) {
                steamGames.map { it.copy(gfnId = gfnMap[it.steamAppId]) } + installed
            }

            fun open(g: Game) {
                when {
                    g.pkg != null -> Installed.launch(ctx, g.pkg)
                    g.gfnId != null && !state.askGfn -> Gfn.launch(ctx, g.gfnId)
                    g.gfnId != null -> gfnPrompt = g
                    else -> ctx.startActivity(
                        android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://store.steampowered.com/app/${g.steamAppId}")
                        )
                    )
                }
            }

            Box(Modifier.fillMaxSize().background(Color.Black)) {
                Crossfade(screen, animationSpec = tween(450), label = "screen") { s ->
                    when (s) {
                        Screen.INTRO -> IntroScreen(console, state.introUris[console]) { screen = Screen.HOME }
                        Screen.HOME -> Home(console, games, state.wallpaperPkg, ::open) { screen = Screen.SETTINGS }
                        Screen.SETTINGS -> {
                            BackHandler { screen = Screen.HOME }
                            SettingsScreen(state, installed, gfnMap.isNotEmpty(), { screen = Screen.HOME }) { refresh++ }
                        }
                    }
                }
            }

            gfnPrompt?.let { g ->
                AlertDialog(
                    onDismissRequest = { gfnPrompt = null },
                    title = { Text("¿Jugar en GeForce NOW?") },
                    text = { Text("${g.title} es compatible. Se abrirá GeForce NOW y entrará directo al juego.") },
                    confirmButton = { TextButton({ gfnPrompt = null; Gfn.launch(ctx, g.gfnId!!) }) { Text("Jugar") } },
                    dismissButton = { TextButton({ gfnPrompt = null }) { Text("Cancelar") } }
                )
            }
        }
    }
}
