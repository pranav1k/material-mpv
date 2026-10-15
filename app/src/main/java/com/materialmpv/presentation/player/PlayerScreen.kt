package com.materialmpv.presentation.player

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.materialmpv.mpv.MpvEngine
import com.materialmpv.presentation.player.components.GestureIndicator
import com.materialmpv.presentation.player.components.GestureSurface
import com.materialmpv.presentation.player.components.PlayerControlsOverlay
import com.materialmpv.presentation.player.components.QuickSettingsSheet
import com.materialmpv.utils.WindowBrightness

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    videoUri: String,
    videoTitle: String?,
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { WindowBrightness.findActivity(context) }

    val controlsVisible by viewModel.controlsVisible.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val videoAspect by viewModel.videoAspect.collectAsState()
    val aspectMode by viewModel.aspectMode.collectAsState()
    val gestureState by viewModel.gestureState.collectAsState()
    val audioTracks by viewModel.audioTracks.collectAsState()
    val subtitleTracks by viewModel.subtitleTracks.collectAsState()
    val currentTitle by viewModel.currentTitle.collectAsState()

    var showSettingsSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Load file on first entrance
    LaunchedEffect(videoUri) {
        viewModel.playFile(videoUri, videoTitle)
    }

    // Keep screen on while video player is active
    DisposableEffect(activity) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Immersive Mode: Toggle system bars with controls
    LaunchedEffect(controlsVisible, activity) {
        val window = activity?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (controlsVisible) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    BackHandler {
        onBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Bare SurfaceView Video Viewport (Hardware overlay buffer)
        MpvViewport(
            aspectRatio = videoAspect,
            aspectMode = aspectMode,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Gesture Detection Layer
        GestureSurface(
            onSingleTap = {
                viewModel.toggleControls()
            },
            onDoubleTapSeek = { deltaSeconds ->
                viewModel.seekRelative(deltaSeconds)
            },
            onVerticalDragBrightness = { deltaRatio ->
                viewModel.adjustBrightness(activity, deltaRatio)
            },
            onVerticalDragVolume = { deltaRatio ->
                viewModel.adjustVolume(context, deltaRatio)
            },
            modifier = Modifier.fillMaxSize()
        )

        // 3. HUD Feedback for Gestures (Volume, Brightness, Double-tap seek)
        GestureIndicator(
            gestureState = gestureState,
            modifier = Modifier.align(Alignment.Center)
        )

        // 4. Material 3 Player Controls Overlay (With isolated slider recomposition)
        PlayerControlsOverlay(
            visible = controlsVisible,
            title = currentTitle,
            isPaused = isPaused,
            isBuffering = isBuffering,
            aspectMode = aspectMode,
            positionProvider = { viewModel.currentPosition },
            durationProvider = { viewModel.totalDuration },
            onBack = onBack,
            onTogglePlayPause = { viewModel.togglePlayPause() },
            onSeek = { seconds -> viewModel.seekTo(seconds) },
            onSeekRelative = { delta -> viewModel.seekRelative(delta) },
            onToggleAspectMode = { viewModel.toggleAspectMode() },
            onOpenSettings = { showSettingsSheet = true }
        )

        // 5. Track and Speed Settings Sheet
        if (showSettingsSheet) {
            QuickSettingsSheet(
                sheetState = sheetState,
                audioTracks = audioTracks,
                subtitleTracks = subtitleTracks,
                onSelectAudioTrack = { viewModel.selectAudioTrack(it) },
                onSelectSubtitleTrack = { viewModel.selectSubtitleTrack(it) },
                onSelectSpeed = { MpvEngine.setSpeed(it) },
                onDismiss = { showSettingsSheet = false }
            )
        }
    }
}
