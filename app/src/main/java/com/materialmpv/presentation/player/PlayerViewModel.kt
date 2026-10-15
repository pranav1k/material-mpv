package com.materialmpv.presentation.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.materialmpv.mpv.MpvEngine
import com.materialmpv.utils.WindowBrightness
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerViewModel : ViewModel(), MPVLib.EventObserver {

    // Compose snapshot states for isolated seekbar recomposition
    var currentPosition by mutableFloatStateOf(0f)
        private set

    var totalDuration by mutableFloatStateOf(0f)
        private set

    private val _position = MutableStateFlow(0f)
    val position: StateFlow<Float> = _position.asStateFlow()

    private val _duration = MutableStateFlow(0f)
    val duration: StateFlow<Float> = _duration.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _videoAspect = MutableStateFlow(16.0 / 9.0)
    val videoAspect: StateFlow<Double> = _videoAspect.asStateFlow()

    private val _controlsVisible = MutableStateFlow(true)
    val controlsVisible: StateFlow<Boolean> = _controlsVisible.asStateFlow()

    private val _gestureState = MutableStateFlow(GestureState())
    val gestureState: StateFlow<GestureState> = _gestureState.asStateFlow()

    private val _aspectMode = MutableStateFlow(VideoAspectMode.FIT)
    val aspectMode: StateFlow<VideoAspectMode> = _aspectMode.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<TrackItem>>(emptyList())
    val audioTracks: StateFlow<List<TrackItem>> = _audioTracks.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<TrackItem>>(emptyList())
    val subtitleTracks: StateFlow<List<TrackItem>> = _subtitleTracks.asStateFlow()

    private val _currentTitle = MutableStateFlow("")
    val currentTitle: StateFlow<String> = _currentTitle.asStateFlow()

    private var autoHideJob: Job? = null
    private var gestureHideJob: Job? = null
    private var positionSyncJob: Job? = null

    init {
        MpvEngine.addObserver(this)
        observeCoreProperties()
        resetAutoHideTimer()
        startPositionSync()
    }

    private fun observeCoreProperties() {
        // mpv dispatches time-pos primarily as INT64
        MpvEngine.observeProperty("time-pos", MPVLib.mpvFormat.MPV_FORMAT_INT64)
        MpvEngine.observeProperty("duration", MPVLib.mpvFormat.MPV_FORMAT_INT64)
        MpvEngine.observeProperty("duration/full", MPVLib.mpvFormat.MPV_FORMAT_DOUBLE)
        MpvEngine.observeProperty("pause", MPVLib.mpvFormat.MPV_FORMAT_FLAG)
        MpvEngine.observeProperty("track-list", MPVLib.mpvFormat.MPV_FORMAT_NONE)
        MpvEngine.observeProperty("sid", MPVLib.mpvFormat.MPV_FORMAT_INT64)
        MpvEngine.observeProperty("aid", MPVLib.mpvFormat.MPV_FORMAT_INT64)
        MpvEngine.observeProperty("video-params/aspect", MPVLib.mpvFormat.MPV_FORMAT_DOUBLE)
        MpvEngine.observeProperty("paused-for-cache", MPVLib.mpvFormat.MPV_FORMAT_FLAG)
        MpvEngine.observeProperty("media-title", MPVLib.mpvFormat.MPV_FORMAT_STRING)
    }

    private fun startPositionSync() {
        positionSyncJob?.cancel()
        positionSyncJob = viewModelScope.launch(Dispatchers.Main) {
            while (isActive) {
                if (!_isPaused.value) {
                    val pos = MPVLib.getPropertyInt("time-pos") ?: MPVLib.getPropertyDouble("time-pos")?.toInt()
                    val dur = MPVLib.getPropertyInt("duration") ?: MPVLib.getPropertyDouble("duration")?.toInt()

                    if (pos != null && pos >= 0) {
                        updatePosition(pos.toFloat())
                    }
                    if (dur != null && dur > 0) {
                        updateDuration(dur.toFloat())
                    }
                }
                delay(500)
            }
        }
    }

    private fun updatePosition(seconds: Float) {
        _position.value = seconds
        viewModelScope.launch(Dispatchers.Main.immediate) {
            currentPosition = seconds
        }
    }

    private fun updateDuration(seconds: Float) {
        if (seconds <= 0f) return
        _duration.value = seconds
        viewModelScope.launch(Dispatchers.Main.immediate) {
            totalDuration = seconds
        }
    }

    fun playFile(uri: String, title: String? = null) {
        _currentTitle.value = title ?: uri.substringAfterLast('/')
        updatePosition(0f)
        updateDuration(0f)
        MpvEngine.loadFile(uri)
        startPositionSync()
    }

    fun togglePlayPause() {
        MpvEngine.togglePause()
        resetAutoHideTimer()
    }

    fun seekTo(seconds: Float) {
        MpvEngine.seekAbsolute(seconds.toDouble())
        updatePosition(seconds)
        resetAutoHideTimer()
    }

    fun seekRelative(seconds: Float) {
        MpvEngine.seekRelative(seconds.toDouble())
        val newPos = (_position.value + seconds).coerceIn(0f, _duration.value.coerceAtLeast(0f))
        updatePosition(newPos)
        showSeekFeedback(seconds)
        resetAutoHideTimer()
    }

    fun toggleControls() {
        val willShow = !_controlsVisible.value
        _controlsVisible.value = willShow
        if (willShow) {
            resetAutoHideTimer()
        } else {
            autoHideJob?.cancel()
        }
    }

    fun showControls() {
        _controlsVisible.value = true
        resetAutoHideTimer()
    }

    fun hideControls() {
        _controlsVisible.value = false
        autoHideJob?.cancel()
    }

    fun resetAutoHideTimer() {
        autoHideJob?.cancel()
        if (!_isPaused.value) {
            autoHideJob = viewModelScope.launch {
                delay(3500)
                _controlsVisible.value = false
            }
        }
    }

    fun toggleAspectMode() {
        _aspectMode.value = when (_aspectMode.value) {
            VideoAspectMode.FIT -> VideoAspectMode.FILL
            VideoAspectMode.FILL -> VideoAspectMode.STRETCH
            VideoAspectMode.STRETCH -> VideoAspectMode.FIT
        }
    }

    fun selectAudioTrack(id: Int) {
        MpvEngine.setAudioTrack(id)
        refreshTracks()
    }

    fun selectSubtitleTrack(id: Int) {
        MpvEngine.setSubtitleTrack(id)
        refreshTracks()
    }

    // Gesture handling: Volume
    fun adjustVolume(context: Context, deltaRatio: Float) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val delta = (deltaRatio * maxVolume).toInt()
        val target = (currentVolume + delta).coerceIn(0, maxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)

        val progress = target.toFloat() / maxVolume.toFloat()
        updateGestureState(
            GestureState(
                type = GestureType.VOLUME,
                progress = progress,
                label = "${(progress * 100).toInt()}%"
            )
        )
    }

    // Gesture handling: Brightness
    fun adjustBrightness(activity: Activity?, deltaRatio: Float) {
        val current = WindowBrightness.getBrightness(activity)
        val target = (current + deltaRatio).coerceIn(0.01f, 1.0f)
        WindowBrightness.setBrightness(activity, target)
        updateGestureState(
            GestureState(
                type = GestureType.BRIGHTNESS,
                progress = target,
                label = "${(target * 100).toInt()}%"
            )
        )
    }

    private fun showSeekFeedback(deltaSeconds: Float) {
        val sign = if (deltaSeconds >= 0) "+" else ""
        updateGestureState(
            GestureState(
                type = GestureType.SEEK,
                progress = 0f,
                label = "$sign${deltaSeconds.toInt()}s"
            )
        )
    }

    private fun updateGestureState(state: GestureState) {
        _gestureState.value = state
        gestureHideJob?.cancel()
        gestureHideJob = viewModelScope.launch {
            delay(1200)
            _gestureState.value = GestureState(type = GestureType.NONE)
        }
    }

    private fun refreshTracks() {
        try {
            val count = MPVLib.getPropertyInt("track-list/count") ?: 0
            val audio = mutableListOf<TrackItem>()
            val subs = mutableListOf<TrackItem>()
            val selectedAid = MPVLib.getPropertyInt("aid") ?: -1
            val selectedSid = MPVLib.getPropertyInt("sid") ?: -1

            for (i in 0 until count) {
                val trackType = MPVLib.getPropertyString("track-list/$i/type") ?: continue
                val trackId = MPVLib.getPropertyInt("track-list/$i/id") ?: (i + 1)
                val trackTitle = MPVLib.getPropertyString("track-list/$i/title") ?: ""
                val trackLang = MPVLib.getPropertyString("track-list/$i/lang")

                when (trackType) {
                    "audio" -> audio.add(
                        TrackItem(
                            id = trackId,
                            type = trackType,
                            title = trackTitle,
                            lang = trackLang,
                            isSelected = trackId == selectedAid
                        )
                    )
                    "sub" -> subs.add(
                        TrackItem(
                            id = trackId,
                            type = trackType,
                            title = trackTitle,
                            lang = trackLang,
                            isSelected = trackId == selectedSid
                        )
                    )
                }
            }
            _audioTracks.value = audio
            _subtitleTracks.value = subs
        } catch (e: Exception) {
            Log.w("PlayerViewModel", "Error reading tracks: ${e.message}")
        }
    }

    // MPVLib.EventObserver implementation
    override fun eventProperty(property: String) {
        when (property) {
            "time-pos" -> {
                val pos = MPVLib.getPropertyInt("time-pos") ?: MPVLib.getPropertyDouble("time-pos")?.toInt()
                if (pos != null && pos >= 0) updatePosition(pos.toFloat())
            }
            "duration" -> {
                val dur = MPVLib.getPropertyInt("duration") ?: MPVLib.getPropertyDouble("duration")?.toInt()
                if (dur != null && dur > 0) updateDuration(dur.toFloat())
            }
            "track-list", "aid", "sid" -> refreshTracks()
        }
    }

    override fun eventProperty(property: String, value: Long) {
        when (property) {
            "time-pos" -> updatePosition(value.toFloat())
            "duration" -> updateDuration(value.toFloat())
            "aid", "sid" -> refreshTracks()
        }
    }

    override fun eventProperty(property: String, value: Boolean) {
        when (property) {
            "pause" -> {
                _isPaused.value = value
                if (value) autoHideJob?.cancel() else resetAutoHideTimer()
            }
            "paused-for-cache" -> {
                _isBuffering.value = value
            }
        }
    }

    override fun eventProperty(property: String, value: String) {
        when (property) {
            "media-title" -> if (value.isNotBlank()) _currentTitle.value = value
        }
    }

    override fun eventProperty(property: String, value: Double) {
        when (property) {
            "time-pos" -> updatePosition(value.toFloat())
            "duration", "duration/full" -> updateDuration(value.toFloat())
            "video-params/aspect" -> if (value > 0.01) _videoAspect.value = value
        }
    }

    override fun eventProperty(property: String, value: MPVNode) {
        // Handled via individual properties
    }

    override fun event(eventId: Int) {
        // Query duration and position on playback start/seek restart
        val dur = MPVLib.getPropertyInt("duration") ?: MPVLib.getPropertyDouble("duration")?.toInt()
        if (dur != null && dur > 0) updateDuration(dur.toFloat())
        val pos = MPVLib.getPropertyInt("time-pos") ?: MPVLib.getPropertyDouble("time-pos")?.toInt()
        if (pos != null && pos >= 0) updatePosition(pos.toFloat())
    }

    override fun onCleared() {
        super.onCleared()
        positionSyncJob?.cancel()
        MpvEngine.removeObserver(this)
    }
}
