package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CategoryItem
import com.example.data.model.MovieItem
import com.example.data.model.SeriesItem
import com.example.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOption(val displayName: String) {
    NEWEST("Más nuevos"),
    OLDEST("Más antiguos"),
    MOST_VIEWED("Más vistos"),
    TOP_RATED("Mejor calificados")
}

class MovieViewModel(
    private val movieRepository: MovieRepository = MovieRepository()
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedYear = MutableStateFlow<Int?>(null)
    val selectedYear: StateFlow<Int?> = _selectedYear.asStateFlow()

    private val _selectedLanguage = MutableStateFlow<String?>(null)
    val selectedLanguage: StateFlow<String?> = _selectedLanguage.asStateFlow()

    private val _selectedCountry = MutableStateFlow<String?>(null)
    val selectedCountry: StateFlow<String?> = _selectedCountry.asStateFlow()

    private val _minRating = MutableStateFlow<Double?>(null)
    val minRating: StateFlow<Double?> = _minRating.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.NEWEST)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _selectedMovie = MutableStateFlow<MovieItem?>(null)
    val selectedMovie: StateFlow<MovieItem?> = _selectedMovie.asStateFlow()

    private val _selectedSeries = MutableStateFlow<SeriesItem?>(null)
    val selectedSeries: StateFlow<SeriesItem?> = _selectedSeries.asStateFlow()

    val allMovies: StateFlow<List<MovieItem>> = movieRepository.getMoviesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSeries: StateFlow<List<SeriesItem>> = movieRepository.getSeriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<CategoryItem>> = movieRepository.getCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredMovies: StateFlow<List<MovieItem>> = combine(
        allMovies,
        _searchQuery,
        _selectedCategory,
        _selectedYear,
        _sortOption
    ) { movies, query, cat, year, sort ->
        val filtered = movies.filter { movie ->
            val matchesQuery = query.isBlank() ||
                    movie.title.contains(query, ignoreCase = true) ||
                    movie.originalTitle.contains(query, ignoreCase = true) ||
                    movie.director.contains(query, ignoreCase = true) ||
                    movie.actors.contains(query, ignoreCase = true) ||
                    movie.overview.contains(query, ignoreCase = true) ||
                    movie.category.contains(query, ignoreCase = true) ||
                    movie.categories.any { it.contains(query, ignoreCase = true) } ||
                    movie.releaseYear.toString().contains(query) ||
                    movie.country.contains(query, ignoreCase = true)

            val matchesCategory = cat == null || movie.category.equals(cat, ignoreCase = true) || movie.categories.contains(cat)
            val matchesYear = year == null || movie.releaseYear == year

            matchesQuery && matchesCategory && matchesYear
        }

        when (sort) {
            SortOption.NEWEST -> filtered.sortedByDescending { it.releaseYear }
            SortOption.OLDEST -> filtered.sortedBy { it.releaseYear }
            SortOption.MOST_VIEWED -> filtered.sortedByDescending { it.viewsCount }
            SortOption.TOP_RATED -> filtered.sortedByDescending { it.rating }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val filteredSeries: StateFlow<List<SeriesItem>> = combine(
        allSeries,
        _searchQuery,
        _selectedCategory,
        _selectedYear,
        _sortOption
    ) { seriesList, query, cat, year, sort ->
        val filtered = seriesList.filter { series ->
            val matchesQuery = query.isBlank() ||
                    series.title.contains(query, ignoreCase = true) ||
                    series.originalTitle.contains(query, ignoreCase = true) ||
                    series.director.contains(query, ignoreCase = true) ||
                    series.actors.contains(query, ignoreCase = true) ||
                    series.overview.contains(query, ignoreCase = true) ||
                    series.category.contains(query, ignoreCase = true) ||
                    series.categories.any { it.contains(query, ignoreCase = true) }

            val matchesCategory = cat == null || series.category.equals(cat, ignoreCase = true) || series.categories.contains(cat)
            val matchesYear = year == null || series.releaseYear == year

            matchesQuery && matchesCategory && matchesYear
        }

        when (sort) {
            SortOption.NEWEST -> filtered.sortedByDescending { it.releaseYear }
            SortOption.OLDEST -> filtered.sortedBy { it.releaseYear }
            SortOption.MOST_VIEWED -> filtered.sortedByDescending { it.viewsCount }
            SortOption.TOP_RATED -> filtered.sortedByDescending { it.rating }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun setSelectedYear(year: Int?) {
        _selectedYear.value = if (_selectedYear.value == year) null else year
    }

    fun setSelectedLanguage(lang: String?) {
        _selectedLanguage.value = if (_selectedLanguage.value == lang) null else lang
    }

    fun setSelectedCountry(country: String?) {
        _selectedCountry.value = if (_selectedCountry.value == country) null else country
    }

    fun setMinRating(rating: Double?) {
        _minRating.value = if (_minRating.value == rating) null else rating
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun selectMovie(movie: MovieItem?) {
        _selectedMovie.value = movie
        if (movie != null) {
            viewModelScope.launch {
                movieRepository.incrementMovieViews(movie.id)
            }
        }
    }

    fun selectSeries(series: SeriesItem?) {
        _selectedSeries.value = series
        if (series != null) {
            viewModelScope.launch {
                movieRepository.incrementSeriesViews(series.id)
            }
        }
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedCategory.value = null
        _selectedYear.value = null
        _selectedLanguage.value = null
        _selectedCountry.value = null
        _minRating.value = null
        _sortOption.value = SortOption.NEWEST
    }
}
