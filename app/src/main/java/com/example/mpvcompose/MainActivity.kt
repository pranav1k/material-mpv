package com.example.mpvcompose

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.mpvcompose.mpv.MpvEngine
import com.example.mpvcompose.presentation.home.HomeScreen
import com.example.mpvcompose.presentation.player.PlayerScreen
import com.example.mpvcompose.presentation.player.PlayerViewModel
import com.example.mpvcompose.presentation.theme.MpvComposeTheme

class MainActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()

    private var activeMediaUri by mutableStateOf<String?>(null)
    private var activeMediaTitle by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize libmpv engine with hardware decoding and fast preset
        MpvEngine.initialize(applicationContext)

        // Handle intent if opened via file manager or external link
        handleIntent(intent)

        setContent {
            MpvComposeTheme {
                val currentUri = activeMediaUri
                if (currentUri != null) {
                    PlayerScreen(
                        videoUri = currentUri,
                        videoTitle = activeMediaTitle,
                        viewModel = playerViewModel,
                        onBack = {
                            activeMediaUri = null
                            activeMediaTitle = null
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    HomeScreen(
                        onPlayMedia = { uri, title ->
                            activeMediaUri = uri
                            activeMediaTitle = title
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        activeMediaUri = uri.toString()
        activeMediaTitle = uri.lastPathSegment
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            MpvEngine.destroy()
        }
    }
}
