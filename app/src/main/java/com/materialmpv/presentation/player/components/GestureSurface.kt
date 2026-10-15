package com.materialmpv.presentation.player.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize

/**
 * Transparent gesture overlay layered over the MpvViewport.
 *
 * Handles:
 * - Single-tap to toggle player controls visibility.
 * - Double-tap left/right half to seek -10s or +10s.
 * - Vertical drag left half for screen brightness.
 * - Vertical drag right half for AudioManager volume.
 */
@Composable
fun GestureSurface(
    onSingleTap: () -> Unit,
    onDoubleTapSeek: (seconds: Float) -> Unit,
    onVerticalDragBrightness: (deltaRatio: Float) -> Unit,
    onVerticalDragVolume: (deltaRatio: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var touchStartX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { viewSize = it }
            // Vertical drag handler for Brightness (Left) and Volume (Right)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        touchStartX = offset.x
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        val height = viewSize.height.toFloat().coerceAtLeast(1f)
                        // Dragging UP should increase, dragging DOWN should decrease
                        val deltaRatio = -dragAmount / height

                        val halfWidth = viewSize.width / 2f
                        if (touchStartX < halfWidth) {
                            onVerticalDragBrightness(deltaRatio)
                        } else {
                            onVerticalDragVolume(deltaRatio)
                        }
                    }
                )
            }
            // Tap gestures: Single tap for controls toggle, Double tap for 10s seek
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onSingleTap()
                    },
                    onDoubleTap = { offset ->
                        val halfWidth = viewSize.width / 2f
                        if (offset.x < halfWidth) {
                            onDoubleTapSeek(-10f)
                        } else {
                            onDoubleTapSeek(10f)
                        }
                    }
                )
            }
    )
}
