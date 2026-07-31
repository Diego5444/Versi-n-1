package com.example.data.model

data class RecommendationItem(
    val movie: Movie,
    val matchScore: Int, // e.g. 95%
    val recommendationReason: String, // e.g. "Basado en tu interés por Ciencia Ficción"
    val matchingGenres: List<String> = emptyList()
)
