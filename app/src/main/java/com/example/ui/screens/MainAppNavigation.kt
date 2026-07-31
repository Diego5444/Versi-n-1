package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.data.model.ContinueWatchingEntry
import com.example.data.model.Episode
import com.example.data.model.MovieItem
import com.example.data.model.Season
import com.example.data.model.SeriesItem
import com.example.ui.viewmodel.*

data class NavTabItem(
    val title: String,
    val icon: ImageVector,
    val tag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppNavigation(
    authViewModel: AuthViewModel,
    movieViewModel: MovieViewModel,
    favoritesViewModel: FavoritesViewModel,
    historyViewModel: HistoryViewModel,
    recommendationsViewModel: RecommendationsViewModel,
    adminViewModel: AdminViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val authUiState by authViewModel.uiState.collectAsState()

    // Request notification permission for Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* Granted or denied handled gracefully */ }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedMovie by remember { mutableStateOf<MovieItem?>(null) }
    var selectedSeries by remember { mutableStateOf<SeriesItem?>(null) }

    var isPlayingVideo by remember { mutableStateOf(false) }
    var playingMovie by remember { mutableStateOf<MovieItem?>(null) }
    var playingSeries by remember { mutableStateOf<SeriesItem?>(null) }
    var playingEpisode by remember { mutableStateOf<Episode?>(null) }
    var playingPositionMs by remember { mutableLongStateOf(0L) }

    val movies by movieViewModel.allMovies.collectAsState()
    val seriesList by movieViewModel.allSeries.collectAsState()

    var showAdminPanel by remember { mutableStateOf(false) }
    var showLegalHub by remember { mutableStateOf(false) }
    var selectedLegalDoc by remember { mutableStateOf<LegalDocType?>(null) }

    val navItems = remember {
        listOf(
            NavTabItem("Inicio", Icons.Default.Movie, "tab_home"),
            NavTabItem("Para Ti", Icons.Default.AutoAwesome, "tab_recommendations"),
            NavTabItem("Favoritos", Icons.Default.Favorite, "tab_favorites"),
            NavTabItem("Historial", Icons.Default.History, "tab_history"),
            NavTabItem("Perfil", Icons.Default.Person, "tab_profile")
        )
    }

    fun startPlaybackForEntry(contentId: String, contentType: String, title: String, posterUrl: String, episodeNumber: Int, episodeTitle: String, positionMs: Long) {
        val foundMovie = movies.firstOrNull { it.id == contentId }
        val foundSeries = seriesList.firstOrNull { it.id == contentId }

        if (foundMovie != null) {
            playingMovie = foundMovie
            playingSeries = null
            playingEpisode = null
            playingPositionMs = positionMs
            isPlayingVideo = true
        } else if (foundSeries != null) {
            playingMovie = null
            playingSeries = foundSeries
            val allEp = foundSeries.seasons.flatMap { it.episodes }
            playingEpisode = allEp.firstOrNull { it.episodeNumber == episodeNumber }
                ?: allEp.firstOrNull { it.hasVideoLink() }
            playingPositionMs = positionMs
            isPlayingVideo = true
        } else {
            if (contentType == "series" || contentType == "anime" || episodeNumber > 0) {
                val fallbackEp = Episode(
                    id = "ep_$episodeNumber",
                    episodeNumber = episodeNumber,
                    title = episodeTitle.ifBlank { "Capítulo $episodeNumber" },
                    servers = emptyList()
                )
                playingSeries = SeriesItem(
                    id = contentId,
                    title = title,
                    posterUrl = posterUrl,
                    contentType = contentType,
                    seasons = listOf(Season(1, "Temporada 1", listOf(fallbackEp)))
                )
                playingEpisode = fallbackEp
                playingMovie = null
            } else {
                playingMovie = MovieItem(id = contentId, title = title, posterUrl = posterUrl)
                playingSeries = null
                playingEpisode = null
            }
            playingPositionMs = positionMs
            isPlayingVideo = true
        }
    }

    if (authUiState !is AuthUiState.Authenticated) {
        AuthScreen(authViewModel = authViewModel)
    } else if (isPlayingVideo) {
        VideoPlayerScreen(
            movie = playingMovie,
            series = playingSeries,
            initialEpisode = playingEpisode,
            initialPositionMs = playingPositionMs,
            historyViewModel = historyViewModel,
            onBack = {
                isPlayingVideo = false
                playingMovie = null
                playingSeries = null
                playingEpisode = null
                playingPositionMs = 0L
            }
        )
    } else if (showAdminPanel) {
        BackHandler { showAdminPanel = false }
        AdminPanelScreen(
            adminViewModel = adminViewModel,
            onBack = { showAdminPanel = false }
        )
    } else if (selectedLegalDoc != null) {
        BackHandler { selectedLegalDoc = null }
        LegalDocumentDetailScreen(
            docType = selectedLegalDoc!!,
            authViewModel = authViewModel,
            onBack = { selectedLegalDoc = null }
        )
    } else if (showLegalHub) {
        BackHandler { showLegalHub = false }
        LegalHubScreen(
            authViewModel = authViewModel,
            onSelectDoc = { selectedLegalDoc = it },
            onBack = { showLegalHub = false }
        )
    } else {
        // Intercept System Back Button to handle detail sheets & tab history
        val isSheetOpen = selectedMovie != null || selectedSeries != null
        BackHandler(enabled = isSheetOpen || selectedTabIndex != 0) {
            if (isSheetOpen) {
                selectedMovie = null
                selectedSeries = null
            } else if (selectedTabIndex != 0) {
                selectedTabIndex = 0
            }
        }

        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF141414),
                    contentColor = Color.White
                ) {
                    navItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFFE50914),
                                selectedTextColor = Color(0xFFE50914),
                                indicatorColor = Color(0xFFE50914).copy(alpha = 0.15f),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            ),
                            modifier = Modifier.testTag(item.tag)
                        )
                    }
                }
            },
            containerColor = Color(0xFF0F0F13)
        ) { innerPadding ->
            val modifier = Modifier.padding(innerPadding)

            when (selectedTabIndex) {
                0 -> HomeScreen(
                    movieViewModel = movieViewModel,
                    favoritesViewModel = favoritesViewModel,
                    historyViewModel = historyViewModel,
                    onMovieClick = { movie -> selectedMovie = movie },
                    onSeriesClick = { series -> selectedSeries = series },
                    onContinueClick = { cw ->
                        startPlaybackForEntry(
                            contentId = cw.contentId,
                            contentType = cw.contentType,
                            title = cw.title,
                            posterUrl = cw.posterUrl,
                            episodeNumber = cw.episodeNumber,
                            episodeTitle = cw.episodeTitle,
                            positionMs = cw.positionMs
                        )
                    },
                    onOpenSearch = { /* Handled in top bar */ },
                )
                1 -> RecommendationsScreen(
                    recommendationsViewModel = recommendationsViewModel,
                    favoritesViewModel = favoritesViewModel,
                    onMovieClick = { movie -> selectedMovie = movie },
                    modifier = modifier
                )
                2 -> FavoritesScreen(
                    favoritesViewModel = favoritesViewModel,
                    onItemClick = { fav ->
                        startPlaybackForEntry(
                            contentId = fav.contentId,
                            contentType = fav.contentType,
                            title = fav.title,
                            posterUrl = fav.posterUrl,
                            episodeNumber = 0,
                            episodeTitle = "",
                            positionMs = 0L
                        )
                    },
                    modifier = modifier
                )
                3 -> HistoryScreen(
                    historyViewModel = historyViewModel,
                    onItemClick = { hist ->
                        startPlaybackForEntry(
                            contentId = hist.contentId,
                            contentType = hist.contentType,
                            title = hist.title,
                            posterUrl = hist.posterUrl,
                            episodeNumber = 0,
                            episodeTitle = "",
                            positionMs = hist.positionMs
                        )
                    },
                    modifier = modifier
                )
                4 -> ProfileScreen(
                    authViewModel = authViewModel,
                    favoritesViewModel = favoritesViewModel,
                    historyViewModel = historyViewModel,
                    onOpenAdminPanel = { showAdminPanel = true },
                    onOpenLegal = { showLegalHub = true },
                    modifier = modifier
                )
            }

            // Movie or Series Detail Sheet Modal
            if (selectedMovie != null || selectedSeries != null) {
                MovieDetailSheet(
                    movie = selectedMovie,
                    series = selectedSeries,
                    favoritesViewModel = favoritesViewModel,
                    historyViewModel = historyViewModel,
                    onPlayClick = {
                        playingMovie = selectedMovie
                        playingSeries = selectedSeries
                        playingEpisode = selectedSeries?.seasons?.flatMap { it.episodes }?.firstOrNull { it.hasVideoLink() }
                        selectedMovie = null
                        selectedSeries = null
                        isPlayingVideo = true
                    },
                    onPlayEpisode = { s, ep ->
                        playingSeries = s
                        playingEpisode = ep
                        selectedMovie = null
                        selectedSeries = null
                        isPlayingVideo = true
                    },
                    onDismiss = {
                        selectedMovie = null
                        selectedSeries = null
                    }
                )
            }
        }
    }
}
