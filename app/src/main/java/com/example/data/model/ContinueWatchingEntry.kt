package com.example.data.model

data class ContinueWatchingEntry(
    val contentId: String = "",
    val contentType: String = "movie", // "movie" or "episode"
    val title: String = "",
    val posterUrl: String = "",
    val seasonNumber: Int = 0,
    val episodeNumber: Int = 0,
    val episodeTitle: String = "",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val lastPlayedTimestamp: Long = System.currentTimeMillis()
)
