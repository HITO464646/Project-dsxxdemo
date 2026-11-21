package com.projectdsx.demo

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class Console(val label: String) {
    X360("Xbox 360"),
    XONE("Xbox One"),
    XSERIES("Xbox Series X|S"),
    PS4("PlayStation 4"),
    PS5("PlayStation 5"),
    DECK("Steam Deck")
}

data class Game(
    val id: String,
    val title: String,
    val pkg: String? = null,          // juego instalado en Android
    val steamAppId: Long? = null,     // juego de tu biblioteca de Steam
    val gfnId: String? = null         // id de GeForce NOW si es compatible
)

/** Estado persistente de la app (SharedPreferences + estado observable de Compose). */
class AppState(ctx: Context) {
    private val sp = ctx.getSharedPreferences("dsx", Context.MODE_PRIVATE)

    // FIXED = siempre la misma consola, RANDOM = al azar, ROTATE = una distinta cada vez
    var bootMode by mutableStateOf(sp.getString("bootMode", "ROTATE")!!); private set
    var fixedConsole by mutableStateOf(sp.getString("fixed", Console.XSERIES.name)!!); private set
    var wallpaperPkg by mutableStateOf(sp.getString("wallpaper", null)); private set
    var steamId by mutableStateOf(sp.getString("steamId", null)); private set
    var steamKey by mutableStateOf(sp.getString("steamKey", "")!!); private set
    var achEnabled by mutableStateOf(sp.getBoolean("achEnabled", true)); private set
    var achSound by mutableStateOf(sp.getString("achSound", null)); private set
    fun changeAchEnabled(v: Boolean) { achEnabled = v; sp.edit().putBoolean("achEnabled", v).apply() }
    fun changeAchSound(v: String?) { achSound = v; put("achSound", v) }
    var askGfn by mutableStateOf(sp.getBoolean("askGfn", true)); private set
    var introUris by mutableStateOf(Console.values().associateWith { sp.getString("intro_${it.name}", null) }); private set

    // Personalización: tamaño de interfaz / iconos y sonidos de navegación
    var uiScale by mutableStateOf(sp.getFloat("uiScale", 1f)); private set
    var iconScale by mutableStateOf(sp.getFloat("iconScale", 1f)); private set
    var navSoundMode by mutableStateOf(sp.getString("navMode", "SYSTEM")!!); private set // OFF/SYSTEM/TONE/CUSTOM
    var navSoundUri by mutableStateOf(sp.getString("navUri", null)); private set
    var sfxVolume by mutableStateOf(sp.getInt("sfxVol", 80)); private set
    fun changeUiScale(v: Float) { uiScale = v.coerceIn(0.7f, 1.4f); sp.edit().putFloat("uiScale", uiScale).apply() }
    fun changeIconScale(v: Float) { iconScale = v.coerceIn(0.6f, 1.6f); sp.edit().putFloat("iconScale", iconScale).apply() }
    fun changeNavMode(v: String) { navSoundMode = v; put("navMode", v) }
    fun changeNavUri(v: String?) { navSoundUri = v; put("navUri", v) }
    fun changeSfxVolume(v: Int) { sfxVolume = v.coerceIn(0, 100); sp.edit().putInt("sfxVol", sfxVolume).apply() }

    private fun put(k: String, v: String?) = sp.edit().putString(k, v).apply()

    fun changeBootMode(v: String) { bootMode = v; put("bootMode", v) }
    fun setFixed(v: Console) { fixedConsole = v.name; put("fixed", v.name) }
    fun setWallpaper(v: String?) { wallpaperPkg = v; put("wallpaper", v) }
    fun changeSteamId(v: String?) { steamId = v; put("steamId", v) }
    fun changeSteamKey(v: String) { steamKey = v; put("steamKey", v) }
    fun changeAskGfn(v: Boolean) { askGfn = v; sp.edit().putBoolean("askGfn", v).apply() }
    fun setIntro(c: Console, uri: String?) {
        introUris = introUris + (c to uri); put("intro_${c.name}", uri)
    }

    /** Decide qué menú de consola se muestra en este arranque. */
    fun pickConsole(): Console {
        val all = Console.values()
        return when (bootMode) {
            "FIXED" -> Console.valueOf(fixedConsole)
            "RANDOM" -> all.random()
            else -> {
                val next = (sp.getInt("last", -1) + 1) % all.size
                sp.edit().putInt("last", next).apply()
                all[next]
            }
        }
    }
}
