package com.example.game.render

import android.opengl.GLSurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.game.engine.SimulationEngine
import com.example.game.model.GameSettings

@Composable
fun GameGLSurfaceView(
    simulation: SimulationEngine,
    settings: GameSettings,
    isPlaying: Boolean,
    onFpsMeasured: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val renderer = remember(simulation) {
        ShadowGLRenderer(
            simulation = simulation,
            settingsProvider = { settings },
            onFpsMeasured = onFpsMeasured
        )
    }

    val glView = remember {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(3)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    DisposableEffect(isPlaying) {
        if (isPlaying) {
            glView.onResume()
        } else {
            glView.onPause()
        }
        onDispose { }
    }

    DisposableEffect(Unit) {
        onDispose {
            glView.onPause()
        }
    }

    AndroidView(
        factory = { glView },
        update = { view ->
            val scale = settings.resolutionScale.coerceIn(0.6f, 1.0f)
            val displayMetrics = context.resources.displayMetrics
            val targetW = (displayMetrics.widthPixels * scale).toInt().coerceAtLeast(640)
            val targetH = (displayMetrics.heightPixels * scale).toInt().coerceAtLeast(360)
            view.holder.setFixedSize(targetW, targetH)
        },
        modifier = modifier
    )
}
