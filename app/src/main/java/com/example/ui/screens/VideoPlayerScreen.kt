package com.example.ui.screens

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.ContinueWatchingEntry
import com.example.data.model.Episode
import com.example.data.model.MediaServer
import com.example.data.model.MovieItem
import com.example.data.model.SeriesItem
import com.example.data.model.WatchHistoryEntry
import com.example.ui.viewmodel.HistoryViewModel
import kotlinx.coroutines.delay

private fun Context.findActivity(): ComponentActivity? {
    var current = this
    while (current is android.content.ContextWrapper) {
        if (current is ComponentActivity) return current
        current = current.baseContext
    }
    return null
}

data class MediaTrackOption(
    val groupIndex: Int,
    val trackIndex: Int,
    val mediaTrackGroup: TrackGroup,
    val label: String,
    val language: String,
    val isSelected: Boolean
)

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    movie: MovieItem? = null,
    series: SeriesItem? = null,
    initialEpisode: Episode? = null,
    initialPositionMs: Long = 0L,
    historyViewModel: HistoryViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    // Determine content metadata
    val contentTitle = movie?.title ?: series?.title ?: "Reproductor"
    val contentPoster = movie?.posterUrl ?: series?.posterUrl ?: ""
    val contentId = movie?.id ?: series?.id ?: ""
    val contentType = if (movie != null) "movie" else series?.contentType ?: "series"

    // Available episodes for series/anime
    val availableEpisodes = remember(series) {
        series?.seasons?.flatMap { it.episodes }?.filter { it.hasVideoLink() } ?: emptyList()
    }

    var currentEpisode by remember(initialEpisode, series) {
        mutableStateOf(initialEpisode ?: availableEpisodes.firstOrNull())
    }
    var serversList by remember(currentEpisode, movie) {
        mutableStateOf(movie?.servers ?: currentEpisode?.servers ?: emptyList())
    }

    var selectedServerIndex by remember(serversList) { mutableIntStateOf(0) }
    val currentServer = serversList.getOrNull(selectedServerIndex)
    val streamUrl = currentServer?.streamUrl.orEmpty()

    // Control visibility and states
    var showControls by remember { mutableStateOf(true) }
    var isControlsLocked by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(initialPositionMs) }
    var duration by remember { mutableLongStateOf(0L) }
    var selectedSpeed by remember { mutableFloatStateOf(1.0f) }

    // Aspect Ratio / Fullscreen Modes
    // RESIZE_MODE_FIT (0), RESIZE_MODE_FILL (3), RESIZE_MODE_ZOOM (4)
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var isLandscape by remember { mutableStateOf(false) }

    // Device physical orientation & manual landscape override
    val configuration = LocalConfiguration.current
    val isDeviceLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isImmersive = isLandscape || isDeviceLandscape

    val effectiveResizeMode = if (isImmersive && resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
        AspectRatioFrameLayout.RESIZE_MODE_FILL
    } else {
        resizeMode
    }

    // Dialog flags
    var showEpisodeDialog by remember { mutableStateOf(false) }
    var showServerDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSubtitlesDialog by remember { mutableStateOf(false) }
    var showResizeDialog by remember { mutableStateOf(false) }

    // Dynamic Track selection from ExoPlayer
    var audioTracksOptions by remember { mutableStateOf<List<MediaTrackOption>>(emptyList()) }
    var subtitleTracksOptions by remember { mutableStateOf<List<MediaTrackOption>>(emptyList()) }
    var isSubtitlesDisabled by remember { mutableStateOf(true) }

    // Intercept System Back Button
    BackHandler {
        if (showEpisodeDialog || showServerDialog || showSpeedDialog || showSubtitlesDialog || showResizeDialog) {
            showEpisodeDialog = false
            showServerDialog = false
            showSpeedDialog = false
            showSubtitlesDialog = false
            showResizeDialog = false
        } else if (isImmersive) {
            isLandscape = false
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        } else {
            onBack()
        }
    }

    // Interactive Slider dragging state for Netflix-style smooth seeking
    var isDragging by remember { mutableStateOf(false) }
    var dragPositionMs by remember { mutableFloatStateOf(0f) }

    // Manage Immersive Screen Insets (True Fullscreen)
    DisposableEffect(isImmersive) {
        val window = activity?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (isImmersive) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.hide(WindowInsetsCompat.Type.statusBars())
                insetsController.hide(WindowInsetsCompat.Type.navigationBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                WindowCompat.setDecorFitsSystemWindows(window, true)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                insetsController.show(WindowInsetsCompat.Type.statusBars())
                insetsController.show(WindowInsetsCompat.Type.navigationBars())
            }
        }
        onDispose {
            window?.let {
                WindowCompat.setDecorFitsSystemWindows(it, true)
                val insetsController = WindowCompat.getInsetsController(it, it.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                insetsController.show(WindowInsetsCompat.Type.statusBars())
                insetsController.show(WindowInsetsCompat.Type.navigationBars())
            }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // ExoPlayer Instance
    val exoPlayer = remember(streamUrl) {
        ExoPlayer.Builder(context).build().apply {
            if (streamUrl.isNotBlank()) {
                setMediaItem(MediaItem.fromUri(Uri.parse(streamUrl)))
                prepare()
                if (initialPositionMs > 0L) {
                    seekTo(initialPositionMs)
                }
                playWhenReady = true
            }
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    duration = exoPlayer.duration.coerceAtLeast(0L)
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                val audioList = mutableListOf<MediaTrackOption>()
                val subList = mutableListOf<MediaTrackOption>()

                for ((gIdx, group) in tracks.groups.withIndex()) {
                    val mediaTrackGroup = group.mediaTrackGroup
                    if (group.type == C.TRACK_TYPE_AUDIO) {
                        for (i in 0 until mediaTrackGroup.length) {
                            val format = mediaTrackGroup.getFormat(i)
                            val lang = format.language?.uppercase() ?: ""
                            val label = format.label ?: ""
                            val displayLabel = when {
                                label.isNotBlank() -> label
                                lang.isNotBlank() -> "Pista de Audio ($lang)"
                                else -> "Audio ${i + 1}"
                            }
                            audioList.add(
                                MediaTrackOption(
                                    groupIndex = gIdx,
                                    trackIndex = i,
                                    mediaTrackGroup = mediaTrackGroup,
                                    label = displayLabel,
                                    language = format.language ?: "Español",
                                    isSelected = group.isTrackSelected(i)
                                )
                            )
                        }
                    } else if (group.type == C.TRACK_TYPE_TEXT) {
                        for (i in 0 until mediaTrackGroup.length) {
                            val format = mediaTrackGroup.getFormat(i)
                            val lang = format.language?.uppercase() ?: ""
                            val label = format.label ?: ""
                            val displayLabel = when {
                                label.isNotBlank() -> label
                                lang.isNotBlank() -> "Subtítulo ($lang)"
                                else -> "Subtítulo ${i + 1}"
                            }
                            subList.add(
                                MediaTrackOption(
                                    groupIndex = gIdx,
                                    trackIndex = i,
                                    mediaTrackGroup = mediaTrackGroup,
                                    label = displayLabel,
                                    language = format.language ?: "Desconocido",
                                    isSelected = group.isTrackSelected(i)
                                )
                            )
                        }
                    }
                }
                audioTracksOptions = audioList
                subtitleTracksOptions = subList
                isSubtitlesDisabled = subList.none { it.isSelected }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            val pos = exoPlayer.currentPosition
            val dur = exoPlayer.duration.coerceAtLeast(1L)
            if (contentId.isNotBlank() && pos > 0) {
                historyViewModel.saveContinueWatching(
                    ContinueWatchingEntry(
                        contentId = contentId,
                        contentType = contentType,
                        title = contentTitle,
                        posterUrl = contentPoster,
                        seasonNumber = 1,
                        episodeNumber = currentEpisode?.episodeNumber ?: 0,
                        episodeTitle = currentEpisode?.title ?: "",
                        positionMs = pos,
                        durationMs = dur
                    )
                )
                val percentage = ((pos.toDouble() / dur.toDouble()) * 100).toInt().coerceIn(0, 100)
                historyViewModel.recordWatchHistory(
                    WatchHistoryEntry(
                        contentId = contentId,
                        contentType = contentType,
                        title = contentTitle,
                        posterUrl = contentPoster,
                        progressPercentage = percentage,
                        positionMs = pos,
                        durationMs = dur
                    )
                )
            }
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Auto update position
    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
            duration = exoPlayer.duration.coerceAtLeast(0L)
            delay(1000)
        }
    }

    // Auto hide controls after 4s
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(4000)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { showControls = !showControls }
    ) {
        // Player View with configurable resizeMode
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    this.resizeMode = effectiveResizeMode
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                view.player = exoPlayer
                view.resizeMode = effectiveResizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                // Top Gradient Vignette
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.90f), Color.Transparent)
                            )
                        )
                )

                // Bottom Gradient Vignette
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))
                            )
                        )
                )

                // Top Bar Content
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = contentTitle,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (currentEpisode != null) {
                            Text(
                                text = "Capítulo ${currentEpisode?.episodeNumber}: ${currentEpisode?.title}",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        } else if (currentServer != null) {
                            Text(
                                text = "Idioma: ${currentServer.language} • ${currentServer.serverName}",
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Episode Selector (Series/Anime)
                        if (series != null && availableEpisodes.isNotEmpty()) {
                            IconButton(onClick = { showEpisodeDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.List,
                                    contentDescription = "Capítulos",
                                    tint = Color.White
                                )
                            }
                        }

                        // Fullscreen toggle (enters immersive landscape mode & hides system bars)
                        IconButton(
                            onClick = {
                                if (isImmersive) {
                                    isLandscape = false
                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                } else {
                                    isLandscape = true
                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isImmersive) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Pantalla Completa",
                                tint = if (isImmersive) Color(0xFFE50914) else Color.White
                            )
                        }

                        // Aspect Ratio Dialog toggle
                        IconButton(onClick = { showResizeDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = "Modo de Aspecto",
                                tint = Color.White
                            )
                        }

                        // Lock Controls Button
                        IconButton(onClick = { isControlsLocked = !isControlsLocked }) {
                            Icon(
                                imageVector = if (isControlsLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Bloquear Controles",
                                tint = if (isControlsLocked) Color.Red else Color.White
                            )
                        }

                        // Servers / Language Selector
                        IconButton(onClick = { showServerDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = "Idioma / Audio",
                                tint = Color.White
                            )
                        }

                        // Subtitles Selector
                        IconButton(onClick = { showSubtitlesDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.ClosedCaption,
                                contentDescription = "Subtítulos",
                                tint = if (!isSubtitlesDisabled) Color(0xFFE50914) else Color.White
                            )
                        }

                        // Speed Selector
                        IconButton(onClick = { showSpeedDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Velocidad",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Middle Controls (Play / Pause / Seek 10s)
                if (!isControlsLocked) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0L)) },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "Retroceder 10s",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                            },
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE50914))
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        IconButton(
                            onClick = { exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(duration)) },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "Adelantar 10s",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // Bottom Bar (Netflix Style Progress Bar & Quick Actions)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Quick Action Buttons: Skip Intro, Skip Credits
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { exoPlayer.seekTo(exoPlayer.currentPosition + 85000) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Saltar Intro", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { exoPlayer.seekTo((duration - 10000).coerceAtLeast(0L)) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Saltar Créditos", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (!isControlsLocked) {
                        val activeSliderValue = if (isDragging) dragPositionMs else currentPosition.toFloat()
                        val totalDuration = duration.coerceAtLeast(1L)

                        // Netflix Custom Red Slider
                        Slider(
                            value = activeSliderValue.coerceIn(0f, totalDuration.toFloat()),
                            onValueChange = { newVal ->
                                isDragging = true
                                dragPositionMs = newVal
                            },
                            onValueChangeFinished = {
                                exoPlayer.seekTo(dragPositionMs.toLong())
                                isDragging = false
                            },
                            valueRange = 0f..totalDuration.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFE50914),
                                activeTrackColor = Color(0xFFE50914),
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val displayPos = if (isDragging) dragPositionMs.toLong() else currentPosition
                            val remainingMs = (duration - displayPos).coerceAtLeast(0L)

                            Text(
                                text = formatTime(displayPos),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "-${formatTime(remainingMs)}",
                                color = Color.LightGray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Display Aspect Ratio / Fullscreen Dialog
        if (showResizeDialog) {
            AlertDialog(
                onDismissRequest = { showResizeDialog = false },
                title = { Text("Ajuste de Pantalla Completa", color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val modes = listOf(
                            Triple(AspectRatioFrameLayout.RESIZE_MODE_FIT, "Ajustar a Pantalla (Normal)", "Mantiene la proporción original del video sin cortar"),
                            Triple(AspectRatioFrameLayout.RESIZE_MODE_FILL, "Estirar (Pantalla Completa)", "Ocupa toda la pantalla completando los bordes"),
                            Triple(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, "Zoom Recorte", "Amplía el video eliminando franjas negras")
                        )
                        modes.forEach { (mode, name, desc) ->
                            val isSel = resizeMode == mode
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        resizeMode = mode
                                        showResizeDialog = false
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSel) Color(0xFFE50914) else Color(0xFF2B2B2B)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(name, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(desc, fontSize = 11.sp, color = Color.LightGray)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showResizeDialog = false }) {
                        Text("Cerrar", color = Color.White)
                    }
                },
                containerColor = Color(0xFF1E1E1E)
            )
        }

        // Servers & Audio Language Dialog
        if (showServerDialog) {
            AlertDialog(
                onDismissRequest = { showServerDialog = false },
                title = { Text("Seleccionar Idioma / Audio", color = Color.White) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. External servers/links if available
                        if (serversList.isNotEmpty()) {
                            Text("Servidores y Enlaces Configurados:", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            serversList.forEachIndexed { idx, server ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedServerIndex = idx
                                            showServerDialog = false
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selectedServerIndex == idx) Color(0xFFE50914) else Color(0xFF2B2B2B)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(server.language.ifBlank { "Español Latino" }, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text("${server.serverName} • ${server.quality}", fontSize = 12.sp, color = Color.LightGray)
                                        }
                                        if (selectedServerIndex == idx) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Embedded audio tracks inside MP4/HLS stream
                        if (audioTracksOptions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Pistas de Audio Internas en el Video:", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            audioTracksOptions.forEach { opt ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                                .buildUpon()
                                                .setOverrideForType(TrackSelectionOverride(opt.mediaTrackGroup, opt.trackIndex))
                                                .build()
                                            showServerDialog = false
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (opt.isSelected) Color(0xFFE50914) else Color(0xFF2B2B2B)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(opt.label, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text("Idioma: ${opt.language}", fontSize = 11.sp, color = Color.LightGray)
                                        }
                                        if (opt.isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                        }
                                    }
                                }
                            }
                        }

                        if (serversList.isEmpty() && audioTracksOptions.isEmpty()) {
                            Text("No se detectaron pistas de audio ni servidores alternativos.", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showServerDialog = false }) {
                        Text("Cerrar", color = Color.White)
                    }
                },
                containerColor = Color(0xFF1E1E1E)
            )
        }

        // Subtitles Dialog
        if (showSubtitlesDialog) {
            AlertDialog(
                onDismissRequest = { showSubtitlesDialog = false },
                title = { Text("Seleccionar Subtítulos", color = Color.White) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Off Option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSubtitlesDisabled) Color(0xFFE50914).copy(alpha = 0.2f) else Color.Transparent)
                                .clickable {
                                    isSubtitlesDisabled = true
                                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                        .buildUpon()
                                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                        .build()
                                    showSubtitlesDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Desactivado", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            if (isSubtitlesDisabled) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFFE50914))
                            }
                        }

                        if (subtitleTracksOptions.isNotEmpty()) {
                            Text("Subtítulos Detectados:", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            subtitleTracksOptions.forEach { opt ->
                                val isSelected = !isSubtitlesDisabled && opt.isSelected
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) Color(0xFFE50914).copy(alpha = 0.2f) else Color.Transparent)
                                        .clickable {
                                            isSubtitlesDisabled = false
                                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                                .buildUpon()
                                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                                .setOverrideForType(TrackSelectionOverride(opt.mediaTrackGroup, opt.trackIndex))
                                                .build()
                                            showSubtitlesDialog = false
                                        }
                                        .padding(vertical = 12.dp, horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(opt.label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                        Text("Idioma: ${opt.language}", color = Color.Gray, fontSize = 11.sp)
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFFE50914))
                                    }
                                }
                            }
                        } else {
                            Text("No se detectaron subtítulos incrustados en este video.", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSubtitlesDialog = false }) {
                        Text("Cerrar", color = Color.White)
                    }
                },
                containerColor = Color(0xFF1E1E1E)
            )
        }

        // Speed Dialog
        if (showSpeedDialog) {
            AlertDialog(
                onDismissRequest = { showSpeedDialog = false },
                title = { Text("Velocidad de reproducción", color = Color.White) },
                text = {
                    Column {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedSpeed = speed
                                        exoPlayer.playbackParameters = PlaybackParameters(speed)
                                        showSpeedDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${speed}x", color = Color.White, fontSize = 16.sp)
                                if (selectedSpeed == speed) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFFE50914))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSpeedDialog = false }) {
                        Text("Cerrar", color = Color.White)
                    }
                },
                containerColor = Color(0xFF1E1E1E)
            )
        }

        // Episode Selection Dialog
        if (showEpisodeDialog && series != null) {
            AlertDialog(
                onDismissRequest = { showEpisodeDialog = false },
                title = { Text("Capítulos Disponibles (${availableEpisodes.size})", color = Color.White) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        availableEpisodes.forEach { ep ->
                            val isSelected = currentEpisode?.episodeNumber == ep.episodeNumber
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) Color(0xFFE50914).copy(alpha = 0.2f) else Color.Transparent)
                                    .clickable {
                                        currentEpisode = ep
                                        serversList = ep.servers
                                        selectedServerIndex = 0
                                        showEpisodeDialog = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Capítulo ${ep.episodeNumber}",
                                        color = if (isSelected) Color(0xFFE50914) else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    if (ep.title.isNotBlank() && ep.title != "Capítulo ${ep.episodeNumber}") {
                                        Text(ep.title, color = Color.Gray, fontSize = 12.sp)
                                    }
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFFE50914))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showEpisodeDialog = false }) {
                        Text("Cerrar", color = Color.White)
                    }
                },
                containerColor = Color(0xFF1E1E1E)
            )
        }
    }
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

