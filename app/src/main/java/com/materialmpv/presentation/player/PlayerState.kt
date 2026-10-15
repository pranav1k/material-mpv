package com.materialmpv.presentation.player

data class TrackItem(
    val id: Int,
    val type: String, // "audio", "sub", "video"
    val title: String,
    val lang: String? = null,
    val isSelected: Boolean = false
) {
    val displayName: String
        get() {
            val parts = mutableListOf<String>()
            if (title.isNotBlank()) parts.add(title)
            if (!lang.isNullOrBlank()) parts.add("[$lang]")
            return if (parts.isNotEmpty()) parts.joinToString(" ") else "Track #$id"
        }
}

enum class GestureType {
    NONE,
    VOLUME,
    BRIGHTNESS,
    SEEK
}

data class GestureState(
    val type: GestureType = GestureType.NONE,
    val progress: Float = 0f, // 0.0 to 1.0 for volume & brightness
    val label: String = ""
)

enum class VideoAspectMode(val label: String) {
    FIT("Fit to Screen"),
    FILL("Crop / Fill"),
    STRETCH("Stretch 16:9")
}
