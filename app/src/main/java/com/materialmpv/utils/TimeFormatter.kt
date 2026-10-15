package com.materialmpv.utils

import java.util.Locale

object TimeFormatter {
    fun formatSeconds(seconds: Long): String {
        val sec = if (seconds < 0) 0 else seconds
        val hours = sec / 3600
        val minutes = (sec % 3600) / 60
        val remainingSeconds = sec % 60

        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, remainingSeconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, remainingSeconds)
        }
    }
}
