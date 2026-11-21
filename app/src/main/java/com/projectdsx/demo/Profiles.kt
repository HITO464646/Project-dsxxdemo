package com.projectdsx.demo

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.app.usage.UsageStatsManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import org.json.JSONObject
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Marcos de perfil comprables con puntos. */
data class Frame(val id: String, val name: String, val price: Int, val colors: List<Color>, val animated: Boolean)

val FRAMES = listOf(
    Frame("none", "Sin marco", 0, listOf(Color(0xFF555555)), false),
    Frame("green", "Verde Xbox", 150, listOf(Color(0xFF9BF00B)), false),
    Frame("gold", "Dorado", 300, listOf(Color(0xFFFFC83D)), false),
    Frame("neon", "Neón animado", 600, listOf(Color(0xFF00E5FF), Color(0xFFFF2BD6), Color(0xFF9BF00B)), true),
    Frame("fire", "Fuego animado", 900, listOf(Color(0xFFFF3D00), Color(0xFFFFC83D)), true)
)

data class Mission(val id: String, val title: String, val minutes: Int, val reward: Int)

val MISSIONS = listOf(
    Mission("m30", "Juega 30 minutos hoy", 30, 50),
    Mission("m60", "Juega 1 hora hoy", 60, 100),
    Mission("m120", "Juega 2 horas hoy", 120, 200)
)

/** Hitos de horas por juego (logros propios de la app). 5 hitos = platino. */
val HOUR_MILESTONES = listOf(1, 5, 10, 25, 50)

object Usage {
    fun hasAccess(ctx: Context): Boolean {
        val ops = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        @Suppress("DEPRECATION")
        return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName) ==
            AppOpsManager.MODE_ALLOWED
    }

    fun openSettings(ctx: Context) =
        ctx.startActivity(Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    /** Milisegundos en primer plano por paquete desde [from]. */
    fun playMs(ctx: Context, from: Long): Map<String, Long> {
        if (!hasAccess(ctx)) return emptyMap()
        val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        return usm.queryAndAggregateUsageStats(from, System.currentTimeMillis())
            .mapValues { it.value.totalTimeInForeground }.filterValues { it > 0 }
    }

    fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

/** Cuentas locales de la app: usuario + contraseña, puntos, horas, misiones y marcos. */
class ProfileStore(ctx: Context) {
    private val sp = ctx.getSharedPreferences("dsx_profiles", Context.MODE_PRIVATE)

    var current by mutableStateOf(sp.getString("current", null)); private set
    var tick by mutableIntStateOf(0); private set   // se lee en la UI para refrescar
    var names by mutableStateOf(readNames()); private set

    private fun readNames(): List<String> = sp.getString("names", "")!!.split("\n").filter { it.isNotBlank() }
    private fun k(n: String, key: String) = "p_${n}_$key"
    private fun bump() { tick++ }

    private fun hash(pass: String, salt: String): String =
        MessageDigest.getInstance("SHA-256").digest((salt + pass).toByteArray()).joinToString("") { "%02x".format(it) }

    fun create(name: String, pass: String): String? {
        val n = name.trim()
        if (n.length < 3) return "El usuario debe tener al menos 3 letras"
        if (pass.length < 4) return "La contraseña debe tener al menos 4 caracteres"
        if (n.contains("\n")) return "Usuario no válido"
        if (names.any { it.equals(n, true) }) return "Ese usuario ya existe"
        val salt = java.util.UUID.randomUUID().toString()
        sp.edit()
            .putString("names", (names + n).joinToString("\n"))
            .putString(k(n, "salt"), salt).putString(k(n, "hash"), hash(pass, salt))
            .putLong(k(n, "created"), System.currentTimeMillis())
            .putLong(k(n, "lastSync"), System.currentTimeMillis())
            .putString("current", n).apply()
        names = readNames(); current = n; bump()
        return null
    }

    fun login(name: String, pass: String): String? {
        val n = names.firstOrNull { it.equals(name.trim(), true) } ?: return "Usuario no encontrado"
        val salt = sp.getString(k(n, "salt"), "") ?: ""
        if (hash(pass, salt) != sp.getString(k(n, "hash"), "")) return "Contraseña incorrecta"
        sp.edit().putString("current", n).apply(); current = n; bump()
        return null
    }

    fun logout() { sp.edit().remove("current").apply(); current = null; bump() }

    fun delete(n: String) {
        val e = sp.edit()
        sp.all.keys.filter { it.startsWith("p_${n}_") }.forEach { e.remove(it) }
        e.putString("names", names.filter { it != n }.joinToString("\n"))
        if (current == n) e.remove("current")
        e.apply(); names = readNames(); if (current == n) current = null; bump()
    }

    // ---- datos del perfil actual ----
    private fun cur() = current ?: ""
    var bio: String
        get() = sp.getString(k(cur(), "bio"), "") ?: ""
        set(v) { sp.edit().putString(k(cur(), "bio"), v).apply(); bump() }
    val points: Int get() = sp.getInt(k(cur(), "points"), 0)
    val createdAt: Long get() = sp.getLong(k(cur(), "created"), System.currentTimeMillis())
    val daysInApp: Int get() = ((System.currentTimeMillis() - createdAt) / 86_400_000L).toInt() + 1
    val frameId: String get() = sp.getString(k(cur(), "frame"), "none") ?: "none"
    val frame: Frame get() = FRAMES.firstOrNull { it.id == frameId } ?: FRAMES[0]
    fun ownsFrame(id: String) = id == "none" || (sp.getString(k(cur(), "owned"), "") ?: "").split(",").contains(id)

    private fun addPoints(n: Int) { sp.edit().putInt(k(cur(), "points"), points + n).apply(); bump() }

    fun buyFrame(f: Frame): Boolean {
        if (ownsFrame(f.id) || points < f.price) return false
        val owned = (sp.getString(k(cur(), "owned"), "") ?: "") + "," + f.id
        sp.edit().putString(k(cur(), "owned"), owned).putInt(k(cur(), "points"), points - f.price).apply(); bump()
        return true
    }
    fun equipFrame(f: Frame) { if (ownsFrame(f.id)) { sp.edit().putString(k(cur(), "frame"), f.id).apply(); bump() } }

    /** Milisegundos totales jugados por paquete (acumulados desde que existe la cuenta). */
    fun totals(): Map<String, Long> {
        val j = JSONObject(sp.getString(k(cur(), "totals"), "{}") ?: "{}")
        return j.keys().asSequence().associateWith { j.getLong(it) }
    }
    val totalMs: Long get() = totals().values.sum()
    fun platinums(): Int = totals().count { (it.value / 3_600_000L).toInt() >= HOUR_MILESTONES.last() }

    /** Suma las horas jugadas desde la última sincronización y entrega puntos (10/hora + 50 por hito). */
    fun sync(ctx: Context, gamePkgs: Set<String>) {
        if (current == null || !Usage.hasAccess(ctx)) return
        val last = sp.getLong(k(cur(), "lastSync"), System.currentTimeMillis())
        val delta = Usage.playMs(ctx, last).filterKeys { it in gamePkgs }
        val now = System.currentTimeMillis()
        val tot = JSONObject(sp.getString(k(cur(), "totals"), "{}") ?: "{}")
        var gained = 0
        for ((p, ms) in delta) {
            val before = tot.optLong(p, 0)
            val after = before + ms
            tot.put(p, after)
            val hb = (before / 3_600_000L).toInt(); val ha = (after / 3_600_000L).toInt()
            gained += (ha - hb) * 10 + HOUR_MILESTONES.count { it in (hb + 1)..ha } * 50
        }
        sp.edit().putString(k(cur(), "totals"), tot.toString()).putLong(k(cur(), "lastSync"), now).apply()
        if (gained > 0) addPoints(gained) else bump()
    }

    // ---- misiones diarias (se reinician solas a las 12 am, hora local) ----
    private fun today() = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
    private fun claimed(): Set<String> = (sp.getString(k(cur(), "claimed_${today()}"), "") ?: "").split(",").toSet()
    fun isClaimed(m: Mission) = m.id in claimed()
    fun claim(m: Mission, minutesToday: Int): Boolean {
        if (isClaimed(m) || minutesToday < m.minutes) return false
        sp.edit().putString(k(cur(), "claimed_${today()}"), (claimed() + m.id).joinToString(",")).apply()
        addPoints(m.reward); return true
    }
}
