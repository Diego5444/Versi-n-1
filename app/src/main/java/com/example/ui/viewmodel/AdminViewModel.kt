package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CategoryItem
import com.example.data.model.Episode
import com.example.data.model.MediaServer
import com.example.data.model.MovieItem
import com.example.data.model.Season
import com.example.data.model.SeriesItem
import com.example.data.model.SubtitleTrack
import com.example.data.model.UserProfile
import com.example.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(
    private val movieRepository: MovieRepository = MovieRepository()
) : ViewModel() {

    private val _adminState = MutableStateFlow<String?>(null)
    val adminState: StateFlow<String?> = _adminState.asStateFlow()

    val moviesList: StateFlow<List<MovieItem>> = movieRepository.getMoviesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val seriesList: StateFlow<List<SeriesItem>> = movieRepository.getSeriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoriesList: StateFlow<List<CategoryItem>> = movieRepository.getCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val usersList: StateFlow<List<UserProfile>> = movieRepository.getUsersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Movie Operations
    fun saveMovie(movie: MovieItem) {
        viewModelScope.launch {
            val res = movieRepository.saveMovie(movie)
            res.onSuccess {
                _adminState.value = "Película guardada con éxito"
            }.onFailure {
                _adminState.value = "Error al guardar película: ${it.message}"
            }
        }
    }

    fun deleteMovie(movieId: String) {
        viewModelScope.launch {
            val res = movieRepository.deleteMovie(movieId)
            res.onSuccess {
                _adminState.value = "Película eliminada"
            }.onFailure {
                _adminState.value = "Error al eliminar: ${it.message}"
            }
        }
    }

    fun toggleMoviePublished(movie: MovieItem) {
        saveMovie(movie.copy(isPublished = !movie.isPublished))
    }

    fun duplicateMovie(movie: MovieItem) {
        val dup = movie.copy(
            id = "",
            title = "${movie.title} (Copia)",
            viewsCount = 0
        )
        saveMovie(dup)
    }

    // Series Operations
    fun saveSeries(series: SeriesItem) {
        viewModelScope.launch {
            val res = movieRepository.saveSeries(series)
            res.onSuccess {
                _adminState.value = "Série/Anime guardado con éxito"
            }.onFailure {
                _adminState.value = "Error al guardar serie: ${it.message}"
            }
        }
    }

    fun deleteSeries(seriesId: String) {
        viewModelScope.launch {
            val res = movieRepository.deleteSeries(seriesId)
            res.onSuccess {
                _adminState.value = "Serie eliminada"
            }.onFailure {
                _adminState.value = "Error al eliminar: ${it.message}"
            }
        }
    }

    // Category Operations
    fun saveCategory(category: CategoryItem) {
        viewModelScope.launch {
            val res = movieRepository.saveCategory(category)
            res.onSuccess {
                _adminState.value = "Categoría guardada"
            }.onFailure {
                _adminState.value = "Error: ${it.message}"
            }
        }
    }

    fun deleteCategory(catId: String) {
        viewModelScope.launch {
            movieRepository.deleteCategory(catId)
        }
    }

    // User Role Operations
    fun updateUserRole(uid: String, role: String) {
        viewModelScope.launch {
            val res = movieRepository.updateUserRole(uid, role)
            res.onSuccess {
                _adminState.value = "Rol actualizado a $role"
            }.onFailure {
                _adminState.value = "Error al actualizar rol: ${it.message}"
            }
        }
    }

    fun clearAdminState() {
        _adminState.value = null
    }
}
