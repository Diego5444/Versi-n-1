package com.example.data.model

data class FavoriteItem(
    val contentId: String = "",
    val contentType: String = "movie", // "movie", "series", "anime"
    val title: String = "",
    val posterUrl: String = "",
    val rating: Double = 0.0,
    val releaseYear: Int = 2024,
    val addedAt: Long = System.currentTimeMillis()
)
