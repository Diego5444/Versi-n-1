package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.viewmodel.AdminViewModel

enum class AdminTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    MOVIES("Películas", Icons.Default.Movie),
    SERIES("Series / Anime", Icons.Default.Tv),
    CATEGORIES("Categorías", Icons.Default.Category),
    USERS("Usuarios", Icons.Default.Group)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    adminViewModel: AdminViewModel,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(AdminTab.DASHBOARD) }
    val adminStateMessage by adminViewModel.adminState.collectAsState()

    val movies by adminViewModel.moviesList.collectAsState()
    val seriesList by adminViewModel.seriesList.collectAsState()
    val categories by adminViewModel.categoriesList.collectAsState()
    val users by adminViewModel.usersList.collectAsState()

    var showMovieDialog by remember { mutableStateOf(false) }
    var editingMovie by remember { mutableStateOf<MovieItem?>(null) }

    var showSeriesDialog by remember { mutableStateOf(false) }
    var editingSeries by remember { mutableStateOf<SeriesItem?>(null) }

    var showCategoryDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(adminStateMessage) {
        adminStateMessage?.let {
            snackbarHostState.showSnackbar(it)
            adminViewModel.clearAdminState()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Panel Administrativo", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF141414),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0D0D0D)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Admin Navigation Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = Color(0xFF181818),
                contentColor = Color(0xFFE50914),
                edgePadding = 16.dp
            ) {
                AdminTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.title, fontSize = 14.sp) },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        selectedContentColor = Color(0xFFE50914),
                        unselectedContentColor = Color.Gray
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    AdminTab.DASHBOARD -> AdminDashboardView(
                        moviesCount = movies.size,
                        seriesCount = seriesList.size,
                        categoriesCount = categories.size,
                        usersCount = users.size,
                        totalViews = movies.sumOf { it.viewsCount.toLong() } + seriesList.sumOf { it.viewsCount.toLong() }
                    )

                    AdminTab.MOVIES -> AdminMoviesView(
                        movies = movies,
                        onAddMovie = {
                            editingMovie = MovieItem()
                            showMovieDialog = true
                        },
                        onEditMovie = {
                            editingMovie = it
                            showMovieDialog = true
                        },
                        onDeleteMovie = { adminViewModel.deleteMovie(it.id) },
                        onDuplicateMovie = { adminViewModel.duplicateMovie(it) },
                        onTogglePublished = { adminViewModel.toggleMoviePublished(it) }
                    )

                    AdminTab.SERIES -> AdminSeriesView(
                        seriesList = seriesList,
                        onAddSeries = {
                            editingSeries = SeriesItem()
                            showSeriesDialog = true
                        },
                        onEditSeries = {
                            editingSeries = it
                            showSeriesDialog = true
                        },
                        onDeleteSeries = { adminViewModel.deleteSeries(it.id) }
                    )

                    AdminTab.CATEGORIES -> AdminCategoriesView(
                        categories = categories,
                        onAddCategory = { showCategoryDialog = true },
                        onDeleteCategory = { adminViewModel.deleteCategory(it.id) }
                    )

                    AdminTab.USERS -> AdminUsersView(
                        users = users,
                        onToggleRole = { user ->
                            val newRole = if (user.role == "admin") "user" else "admin"
                            adminViewModel.updateUserRole(user.uid, newRole)
                        }
                    )
                }
            }
        }
    }

    // Add / Edit Movie Dialog
    if (showMovieDialog && editingMovie != null) {
        MovieFormDialog(
            movie = editingMovie!!,
            categoriesList = categories.map { it.name },
            onDismiss = { showMovieDialog = false },
            onSave = { saved ->
                adminViewModel.saveMovie(saved)
                showMovieDialog = false
            }
        )
    }

    // Add / Edit Series Dialog
    if (showSeriesDialog && editingSeries != null) {
        SeriesFormDialog(
            series = editingSeries!!,
            categoriesList = categories.map { it.name },
            onDismiss = { showSeriesDialog = false },
            onSave = { saved ->
                adminViewModel.saveSeries(saved)
                showSeriesDialog = false
            }
        )
    }

    // Add Category Dialog
    if (showCategoryDialog) {
        CategoryFormDialog(
            onDismiss = { showCategoryDialog = false },
            onSave = { name, desc ->
                adminViewModel.saveCategory(CategoryItem(name = name, description = desc))
                showCategoryDialog = false
            }
        )
    }
}

@Composable
fun AdminDashboardView(
    moviesCount: Int,
    seriesCount: Int,
    categoriesCount: Int,
    usersCount: Int,
    totalViews: Long
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Resumen del Sistema",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard("Películas", moviesCount.toString(), Icons.Default.Movie, Color(0xFFE50914), Modifier.weight(1f))
            StatCard("Series/Anime", seriesCount.toString(), Icons.Default.Tv, Color(0xFF1E88E5), Modifier.weight(1f))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard("Categorías", categoriesCount.toString(), Icons.Default.Category, Color(0xFF43A047), Modifier.weight(1f))
            StatCard("Usuarios", usersCount.toString(), Icons.Default.Group, Color(0xFFFB8C00), Modifier.weight(1f))
        }

        StatCard("Visualizaciones Totales", totalViews.toString(), Icons.Default.Visibility, Color(0xFF8E24AA), Modifier.fillMaxWidth())

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Estado de la Base de Datos", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Conectado a Firebase Realtime Database (abby-cdb30)",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
                Text(
                    "Catálogo dinámico sincronizado en tiempo real.",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(title, color = Color.Gray, fontSize = 12.sp)
                Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun AdminMoviesView(
    movies: List<MovieItem>,
    onAddMovie: () -> Unit,
    onEditMovie: (MovieItem) -> Unit,
    onDeleteMovie: (MovieItem) -> Unit,
    onDuplicateMovie: (MovieItem) -> Unit,
    onTogglePublished: (MovieItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Gestión de Películas (${movies.size})",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onAddMovie,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Agregar Película", maxLines = 1, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (movies.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay películas registradas. Presiona 'Agregar Película'.", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(movies) { movie ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF181818))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = movie.posterUrl.ifBlank { "https://via.placeholder.com/150x220?text=Poster" },
                                contentDescription = movie.title,
                                modifier = Modifier
                                    .size(width = 60.dp, height = 90.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(movie.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("${movie.releaseYear} • ${movie.durationMinutes} min • ${movie.category}", color = Color.Gray, fontSize = 12.sp)
                                Text("Servidores: ${movie.servers.size} • Vistas: ${movie.viewsCount}", color = Color.LightGray, fontSize = 12.sp)
                            }

                            Row {
                                IconButton(onClick = { onTogglePublished(movie) }) {
                                    Icon(
                                        imageVector = if (movie.isPublished) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Publicar/Ocultar",
                                        tint = if (movie.isPublished) Color.Green else Color.Gray
                                    )
                                }
                                IconButton(onClick = { onDuplicateMovie(movie) }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicar", tint = Color.LightGray)
                                }
                                IconButton(onClick = { onEditMovie(movie) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.LightGray)
                                }
                                IconButton(onClick = { onDeleteMovie(movie) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFE50914))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminSeriesView(
    seriesList: List<SeriesItem>,
    onAddSeries: () -> Unit,
    onEditSeries: (SeriesItem) -> Unit,
    onDeleteSeries: (SeriesItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Gestión de Series (${seriesList.size})",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onAddSeries,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Agregar Serie", maxLines = 1, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (seriesList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay series ni anime. Presiona 'Agregar Serie'.", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(seriesList) { series ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF181818))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = series.posterUrl.ifBlank { "https://via.placeholder.com/150x220?text=Poster" },
                                contentDescription = series.title,
                                modifier = Modifier
                                    .size(width = 60.dp, height = 90.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(series.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                val availableEpCount = series.seasons.flatMap { it.episodes }.count { it.hasVideoLink() }
                                val statusColor = if (series.status == "Finalizado") Color(0xFF43A047) else Color(0xFFFFB300)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(series.contentType.uppercase(), color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("•", color = Color.Gray, fontSize = 11.sp)
                                    Box(
                                        modifier = Modifier
                                            .background(statusColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(series.status.ifBlank { "En Emisión" }, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Capítulos con enlace: $availableEpCount / ${series.totalEpisodes.coerceAtLeast(availableEpCount)}", color = Color.LightGray, fontSize = 12.sp)
                            }

                            Row {
                                IconButton(onClick = { onEditSeries(series) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.LightGray)
                                }
                                IconButton(onClick = { onDeleteSeries(series) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFE50914))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminCategoriesView(
    categories: List<CategoryItem>,
    onAddCategory: () -> Unit,
    onDeleteCategory: (CategoryItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Categorías (${categories.size})",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onAddCategory,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nueva Categoría", maxLines = 1, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (categories.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay categorías configuradas.", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF181818))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(cat.name, color = Color.White, fontWeight = FontWeight.Bold)
                                if (cat.description.isNotBlank()) {
                                    Text(cat.description, color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                            IconButton(onClick = { onDeleteCategory(cat) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFE50914))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminUsersView(
    users: List<UserProfile>,
    onToggleRole: (UserProfile) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Gestión de Usuarios (${users.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))

        if (users.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay usuarios registrados aún.", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(users) { user ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF181818))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.displayName.ifBlank { "Sin Nombre" }, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(user.email, color = Color.Gray, fontSize = 12.sp)
                                Text("UID: ${user.uid.take(10)}...", color = Color.DarkGray, fontSize = 10.sp)
                            }

                            Button(
                                onClick = { onToggleRole(user) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (user.role == "admin") Color(0xFFE50914) else Color(0xFF333333)
                                )
                            ) {
                                Text(if (user.role == "admin") "ADMIN" else "USUARIO", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieFormDialog(
    movie: MovieItem,
    categoriesList: List<String>,
    onDismiss: () -> Unit,
    onSave: (MovieItem) -> Unit
) {
    var title by remember { mutableStateOf(movie.title) }
    var origTitle by remember { mutableStateOf(movie.originalTitle) }
    var overview by remember { mutableStateOf(movie.overview) }
    var posterUrl by remember { mutableStateOf(movie.posterUrl) }
    var bannerUrl by remember { mutableStateOf(movie.bannerUrl) }
    var thumbnailUrl by remember { mutableStateOf(movie.thumbnailUrl) }
    var yearText by remember { mutableStateOf(movie.releaseYear.toString()) }
    var durationText by remember { mutableStateOf(movie.durationMinutes.toString()) }
    var director by remember { mutableStateOf(movie.director) }
    var actors by remember { mutableStateOf(movie.actors) }
    var category by remember { mutableStateOf(movie.category) }
    var country by remember { mutableStateOf(movie.country) }
    var classification by remember { mutableStateOf(movie.classification) }
    var ratingText by remember { mutableStateOf(movie.rating.toString()) }
    var trailerUrl by remember { mutableStateOf(movie.trailerUrl) }
    var isPopular by remember { mutableStateOf(movie.isPopular) }
    var isFeatured by remember { mutableStateOf(movie.isFeatured) }
    var isNew by remember { mutableStateOf(movie.isNew) }

    // Media Servers state
    var servers by remember { mutableStateOf(movie.servers.ifEmpty { listOf(MediaServer(serverName = "Servidor Principal", language = "Español Latino")) }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (movie.id.isBlank()) "Agregar Película" else "Editar Película", color = Color.White) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = origTitle, onValueChange = { origTitle = it }, label = { Text("Título Original") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = overview, onValueChange = { overview = it }, label = { Text("Sinopsis") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

                // Image URLs & Preview
                Text("URLs de Imágenes con Vista Previa", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                OutlinedTextField(value = posterUrl, onValueChange = { posterUrl = it }, label = { Text("Poster URL") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bannerUrl, onValueChange = { bannerUrl = it }, label = { Text("Banner URL") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = thumbnailUrl, onValueChange = { thumbnailUrl = it }, label = { Text("Miniatura URL") }, modifier = Modifier.fillMaxWidth())

                if (posterUrl.isNotBlank()) {
                    AsyncImage(
                        model = posterUrl,
                        contentDescription = "Preview Poster",
                        modifier = Modifier
                            .height(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = yearText, onValueChange = { yearText = it }, label = { Text("Año") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = durationText, onValueChange = { durationText = it }, label = { Text("Duración (min)") }, modifier = Modifier.weight(1f))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = director, onValueChange = { director = it }, label = { Text("Director") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = country, onValueChange = { country = it }, label = { Text("País") }, modifier = Modifier.weight(1f))
                }

                OutlinedTextField(value = actors, onValueChange = { actors = it }, label = { Text("Actores (separados por coma)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Categoría Principal") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = classification, onValueChange = { classification = it }, label = { Text("Clasificación (ej. +13)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = ratingText, onValueChange = { ratingText = it }, label = { Text("Calificación (0-10)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = trailerUrl, onValueChange = { trailerUrl = it }, label = { Text("Trailer YouTube URL") }, modifier = Modifier.fillMaxWidth())

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isFeatured, onCheckedChange = { isFeatured = it })
                    Text("Destacado en Inicio", color = Color.White)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPopular, onCheckedChange = { isPopular = it })
                    Text("Popular", color = Color.White)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isNew, onCheckedChange = { isNew = it })
                    Text("Nuevo Lanzamiento", color = Color.White)
                }

                HorizontalDivider(color = Color.DarkGray)

                Text("Servidores de Reproducción por Idioma", fontWeight = FontWeight.Bold, color = Color.White)
                servers.forEachIndexed { idx, srv ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF242424))
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(value = srv.language, onValueChange = { newLang ->
                                servers = servers.toMutableList().also { it[idx] = srv.copy(language = newLang) }
                            }, label = { Text("Idioma (ej. Español Latino)") })

                            OutlinedTextField(value = srv.streamUrl, onValueChange = { newUrl ->
                                servers = servers.toMutableList().also { it[idx] = srv.copy(streamUrl = newUrl) }
                            }, label = { Text("Stream URL (HLS/MP4)") })

                            OutlinedTextField(value = srv.serverName, onValueChange = { newName ->
                                servers = servers.toMutableList().also { it[idx] = srv.copy(serverName = newName) }
                            }, label = { Text("Nombre Servidor (ej. Servidor 1)") })
                        }
                    }
                }

                Button(
                    onClick = { servers = servers + MediaServer(serverName = "Servidor ${servers.size + 1}", language = "Inglés") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
                ) {
                    Text("+ Agregar Servidor / Idioma")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val year = yearText.toIntOrNull() ?: 2024
                    val dur = durationText.toIntOrNull() ?: 120
                    val rat = ratingText.toDoubleOrNull() ?: 0.0
                    onSave(
                        movie.copy(
                            title = title,
                            originalTitle = origTitle,
                            overview = overview,
                            posterUrl = posterUrl,
                            bannerUrl = bannerUrl,
                            thumbnailUrl = thumbnailUrl,
                            releaseYear = year,
                            durationMinutes = dur,
                            director = director,
                            actors = actors,
                            category = category,
                            country = country,
                            classification = classification,
                            rating = rat,
                            trailerUrl = trailerUrl,
                            isPopular = isPopular,
                            isFeatured = isFeatured,
                            isNew = isNew,
                            servers = servers
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
            ) {
                Text("Guardar Película")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.White) }
        },
        containerColor = Color(0xFF181818)
    )
}

@Composable
fun SeriesFormDialog(
    series: SeriesItem,
    categoriesList: List<String>,
    onDismiss: () -> Unit,
    onSave: (SeriesItem) -> Unit
) {
    var title by remember { mutableStateOf(series.title) }
    var overview by remember { mutableStateOf(series.overview) }
    var posterUrl by remember { mutableStateOf(series.posterUrl) }
    var bannerUrl by remember { mutableStateOf(series.bannerUrl) }
    var yearText by remember { mutableStateOf(series.releaseYear.toString()) }
    var category by remember { mutableStateOf(series.category) }
    var trailerUrl by remember { mutableStateOf(series.trailerUrl) }
    var contentType by remember { mutableStateOf(series.contentType) } // "series" or "anime"
    var status by remember { mutableStateOf(if (series.status.isNotBlank()) series.status else "En Emisión") }
    
    // Initialize seasons
    val initialSeasons = remember(series) {
        if (series.seasons.isNotEmpty()) series.seasons
        else listOf(Season(seasonNumber = 1, title = "Temporada 1", episodes = emptyList()))
    }
    var seasonsList by remember { mutableStateOf(initialSeasons) }
    var selectedSeasonIndex by remember { mutableIntStateOf(0) }
    val desiredCounts = remember { mutableStateMapOf<Int, String>() }

    val currentSeason = seasonsList.getOrNull(selectedSeasonIndex.coerceIn(0, (seasonsList.size - 1).coerceAtLeast(0)))
        ?: Season(1, "Temporada 1")

    val currentCountText = desiredCounts[selectedSeasonIndex]
        ?: if (currentSeason.episodes.isNotEmpty()) currentSeason.episodes.size.toString()
        else "16"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurar Serie / Anime y Capítulos", color = Color.White) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título Serie/Anime") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = overview, onValueChange = { overview = it }, label = { Text("Sinopsis") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = posterUrl, onValueChange = { posterUrl = it }, label = { Text("Poster URL") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bannerUrl, onValueChange = { bannerUrl = it }, label = { Text("Banner URL") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = yearText, onValueChange = { yearText = it }, label = { Text("Año") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Categoría") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = trailerUrl, onValueChange = { trailerUrl = it }, label = { Text("Enlace de Tráiler (Video URL)") }, modifier = Modifier.fillMaxWidth())

                // Type selector
                Text("Tipo de Contenido:", color = Color.Gray, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { contentType = "series" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (contentType == "series") Color(0xFF1E88E5) else Color(0xFF333333)
                        )
                    ) { Text("SERIE") }

                    Button(
                        onClick = { contentType = "anime" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (contentType == "anime") Color(0xFF8E24AA) else Color(0xFF333333)
                        )
                    ) { Text("ANIME") }
                }

                HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 4.dp))

                // Status selector
                Text("Estado de Emisión:", color = Color.Gray, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = status == "En Emisión",
                        onClick = { status = "En Emisión" },
                        label = { Text("EN EMISIÓN") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFB300),
                            selectedLabelColor = Color.Black
                        )
                    )
                    FilterChip(
                        selected = status == "Finalizado",
                        onClick = { status = "Finalizado" },
                        label = { Text("FINALIZADO") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF43A047),
                            selectedLabelColor = Color.White
                        )
                    )
                }

                HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 4.dp))

                // SEASONS AND CHAPTERS EDITOR SECTION
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF222226), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "GESTOR DE TEMPORADAS Y CAPÍTULOS",
                            color = Color(0xFFE50914),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                val nextNum = seasonsList.size + 1
                                val newSeason = Season(
                                    seasonNumber = nextNum,
                                    title = "Temporada $nextNum",
                                    episodes = emptyList()
                                )
                                seasonsList = seasonsList + newSeason
                                selectedSeasonIndex = seasonsList.lastIndex
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Añadir Temporada", tint = Color.White)
                        }
                    }

                    // Season chips selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        seasonsList.forEachIndexed { idx, season ->
                            FilterChip(
                                selected = selectedSeasonIndex == idx,
                                onClick = { selectedSeasonIndex = idx },
                                label = { Text(season.title) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFE50914),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // Total episodes for selected season
                    OutlinedTextField(
                        value = currentCountText,
                        onValueChange = { input ->
                            desiredCounts[selectedSeasonIndex] = input
                        },
                        label = { Text("Cantidad de Capítulos (${currentSeason.title})") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    val activeEpList = currentSeason.episodes
                    val targetEpCount = currentCountText.toIntOrNull() ?: activeEpList.size
                    val displayCount = if (targetEpCount > 0) targetEpCount else 1
                    val filledCount = (1..displayCount).count { num ->
                        activeEpList.firstOrNull { it.episodeNumber == num }?.hasVideoLink() == true
                    }

                    Text(
                        "Capítulos de ${currentSeason.title} ($filledCount de $displayCount con enlace de video):",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    for (i in 1..displayCount) {
                        val existingEp = activeEpList.firstOrNull { it.episodeNumber == i }
                        val currentUrl = existingEp?.servers?.firstOrNull()?.streamUrl.orEmpty()

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF161618), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Capítulo $i",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (currentUrl.isNotBlank()) Color(0xFF43A047).copy(alpha = 0.2f) else Color.DarkGray,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        if (currentUrl.isNotBlank()) "Con Enlace" else "Sin Enlace",
                                        color = if (currentUrl.isNotBlank()) Color(0xFF43A047) else Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedTextField(
                                value = currentUrl,
                                onValueChange = { newUrl ->
                                    val epList = currentSeason.episodes.toMutableList()
                                    val epIdx = epList.indexOfFirst { it.episodeNumber == i }
                                    val newServers = if (newUrl.isNotBlank()) {
                                        listOf(MediaServer(id = "srv_1", streamUrl = newUrl.trim()))
                                    } else emptyList()

                                    if (epIdx >= 0) {
                                        val prev = epList[epIdx]
                                        epList[epIdx] = prev.copy(servers = newServers)
                                    } else {
                                        epList.add(
                                            Episode(
                                                id = "s${currentSeason.seasonNumber}_ep_$i",
                                                episodeNumber = i,
                                                title = "Capítulo $i",
                                                servers = newServers
                                            )
                                        )
                                    }
                                    val updatedSeasons = seasonsList.toMutableList()
                                    val sIdx = selectedSeasonIndex.coerceIn(0, updatedSeasons.lastIndex)
                                    updatedSeasons[sIdx] = currentSeason.copy(episodes = epList.sortedBy { it.episodeNumber })
                                    seasonsList = updatedSeasons
                                },
                                placeholder = { Text("https://servidor.com/capitulo_$i.mp4", fontSize = 11.sp, color = Color.Gray) },
                                label = { Text("Enlace de Video (MP4 / HLS)", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 1,
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalSeasons = seasonsList.mapIndexed { idx, season ->
                        val countText = desiredCounts[idx]
                            ?: if (season.episodes.isNotEmpty()) season.episodes.size.toString()
                            else "16"
                        val targetCount = (countText.toIntOrNull() ?: season.episodes.size).coerceAtLeast(1)
                        val finalEpisodes = (1..targetCount).map { epNum ->
                            season.episodes.firstOrNull { it.episodeNumber == epNum }
                                ?: Episode(
                                    id = "s${season.seasonNumber}_ep_$epNum",
                                    episodeNumber = epNum,
                                    title = "Capítulo $epNum"
                                )
                        }
                        season.copy(episodes = finalEpisodes)
                    }
                    val totalEps = finalSeasons.sumOf { it.episodes.size }
                    onSave(
                        series.copy(
                            title = title,
                            overview = overview,
                            posterUrl = posterUrl,
                            bannerUrl = bannerUrl,
                            releaseYear = yearText.toIntOrNull() ?: 2024,
                            category = category,
                            trailerUrl = trailerUrl,
                            contentType = contentType,
                            status = status,
                            totalEpisodes = if (totalEps > 0) totalEps else (yearText.toIntOrNull() ?: 0),
                            seasons = finalSeasons
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
            ) {
                Text("Guardar Serie")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.White) }
        },
        containerColor = Color(0xFF181818)
    )
}

@Composable
fun CategoryFormDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Categoría", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre de la Categoría") })
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Descripción") })
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onSave(name, desc) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.White) }
        },
        containerColor = Color(0xFF181818)
    )
}
