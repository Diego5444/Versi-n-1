package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.RecommendationItem
import com.example.data.repository.AuthRepository
import com.example.data.repository.MovieRepository
import com.example.data.repository.RecommendationEngine
import com.example.data.repository.UserDataRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

class RecommendationsViewModel(
    private val movieRepository: MovieRepository = MovieRepository(),
    private val userDataRepository: UserDataRepository = UserDataRepository(),
    private val authRepository: AuthRepository = AuthRepository(),
    private val recommendationEngine: RecommendationEngine = RecommendationEngine()
) : ViewModel() {

    private val allMovies = movieRepository.getMoviesFlow()
    private val currentUserId = authRepository.observeAuthState()

    private val watchHistory = currentUserId.flatMapLatest { user ->
        if (user != null) userDataRepository.getWatchHistoryFlow(user.uid) else flowOf(emptyList())
    }

    private val favorites = currentUserId.flatMapLatest { user ->
        if (user != null) userDataRepository.getFavoritesFlow(user.uid) else flowOf(emptyList())
    }

    val recommendations: StateFlow<List<RecommendationItem>> = combine(
        allMovies,
        watchHistory,
        favorites
    ) { movies, history, favs ->
        recommendationEngine.generateRecommendations(movies, history, favs)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
}
