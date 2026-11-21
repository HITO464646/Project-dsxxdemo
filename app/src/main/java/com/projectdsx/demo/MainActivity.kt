package com.projectdsx.demo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay

private enum class Screen { LOGIN, INTRO, HOME, SETTINGS, PROFILE }

class MainActivity : ComponentActivity() {
    private lateinit var state: AppState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        state = AppState(this)
        val profiles = ProfileStore(this)
        val console = state.pickConsole() // cada arranque decide el menú según Ajustes

        setContent {
            val ctx = LocalContext.current
            var screen by remember { mutableStateOf(if (profiles.current == null) Screen.LOGIN else Screen.INTRO) }
            var createFirst by remember { mutableStateOf(false) }
            var installed by remember { mutableStateOf(Installed.games(ctx)) }
            var steamGames by remember { mutableStateOf<List<Game>>(emptyList()) }
            var gfnMap by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
            var refresh by remember { mutableIntStateOf(0) }
            var gfnPrompt by remember { mutableStateOf<Game?>(null) }
            var launching by remember { mutableStateOf<Game?>(null) }
            var banner by remember { mutableStateOf<Pair<String, String>?>(null) }
            var loginShown by remember { mutableStateOf(false) }

            LaunchedEffect(refresh) {
                installed = Installed.games(ctx)
                gfnMap = Gfn.supportedBySteamId()
            }
            LaunchedEffect(state.steamId, state.steamKey, refresh) {
                val id = state.steamId
                steamGames = if (id != null && state.steamKey.isNotBlank()) Steam.owned(state.steamKey, id) else emptyList()
            }
            LaunchedEffect(profiles.current, installed) {
                profiles.sync(ctx, installed.mapNotNull { it.pkg }.toSet())
            }
            // Logro de "inicio de sesión" en consolas Xbox y Steam Deck
            LaunchedEffect(screen, profiles.current) {
                if (screen == Screen.HOME && !loginShown && profiles.current != null) {
                    loginShown = true
                    if (console in setOf(Console.X360, Console.XONE, Console.XSERIES, Console.DECK)) {
                        val txt = if (console == Console.DECK) "Bienvenido de nuevo, ${profiles.current}"
                        else "Inicio de sesión · ${profiles.current}"
                        banner = (if (console == Console.DECK) "Steam Deck" else console.label) to txt
                        Sfx.confirm(state)
                        delay(4000); banner = null
                    }
                }
            }

            val games = remember(installed, steamGames, gfnMap) {
                steamGames.map { it.copy(gfnId = gfnMap[it.steamAppId]) } + installed
            }

            fun startGame(g: Game) {
                when {
                    g.pkg != null -> Installed.launch(ctx, g.pkg)
                    g.gfnId != null -> Gfn.launch(ctx, g.gfnId)
                    else -> ctx.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://store.steampowered.com/app/${g.steamAppId}"))
                    )
                }
                // Etiqueta "ya entraste" sobre el juego (necesita permiso de superposición)
                AchievementAlert.show(ctx, "Ya entraste", g.title, g.pkg, sound = false)
            }

            fun open(g: Game) {
                if (g.pkg == null && g.gfnId != null && state.askGfn) gfnPrompt = g else launching = g
            }

            val d = LocalDensity.current
            CompositionLocalProvider(
                LocalApp provides state,
                LocalProfiles provides profiles,
                LocalDensity provides Density(d.density * state.uiScale, d.fontScale)
            ) {
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    Crossfade(screen, animationSpec = tween(450), label = "screen") { s ->
                        when (s) {
                            Screen.LOGIN -> LoginScreen(profiles, createFirst) { loginShown = false; screen = Screen.INTRO }
                            Screen.INTRO -> IntroScreen(console, state.introUris[console]) { screen = Screen.HOME }
                            Screen.HOME -> Home(console, games, state.wallpaperPkg, ::open,
                                { screen = Screen.PROFILE }, { screen = Screen.SETTINGS })
                            Screen.PROFILE -> {
                                BackHandler { screen = Screen.HOME }
                                ProfileScreen(profiles, installed, { screen = Screen.HOME }) { create ->
                                    createFirst = create; screen = Screen.LOGIN
                                }
                            }
                            Screen.SETTINGS -> {
                                BackHandler { screen = Screen.HOME }
                                SettingsScreen(state, installed, gfnMap.isNotEmpty(), { screen = Screen.HOME }) { refresh++ }
                            }
                        }
                    }
                    InAppBanner(banner, Modifier.align(Alignment.BottomEnd))
                    launching?.let { g ->
                        LaunchOverlay(g) { launching = null; startGame(g) }
                    }
                }

                gfnPrompt?.let { g ->
                    AlertDialog(
                        onDismissRequest = { gfnPrompt = null },
                        title = { Text("¿Jugar en GeForce NOW?") },
                        text = { Text("${g.title} es compatible. Se abrirá GeForce NOW y entrará directo al juego.") },
                        confirmButton = { TextButton({ gfnPrompt = null; launching = g }) { Text("Jugar") } },
                        dismissButton = { TextButton({ gfnPrompt = null }) { Text("Cancelar") } }
                    )
                }
            }
        }
    }

    /** Mando: A = aceptar, B = atrás (el D-pad y los sticks mueven el foco de forma nativa). */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val mapped = when (event.keyCode) {
            KeyEvent.KEYCODE_BUTTON_A -> KeyEvent.KEYCODE_DPAD_CENTER
            KeyEvent.KEYCODE_BUTTON_B -> KeyEvent.KEYCODE_BACK
            else -> null
        }
        if (mapped != null) return super.dispatchKeyEvent(KeyEvent(event.downTime, event.eventTime, event.action, mapped, event.repeatCount))
        return super.dispatchKeyEvent(event)
    }
}
