package com.projectdsx.demo

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator
import android.net.Uri
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.runtime.compositionLocalOf

val LocalApp = compositionLocalOf<AppState> { error("AppState no disponible") }
val LocalProfiles = compositionLocalOf<ProfileStore> { error("ProfileStore no disponible") }

/** Sonidos de navegación: sistema, tono propio, archivo personalizado o silencio. */
object Sfx {
    private var pool: SoundPool? = null
    private var soundId = 0
    private var ready = false
    private var loadedUri: String? = null
    private var tone: ToneGenerator? = null

    fun nav(view: View, st: AppState) {
        when (st.navSoundMode) {
            "OFF" -> Unit
            "SYSTEM" -> view.playSoundEffect(SoundEffectConstants.NAVIGATION_RIGHT)
            "TONE" -> {
                if (tone == null) tone = ToneGenerator(AudioManager.STREAM_MUSIC, st.sfxVolume)
                tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 35)
            }
            "CUSTOM" -> custom(view.context, st)
        }
    }

    private fun custom(ctx: Context, st: AppState) {
        val uri = st.navSoundUri ?: return
        if (pool == null) {
            pool = SoundPool.Builder().setMaxStreams(4)
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build()).build()
            pool!!.setOnLoadCompleteListener { _, _, status -> ready = status == 0 }
        }
        if (loadedUri != uri) {
            ready = false; loadedUri = uri
            runCatching {
                ctx.contentResolver.openAssetFileDescriptor(Uri.parse(uri), "r")?.use { soundId = pool!!.load(it, 1) }
            }
            return
        }
        if (ready) { val v = st.sfxVolume / 100f; pool!!.play(soundId, v, v, 1, 0, 1f) }
    }

    /** Sonido corto de "entraste / confirmar". */
    fun confirm(st: AppState) {
        ToneGenerator(AudioManager.STREAM_MUSIC, st.sfxVolume).startTone(ToneGenerator.TONE_PROP_ACK, 250)
    }
}
