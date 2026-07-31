package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.FavoriteItem
import com.example.data.model.MovieItem
import com.example.data.model.SeriesItem
import com.example.data.repository.AuthRepository
import com.example.data.repository.UserDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val userDataRepository: UserDataRepository = UserDataRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _favSearchQuery = MutableStateFlow("")
    val favSearchQuery: StateFlow<String> = _favSearchQuery.asStateFlow()

    private val currentUserId = authRepository.observeAuthState()

    val favoritesList: StateFlow<List<FavoriteItem>> = currentUserId.flatMapLatest { user ->
        if (user != null) {
            userDataRepository.getFavoritesFlow(user.uid)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setFavSearchQuery(query: String) {
        _favSearchQuery.value = query
    }

    fun isFavorite(contentId: String): Boolean {
        return favoritesList.value.any { it.contentId == contentId }
    }

    fun removeFavorite(contentId: String) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            userDataRepository.toggleFavorite(
                uid = uid,
                contentId = contentId,
                contentType = "",
                title = "",
                posterUrl = "",
                rating = 0.0,
                releaseYear = 0,
                isCurrentlyFav = true
            )
        }
    }

    fun toggleMovieFavorite(movie: MovieItem) {
        val uid = authRepository.currentUser?.uid ?: return
        val currentlyFav = isFavorite(movie.id)
        viewModelScope.launch {
            userDataRepository.toggleFavorite(
                uid = uid,
                contentId = movie.id,
                contentType = "movie",
                title = movie.title,
                posterUrl = movie.posterUrl,
                rating = movie.rating,
                releaseYear = movie.releaseYear,
                isCurrentlyFav = currentlyFav
            )
        }
    }

    fun toggleSeriesFavorite(series: SeriesItem) {
        val uid = authRepository.currentUser?.uid ?: return
        val currentlyFav = isFavorite(series.id)
        viewModelScope.launch {
            userDataRepository.toggleFavorite(
                uid = uid,
                contentId = series.id,
                contentType = series.contentType,
                title = series.title,
                posterUrl = series.posterUrl,
                rating = series.rating,
                releaseYear = series.releaseYear,
                isCurrentlyFav = currentlyFav
            )
        }
    }
}
