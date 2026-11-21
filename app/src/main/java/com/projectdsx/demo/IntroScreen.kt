package com.projectdsx.demo

import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

/**
 * Reproduce el video de intro que elegiste para esa consola (Ajustes > Videos de intro).
 * Si no hay video, muestra una animación de logo de texto. Toca la pantalla para saltar.
 */
@Composable
fun IntroScreen(console: Console, videoUri: String?, onDone: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color.Black).clickable { onDone() },
        contentAlignment = Alignment.Center
    ) {
        if (videoUri != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(Uri.parse(videoUri))
                        setOnPreparedListener { it.isLooping = false }
                        setOnCompletionListener { onDone() }
                        setOnErrorListener { _, _, _ -> onDone(); true }
                        start()
                    }
                }
            )
        } else {
            val a = remember { Animatable(0f) }
            LaunchedEffect(console) {
                a.animateTo(1f, tween(900))
                delay(900)
                a.animateTo(0f, tween(600))
                onDone()
            }
            Text(
                console.label.uppercase(), color = Color.White, fontSize = 44.sp,
                fontWeight = FontWeight.Light, modifier = Modifier.alpha(a.value)
            )
        }
    }
}
