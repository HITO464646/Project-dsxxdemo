package com.projectdsx.demo

import android.app.Notification
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/**
 * Escucha las notificaciones de Google Play Juegos. Cuando detecta un "logro desbloqueado"
 * suena el sonido y muestra una etiqueta flotante al costado, que luego desaparece sola.
 * (Mejor esfuerzo: Google no ofrece una API para leer logros de otros juegos.)
 */
class AchievementService : NotificationListenerService() {
    private val sources = setOf("com.google.android.play.games", "com.google.android.gms")
    private val keywords = Regex("logro|achievement|desbloque|unlocked", RegexOption.IGNORE_CASE)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in sources) return
        if (!AppState(this).achEnabled) return
        val ex = sbn.notification.extras
        val title = ex.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = ex.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (!keywords.containsMatchIn("$title $text")) return
        AchievementAlert.show(this, header = title.ifBlank { "Logro desbloqueado" }, name = text)
    }
}

object AchievementAlert {
    private val main = Handler(Looper.getMainLooper())

    fun show(ctx: Context, header: String, name: String) = main.post {
        playSound(ctx)
        if (!Settings.canDrawOverlays(ctx)) {
            Toast.makeText(ctx, "🏆 $header  $name", Toast.LENGTH_LONG).show()
            return@post
        }
        val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val d = ctx.resources.displayMetrics.density
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16 * d).toInt(), (10 * d).toInt(), (20 * d).toInt(), (10 * d).toInt())
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#E61B2838")); cornerRadius = 12 * d
                setStroke((2 * d).toInt(), Color.parseColor("#1A9FFF"))
            }
            addView(TextView(ctx).apply {
                text = "🏆  $header"; setTextColor(Color.parseColor("#66C0F4")); textSize = 12f
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(ctx).apply { text = name; setTextColor(Color.WHITE); textSize = 15f; maxWidth = (260 * d).toInt() })
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.END or Gravity.BOTTOM; x = (16 * d).toInt(); y = (24 * d).toInt() }
        runCatching { wm.addView(box, lp) }
        main.postDelayed({ runCatching { wm.removeView(box) } }, 5000)
    }

    private fun playSound(ctx: Context) {
        val uri = AppState(ctx).achSound
        if (uri != null) {
            runCatching {
                MediaPlayer.create(ctx, Uri.parse(uri))?.apply {
                    setOnCompletionListener { it.release() }; start()
                }
            }.onSuccess { if (it != null) return }
        }
        // Sonido propio por defecto (dos tonos); puedes elegir tu archivo en Ajustes.
        ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100).apply {
            startTone(ToneGenerator.TONE_PROP_ACK, 250)
            main.postDelayed({ startTone(ToneGenerator.TONE_PROP_BEEP2, 350) }, 280)
            main.postDelayed({ release() }, 900)
        }
    }
}

fun hasListenerAccess(ctx: Context): Boolean =
    Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners")
        ?.contains(ctx.packageName) == true
