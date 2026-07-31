package com.example.data.repository

import com.example.data.model.FavoriteItem
import com.example.data.model.MovieItem
import com.example.data.model.RecommendationItem
import com.example.data.model.WatchHistoryEntry

class RecommendationEngine {

    fun generateRecommendations(
        allMovies: List<MovieItem>,
        watchHistory: List<WatchHistoryEntry>,
        favorites: List<FavoriteItem>
    ): List<RecommendationItem> {
        if (allMovies.isEmpty()) return emptyList()

        val watchedMovieIds = watchHistory.map { it.contentId }.toSet()
        val favoriteMovieIds = favorites.map { it.contentId }.toSet()

        val recommendations = mutableListOf<RecommendationItem>()

        allMovies.forEach { movie ->
            val isWatched = watchedMovieIds.contains(movie.id)
            var score = 70

            if (favoriteMovieIds.contains(movie.id)) score += 20
            if (movie.rating >= 8.0) score += 10
            if (movie.isPopular) score += 5

            val finalMatchScore = score.coerceIn(60, 99)

            val reason = when {
                favoriteMovieIds.contains(movie.id) -> "En tus favoritos"
                movie.rating >= 8.5 -> "Aclamada por la crítica"
                movie.isPopular -> "Populares del momento"
                else -> "Recomendación personalizada"
            }

            recommendations.add(
                RecommendationItem(
                    movie = movie,
                    matchScore = if (isWatched) (finalMatchScore - 10).coerceAtLeast(50) else finalMatchScore,
                    recommendationReason = if (isWatched) "Ya la viste" else reason,
                    matchingGenres = movie.categories
                )
            )
        }

        return recommendations.sortedByDescending { it.matchScore }
    }
}
