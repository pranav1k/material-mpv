package com.example.mpvcompose.presentation.player

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.example.mpvcompose.mpv.MpvEngine

/**
 * Bare SurfaceView viewport bound into Jetpack Compose.
 *
 * Renders directly to the hardware overlay plane (bypassing TextureView's extra GPU copy),
 * ensuring zero recomposition overhead on the video frame pipeline and full HDR support.
 */
@Composable
fun MpvViewport(
    aspectRatio: Double,
    aspectMode: VideoAspectMode,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val viewModifier = when (aspectMode) {
            VideoAspectMode.FIT -> {
                val ratio = if (aspectRatio > 0.01) aspectRatio.toFloat() else (16f / 9f)
                Modifier.aspectRatio(ratio, matchHeightConstraintsFirst = false)
            }
            VideoAspectMode.FILL, VideoAspectMode.STRETCH -> {
                Modifier.fillMaxSize()
            }
        }

        AndroidView(
            modifier = viewModifier,
            factory = { context ->
                SurfaceView(context).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            MpvEngine.attachSurface(holder.surface)
                        }

                        override fun surfaceChanged(
                            holder: SurfaceHolder,
                            format: Int,
                            width: Int,
                            height: Int
                        ) {
                            MpvEngine.updateSurfaceSize(width, height)
                        }

                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            MpvEngine.detachSurface()
                        }
                    })
                }
            }
        )
    }
}
