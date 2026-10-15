package com.example.mpvcompose.presentation.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mpvcompose.utils.TimeFormatter

/**
 * Isolated seekbar component.
 *
 * Receives lambda state providers [positionProvider] and [durationProvider].
 * Because reads happen only within this composable, frequent timestamp updates (e.g. 1Hz or 10Hz ticks)
 * trigger recomposition ONLY for this slider and its timestamp text, leaving the rest of the
 * player overlay, navigation bars, and video surface untouched.
 */
@Composable
fun IsolatedSeekBar(
    positionProvider: () -> Float,
    durationProvider: () -> Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val duration = durationProvider()
    val position = positionProvider()

    // Temporary user scrub state to avoid slider jitter during drag gestures
    var scrubPosition by remember { mutableStateOf<Float?>(null) }

    val currentDisplayPos = scrubPosition ?: position
    val validDuration = if (duration > 0f) duration else 1f
    val safeSliderValue = currentDisplayPos.coerceIn(0f, validDuration)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Slider(
            value = safeSliderValue,
            onValueChange = { newValue ->
                scrubPosition = newValue
            },
            onValueChangeFinished = {
                scrubPosition?.let { onSeek(it) }
                scrubPosition = null
            },
            valueRange = 0f..validDuration,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.24f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = TimeFormatter.formatSeconds(currentDisplayPos.toLong()),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                color = Color.White
            )

            Text(
                text = TimeFormatter.formatSeconds(duration.toLong()),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}
