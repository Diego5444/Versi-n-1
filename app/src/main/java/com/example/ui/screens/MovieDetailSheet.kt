package com.example.ui.screens

import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.example.data.model.Episode
import com.example.data.model.Season
import com.example.data.model.MovieItem
import com.example.data.model.SeriesItem
import com.example.ui.viewmodel.FavoritesViewModel
import com.example.ui.viewmodel.HistoryViewModel

@OptIn(UnstableApi::class)
@ExperimentalMaterial3Api
@Composable
fun MovieDetailSheet(
    movie: MovieItem? = null,
    series: SeriesItem? = null,
    favoritesViewModel: FavoritesViewModel,
    historyViewModel: HistoryViewModel,
    onPlayClick: () -> Unit,
    onPlayEpisode: ((SeriesItem, Episode) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    BackHandler { onDismiss() }

    val contentTitle = movie?.title ?: series?.title ?: ""
    val originalTitle = movie?.originalTitle ?: series?.originalTitle ?: ""
    val overview = movie?.overview ?: series?.overview ?: ""
    val posterUrl = movie?.posterUrl ?: series?.posterUrl ?: ""
    val bannerUrl = movie?.bannerUrl ?: series?.bannerUrl ?: ""
    val releaseYear = movie?.releaseYear ?: series?.releaseYear ?: 2024
    val durationMinutes = movie?.durationMinutes ?: 120
    val rating = movie?.rating ?: series?.rating ?: 0.0
    val director = movie?.director ?: series?.director ?: ""
    val actors = movie?.actors ?: series?.actors ?: ""
    val category = movie?.category ?: series?.category ?: ""
    val country = movie?.country ?: series?.country ?: ""
    val classification = movie?.classification ?: series?.classification ?: "+13"
    val contentId = movie?.id ?: series?.id ?: ""
    val savedTrailerUrl = movie?.trailerUrl ?: series?.trailerUrl ?: ""

    val isFavorite = favoritesViewModel.isFavorite(contentId)

    // Trailer player state (supports direct MP4 / HLS streams via ExoPlayer AND Web embeds like Dailymotion/YouTube/HTML via WebView)
    var isTrailerExpanded by remember { mutableStateOf(false) }
    val defaultFallbackTrailer = "https://geo.dailymotion.com/player/xbpgj.html?video=xafumda"
    val trailerUrlToPlay = savedTrailerUrl.ifBlank { defaultFallbackTrailer }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    // Helper to detect if a URL is a web embed player (like Dailymotion, YouTube, Vimeo, HTML page)
    fun isWebEmbedUrl(url: String): Boolean {
        val clean = url.trim().lowercase()
        if (clean.isBlank()) return false
        val directVideoExtensions = listOf(".mp4", ".m3u8", ".mkv", ".webm", ".mov", ".avi", ".mpd")
        val hasDirectExt = directVideoExtensions.any { clean.endsWith(it) || clean.contains("$it?") }
        if (hasDirectExt) return false

        return clean.contains("dailymotion.com") ||
                clean.contains("youtube.com") ||
                clean.contains("youtu.be") ||
                clean.contains("vimeo.com") ||
                clean.contains("player.") ||
                clean.contains(".html") ||
                clean.contains(".htm") ||
                clean.contains("embed")
    }

    val currentIsWebEmbed = remember(trailerUrlToPlay) {
        isWebEmbedUrl(trailerUrlToPlay)
    }

    // Initialize and cleanup ExoPlayer instance when in direct video mode
    DisposableEffect(isTrailerExpanded, currentIsWebEmbed, trailerUrlToPlay) {
        if (isTrailerExpanded && !currentIsWebEmbed && trailerUrlToPlay.isNotBlank()) {
            val player = ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(Uri.parse(trailerUrlToPlay)))
                prepare()
                playWhenReady = true
            }
            exoPlayer = player
            onDispose {
                player.release()
                exoPlayer = null
            }
        } else {
            onDispose {
                exoPlayer?.release()
                exoPlayer = null
            }
        }
    }

    // Ensure ExoPlayer is released when modal bottom sheet closes
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer?.release()
            exoPlayer = null
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            exoPlayer?.release()
            exoPlayer = null
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = Color(0xFF141414),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .verticalScroll(rememberScrollState())
        ) {
            // Banner Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                AsyncImage(
                    model = bannerUrl.ifBlank { posterUrl },
                    contentDescription = contentTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0x88000000),
                                    Color(0xFF141414)
                                )
                            )
                        )
                )

                IconButton(
                    onClick = {
                        exoPlayer?.release()
                        exoPlayer = null
                        onDismiss()
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                }
            }

            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = contentTitle,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (originalTitle.isNotBlank()) {
                    Text(
                        text = "Título Original: $originalTitle",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("$rating", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .background(Color.DarkGray, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(classification, color = Color.White, fontSize = 11.sp)
                    }

                    Text("$releaseYear", color = Color.LightGray)
                    if (movie != null) {
                        Text("$durationMinutes min", color = Color.LightGray)
                    } else if (series != null) {
                        val statusText = if (series.status.isNotBlank()) series.status else "En Emisión"
                        val statusColor = if (statusText == "Finalizado") Color(0xFF43A047) else Color(0xFFFFB300)
                        Box(
                            modifier = Modifier
                                .background(statusColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(statusText.uppercase(), color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        val epCount = series.totalEpisodes
                        if (epCount > 0) {
                            Text("$epCount Cap.", color = Color.LightGray, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Play, Trailer & Favorite Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onPlayClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reproducir", fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                    }

                    Button(
                        onClick = {
                            isTrailerExpanded = !isTrailerExpanded
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTrailerExpanded) Color(0xFFE50914) else Color(0xFF262626)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Default.OndemandVideo, contentDescription = "Tráiler", tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tráiler", fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            if (movie != null) favoritesViewModel.toggleMovieFavorite(movie)
                            else if (series != null) favoritesViewModel.toggleSeriesFavorite(series)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (isFavorite) Color(0xFFE50914) else Color.White
                        )
                    }
                }

                // Minimalist Trailer Section
                AnimatedVisibility(visible = isTrailerExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .background(Color(0xFF1E1E24), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.OndemandVideo,
                                    contentDescription = null,
                                    tint = Color(0xFFE50914),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Tráiler Oficial",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                            }
                            IconButton(
                                onClick = {
                                    isTrailerExpanded = false
                                    exoPlayer?.pause()
                                }
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar Tráiler", tint = Color.LightGray)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Video Container (WebView for Dailymotion/YouTube/Embeds or ExoPlayer for direct MP4/HLS)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentIsWebEmbed) {
                                AndroidView(
                                    factory = { ctx ->
                                        WebView(ctx).apply {
                                            layoutParams = ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                            settings.javaScriptEnabled = true
                                            settings.domStorageEnabled = true
                                            settings.mediaPlaybackRequiresUserGesture = false
                                            settings.loadWithOverviewMode = true
                                            settings.useWideViewPort = true
                                            webViewClient = WebViewClient()
                                            webChromeClient = WebChromeClient()
                                            loadUrl(trailerUrlToPlay)
                                        }
                                    },
                                    update = { webView ->
                                        if (webView.url != trailerUrlToPlay && trailerUrlToPlay.isNotBlank()) {
                                            webView.loadUrl(trailerUrlToPlay)
                                        }
                                    },
                                    onRelease = { webView ->
                                        webView.stopLoading()
                                        webView.destroy()
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                if (exoPlayer != null) {
                                    AndroidView(
                                        factory = { ctx ->
                                            PlayerView(ctx).apply {
                                                player = exoPlayer
                                                useController = true
                                                layoutParams = FrameLayout.LayoutParams(
                                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                                    ViewGroup.LayoutParams.MATCH_PARENT
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = Color(0xFFE50914), modifier = Modifier.size(36.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Cargando tráiler...", color = Color.Gray, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (series != null) {
                    val seasons = if (series.seasons.isNotEmpty()) series.seasons else listOf(Season(1, "Temporada 1"))
                    var selectedSeasonIdx by remember(series) { mutableIntStateOf(0) }
                    val activeSeason = seasons.getOrNull(selectedSeasonIdx.coerceIn(0, seasons.lastIndex)) ?: seasons.first()
                    
                    val activeEpisodes = activeSeason.episodes
                    val availableEpisodes = activeEpisodes.filter { ep -> ep.hasVideoLink() }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Temporadas y Capítulos", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            "${availableEpisodes.size} de ${activeEpisodes.size} listos",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    if (seasons.size > 1) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            seasons.forEachIndexed { sIdx, season ->
                                FilterChip(
                                    selected = selectedSeasonIdx == sIdx,
                                    onClick = { selectedSeasonIdx = sIdx },
                                    label = { Text(season.title) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFE50914),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (activeEpisodes.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(activeEpisodes) { ep ->
                                val hasLink = ep.hasVideoLink()
                                Card(
                                    onClick = {
                                        if (hasLink) {
                                            onPlayEpisode?.invoke(series, ep) ?: onPlayClick()
                                        }
                                    },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (hasLink) Color(0xFF222226) else Color(0xFF161618)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.width(140.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Capítulo ${ep.episodeNumber}",
                                                fontWeight = FontWeight.Bold,
                                                color = if (hasLink) Color.White else Color.Gray,
                                                fontSize = 13.sp
                                            )
                                            if (hasLink) {
                                                Icon(
                                                    Icons.Default.PlayArrow,
                                                    contentDescription = "Reproducir",
                                                    tint = Color(0xFFE50914),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        if (ep.title.isNotBlank() && ep.title != "Capítulo ${ep.episodeNumber}") {
                                            Text(
                                                ep.title,
                                                color = Color.LightGray,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            if (hasLink) "▶ Ver Capítulo" else "Próximamente",
                                            color = if (hasLink) Color(0xFF43A047) else Color.DarkGray,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1C1C1E), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "No hay capítulos configurados para esta temporada.",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Estado: ${series.status.ifBlank { "En Emisión" }}",
                                    color = Color.DarkGray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text("Sinopsis", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    text = overview.ifBlank { "Sin descripción disponible." },
                    fontSize = 14.sp,
                    color = Color.LightGray,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (director.isNotBlank()) {
                    Text("Director: $director", color = Color.White, fontSize = 13.sp)
                }
                if (actors.isNotBlank()) {
                    Text("Elenco: $actors", color = Color.White, fontSize = 13.sp)
                }
                if (category.isNotBlank()) {
                    Text("Categoría: $category", color = Color.White, fontSize = 13.sp)
                }
                if (country.isNotBlank()) {
                    Text("País: $country", color = Color.White, fontSize = 13.sp)
                }
            }
        }
    }
}

