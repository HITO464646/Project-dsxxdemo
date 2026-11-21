package com.projectdsx.demo

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

object Steam {
    /** Biblioteca de Steam. Requiere tu API key y que tu perfil/juegos sean públicos. */
    suspend fun owned(key: String, steamId: String): List<Game> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://api.steampowered.com/IPlayerService/GetOwnedGames/v1/" +
                "?key=$key&steamid=$steamId&include_appinfo=true&format=json"
            val arr = JSONObject(URL(url).readText()).getJSONObject("response").optJSONArray("games")
                ?: return@runCatching emptyList()
            (0 until arr.length()).map {
                val g = arr.getJSONObject(it)
                Game("s${g.getLong("appid")}", g.getString("name"), steamAppId = g.getLong("appid"))
            }.sortedBy { it.title }
        }.getOrDefault(emptyList())
    }

    fun cover(appId: Long) = "https://cdn.cloudflare.steamstatic.com/steam/apps/$appId/header.jpg"
}

object Gfn {
    const val PKG = "com.nvidia.geforcenow"

    /** Mapa steamAppId -> id de GeForce NOW, usando la lista pública de juegos compatibles. */
    suspend fun supportedBySteamId(): Map<Long, String> = withContext(Dispatchers.IO) {
        runCatching {
            val txt = URL("https://static.nvidiagrid.net/supported-public-game-list/locales/gfnpc-en-US.json").readText()
            val arr = JSONArray(txt)
            val out = HashMap<Long, String>()
            val re = Regex("/app/(\\d+)")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val steamUrl = o.optString("steamUrl")
                val appId = re.find(steamUrl)?.groupValues?.get(1)?.toLongOrNull() ?: continue
                out[appId] = o.get("id").toString()
            }
            out
        }.getOrDefault(emptyMap())
    }

    fun isInstalled(ctx: Context) = runCatching { ctx.packageManager.getPackageInfo(PKG, 0); true }.getOrDefault(false)

    /** Abre el juego directo en GeForce NOW (enlace profundo NO oficial; puede cambiar). */
    fun launch(ctx: Context, gfnId: String) {
        val uri = Uri.parse("https://play.geforcenow.com/mall/#/deeplink?game-id=$gfnId&lang=es_ES")
        val i = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            ctx.startActivity(Intent(i).setPackage(PKG))
        } catch (e: ActivityNotFoundException) {
            ctx.startActivity(i) // sin la app, se abre en el navegador
        }
    }
}

object Installed {
    /** Juegos de Android instalados. */
    fun games(ctx: Context): List<Game> {
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .map { it.activityInfo.applicationInfo }
            .filter {
                it.category == ApplicationInfo.CATEGORY_GAME ||
                    @Suppress("DEPRECATION") (it.flags and ApplicationInfo.FLAG_IS_GAME) != 0
            }
            .distinctBy { it.packageName }
            .map { Game("a${it.packageName}", pm.getApplicationLabel(it).toString(), pkg = it.packageName) }
            .sortedBy { it.title }
    }

    fun launch(ctx: Context, pkg: String) {
        ctx.packageManager.getLaunchIntentForPackage(pkg)?.let { ctx.startActivity(it) }
    }
}
