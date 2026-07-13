package com.sarrows.app.ui.player

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.sarrows.app.ui.theme.*
import com.sarrows.app.ui.viewmodels.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlayerActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on during playback
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val contentType = intent.getStringExtra("contentType") ?: "movie"
        val contentId   = intent.getStringExtra("contentId") ?: ""
        val title       = intent.getStringExtra("title") ?: ""

        setContent {
            SarrowsTheme {
                PlayerScreen(
                    contentType = contentType,
                    contentId   = contentId,
                    title       = title,
                    playerViewModel = playerViewModel,
                    onBack = { finish() }
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    contentType: String,
    contentId: String,
    title: String,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val uiState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(contentId, contentType) {
        when (contentType) {
            "movie"   -> playerViewModel.loadMovie(contentId)
            "episode" -> playerViewModel.loadEpisode(contentId)
            "series"  -> playerViewModel.loadEpisode(contentId) // first episode
            else      -> playerViewModel.loadMovie(contentId)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SarrowsBlack),
        contentAlignment = Alignment.Center
    ) {
        when (val state = uiState) {
            is PlayerUiState.Loading -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = SarrowsRed)
                    Spacer(Modifier.height(16.dp))
                    Text("Loading stream…", color = SarrowsWhite60)
                }
            }

            is PlayerUiState.StreamReady -> {
                val exoPlayer = remember(state.url) {
                    ExoPlayer.Builder(context).build().apply {
                        val mediaItem = MediaItem.fromUri(state.url)
                        setMediaItem(mediaItem)
                        prepare()
                        playWhenReady = true
                    }
                }

                DisposableEffect(exoPlayer) {
                    onDispose {
                        // Save progress before releasing
                        val progress = (exoPlayer.currentPosition / 1000).toInt()
                        val targetType = if (contentType == "episode") "Episode" else "Movie"
                        if (progress > 5) {
                            playerViewModel.saveProgress(targetType, contentId, progress)
                        }
                        exoPlayer.release()
                    }
                }

                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                        }
                    }
                )

                // Record view once
                LaunchedEffect(contentId) {
                    val targetType = if (contentType == "episode") "Episode" else "Movie"
                    playerViewModel.recordView(targetType, contentId)
                }

                // Back button overlay
                Box(modifier = Modifier.fillMaxSize()) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, null, tint = SarrowsWhite, modifier = Modifier.size(28.dp))
                    }
                }
            }

            is PlayerUiState.EmbedReady -> {
                // WebView for embed content
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SarrowsDark)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, null, tint = SarrowsWhite)
                        }
                        Text(title, style = MaterialTheme.typography.titleSmall, color = SarrowsWhite)
                    }
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            android.webkit.WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.mediaPlaybackRequiresUserGesture = false
                                settings.domStorageEnabled = true
                                settings.allowFileAccess = false
                                loadUrl(state.url)
                            }
                        }
                    )
                }
            }

            is PlayerUiState.Error -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(Icons.Default.ErrorOutline, null, tint = SarrowsRed, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Playback Error", style = MaterialTheme.typography.titleMedium, color = SarrowsWhite)
                    Spacer(Modifier.height(8.dp))
                    Text(state.message, style = MaterialTheme.typography.bodySmall, color = SarrowsWhite60)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = SarrowsRed)) {
                        Text("Go Back")
                    }
                }
            }

            is PlayerUiState.Unauthenticated -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(Icons.Default.Lock, null, tint = SarrowsWhite60, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Sign in to watch", style = MaterialTheme.typography.titleMedium, color = SarrowsWhite)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = SarrowsRed)) {
                        Text("Go Back")
                    }
                }
            }

            is PlayerUiState.RateLimited -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(Icons.Default.Timer, null, tint = SarrowsGold, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Too many requests", style = MaterialTheme.typography.titleMedium, color = SarrowsWhite)
                    Spacer(Modifier.height(8.dp))
                    Text("Please wait a moment before streaming again.", color = SarrowsWhite60)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onBack) { Text("Go Back") }
                }
            }

            else -> {}
        }
    }
}
