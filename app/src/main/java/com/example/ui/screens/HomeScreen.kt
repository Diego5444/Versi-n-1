package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ContinueWatchingEntry
import com.example.data.model.MovieItem
import com.example.data.model.SeriesItem
import com.example.ui.viewmodel.FavoritesViewModel
import com.example.ui.viewmodel.HistoryViewModel
import com.example.ui.viewmodel.MovieViewModel

@Composable
fun HomeScreen(
    movieViewModel: MovieViewModel,
    favoritesViewModel: FavoritesViewModel,
    historyViewModel: HistoryViewModel,
    onMovieClick: (MovieItem) -> Unit,
    onSeriesClick: (SeriesItem) -> Unit,
    onContinueClick: (ContinueWatchingEntry) -> Unit,
    onOpenSearch: () -> Unit = {}
) {
    val movies by movieViewModel.allMovies.collectAsState()
    val seriesList by movieViewModel.allSeries.collectAsState()
    val categories by movieViewModel.allCategories.collectAsState()
    val continueWatchingList by historyViewModel.continueWatchingList.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val featuredMovies = movies.filter { it.isFeatured }
    val featuredMovie = featuredMovies.firstOrNull() ?: movies.firstOrNull()

    val popularMovies = movies.filter { it.isPopular }
    val seriesOnly = seriesList.filter { it.contentType == "series" }
    val animeOnly = seriesList.filter { it.contentType == "anime" }

    val filteredMovies = remember(searchQuery, movies) {
        if (searchQuery.isNotBlank()) {
            movies.filter { movie ->
                movie.title.contains(searchQuery, ignoreCase = true) ||
                movie.category.contains(searchQuery, ignoreCase = true) ||
                movie.categories.any { it.contains(searchQuery, ignoreCase = true) } ||
                movie.overview.contains(searchQuery, ignoreCase = true)
            }
        } else emptyList()
    }

    val filteredSeries = remember(searchQuery, seriesList) {
        if (searchQuery.isNotBlank()) {
            seriesList.filter { series ->
                series.title.contains(searchQuery, ignoreCase = true) ||
                series.category.contains(searchQuery, ignoreCase = true) ||
                series.categories.any { it.contains(searchQuery, ignoreCase = true) } ||
                series.contentType.contains(searchQuery, ignoreCase = true) ||
                series.overview.contains(searchQuery, ignoreCase = true)
            }
        } else emptyList()
    }

    val isEmptyCatalog = movies.isEmpty() && seriesList.isEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F13))
    ) {
        if (isEmptyCatalog) {
            // Strictly required empty state: "No hay contenido disponible"
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "No hay contenido disponible",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Aún no se ha añadido contenido desde el Panel de Administración.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Top Search Bar
                item {
                    if (isSearchActive) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Buscar película, serie, anime...", color = Color.Gray, fontSize = 14.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                                },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        if (searchQuery.isNotEmpty()) {
                                            searchQuery = ""
                                        } else {
                                            isSearchActive = false
                                        }
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
                                    }
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF1E1E24),
                                    unfocusedContainerColor = Color(0xFF1E1E24),
                                    focusedBorderColor = Color(0xFFE50914),
                                    unfocusedBorderColor = Color.DarkGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFFE50914), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("C", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "CineSync",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(onClick = {
                                isSearchActive = true
                                onOpenSearch()
                            }) {
                                Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color.White)
                            }
                        }
                    }
                }

                if (isSearchActive) {
                    // Search Results Mode
                    if (searchQuery.isBlank()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Escribe para buscar contenidos por título, género o sinopsis.",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                // Quick suggestion chips
                                val quickQueries = listOf("Acción", "Anime", "Comedia", "Drama", "Películas")
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(quickQueries) { q ->
                                        SuggestionChip(
                                            onClick = { searchQuery = q },
                                            label = { Text(q, color = Color.White, fontSize = 12.sp) },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = Color(0xFF1E1E24)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        val hasResults = filteredMovies.isNotEmpty() || filteredSeries.isNotEmpty()
                        if (!hasResults) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.SearchOff, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(56.dp))
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("No se encontraron resultados para", color = Color.Gray, fontSize = 14.sp)
                                        Text("\"$searchQuery\"", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            if (filteredMovies.isNotEmpty()) {
                                item {
                                    SectionHeader("Películas encontradas (${filteredMovies.size})")
                                    ContentHorizontalList(
                                        movies = filteredMovies,
                                        favoritesViewModel = favoritesViewModel,
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            if (filteredSeries.isNotEmpty()) {
                                item {
                                    SectionHeader("Series y Anime encontrados (${filteredSeries.size})")
                                    SeriesHorizontalList(
                                        seriesList = filteredSeries,
                                        favoritesViewModel = favoritesViewModel,
                                        onSeriesClick = onSeriesClick
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Standard Home Catalog Mode
                    // Hero Banner (Featured)
                    if (featuredMovie != null) {
                        item {
                            HeroBannerCard(
                                movie = featuredMovie,
                                onClick = { onMovieClick(featuredMovie) }
                            )
                        }
                    }

                    // Continue Watching Row (if user has items)
                    if (continueWatchingList.isNotEmpty()) {
                        item {
                            SectionHeader("Continuar Viendo")
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(continueWatchingList) { cw ->
                                    ContinueWatchingCard(
                                        entry = cw,
                                        onClick = { onContinueClick(cw) }
                                    )
                                }
                            }
                        }
                    }

                    // Popular Movies
                    if (popularMovies.isNotEmpty()) {
                        item {
                            SectionHeader("Populares en CineSync")
                            ContentHorizontalList(
                                movies = popularMovies,
                                favoritesViewModel = favoritesViewModel,
                                onMovieClick = onMovieClick
                            )
                        }
                    }

                    // All Movies
                    if (movies.isNotEmpty()) {
                        item {
                            SectionHeader("Películas")
                            ContentHorizontalList(
                                movies = movies,
                                favoritesViewModel = favoritesViewModel,
                                onMovieClick = onMovieClick
                            )
                        }
                    }

                    // Series
                    if (seriesOnly.isNotEmpty()) {
                        item {
                            SectionHeader("Series Destacadas")
                            SeriesHorizontalList(
                                seriesList = seriesOnly,
                                favoritesViewModel = favoritesViewModel,
                                onSeriesClick = onSeriesClick
                            )
                        }
                    }

                    // Anime
                    if (animeOnly.isNotEmpty()) {
                        item {
                            SectionHeader("Anime")
                            SeriesHorizontalList(
                                seriesList = animeOnly,
                                favoritesViewModel = favoritesViewModel,
                                onSeriesClick = onSeriesClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 10.dp)
    )
}

@Composable
fun HeroBannerCard(movie: MovieItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = movie.bannerUrl.ifBlank { movie.posterUrl },
            contentDescription = movie.title,
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
                            Color(0x99000000),
                            Color(0xFA0F0F13)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE50914), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("DESTACADO", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("${movie.rating}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = movie.title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = movie.overview,
                color = Color.LightGray,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ContentHorizontalList(
    movies: List<MovieItem>,
    favoritesViewModel: FavoritesViewModel,
    onMovieClick: (MovieItem) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(movies, key = { it.id }) { movie ->
            MoviePosterCard(
                movie = movie,
                isFavorite = favoritesViewModel.isFavorite(movie.id),
                onFavoriteToggle = { favoritesViewModel.toggleMovieFavorite(movie) },
                onClick = { onMovieClick(movie) }
            )
        }
    }
}

@Composable
fun SeriesHorizontalList(
    seriesList: List<SeriesItem>,
    favoritesViewModel: FavoritesViewModel,
    onSeriesClick: (SeriesItem) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(seriesList, key = { it.id }) { series ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                modifier = Modifier
                    .width(130.dp)
                    .clickable { onSeriesClick(series) }
            ) {
                Column {
                    Box(modifier = Modifier.height(180.dp)) {
                        AsyncImage(
                            model = series.posterUrl,
                            contentDescription = series.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Text(
                        text = series.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MoviePosterCard(
    movie: MovieItem,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
        modifier = Modifier
            .width(130.dp)
            .clickable(onClick = onClick)
    ) {
        Column {
            Box(modifier = Modifier.height(180.dp)) {
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = movie.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000))
                        .clickable(onClick = onFavoriteToggle)
                        .padding(6.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorito",
                        tint = if (isFavorite) Color(0xFFE50914) else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = movie.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${movie.releaseYear}",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun ContinueWatchingCard(
    entry: ContinueWatchingEntry,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
        modifier = Modifier
            .width(200.dp)
            .clickable(onClick = onClick)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                AsyncImage(
                    model = entry.posterUrl,
                    contentDescription = entry.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "Continuar",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = entry.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                val progress = if (entry.durationMs > 0) (entry.positionMs.toFloat() / entry.durationMs.toFloat()) else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    color = Color(0xFFE50914),
                    trackColor = Color.DarkGray
                )
            }
        }
    }
}
