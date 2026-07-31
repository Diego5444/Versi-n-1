package com.example.data.model

data class WatchHistoryEntry(
    val historyId: String = "",
    val contentId: String = "",
    val contentType: String = "movie", // "movie" or "episode"
    val title: String = "",
    val posterUrl: String = "",
    val watchedAtTimestamp: Long = System.currentTimeMillis(),
    val progressPercentage: Int = 0,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)
