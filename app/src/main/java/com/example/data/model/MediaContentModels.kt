package com.example.data.model

data class SubtitleTrack(
    val id: String = "",
    val language: String = "Español",
    val vttUrl: String = ""
)

data class MediaServer(
    val id: String = "",
    val serverName: String = "Servidor 1",
    val language: String = "Español Latino",
    val streamUrl: String = "",
    val quality: String = "1080p",
    val subtitles: List<SubtitleTrack> = emptyList()
)

data class Episode(
    val id: String = "",
    val episodeNumber: Int = 1,
    val title: String = "",
    val overview: String = "",
    val durationMinutes: Int = 45,
    val thumbnailUrl: String = "",
    val servers: List<MediaServer> = emptyList()
) {
    fun hasVideoLink(): Boolean = servers.any { it.streamUrl.isNotBlank() }
}

data class Season(
    val seasonNumber: Int = 1,
    val title: String = "Temporada 1",
    val episodes: List<Episode> = emptyList()
)

data class CategoryItem(
    val id: String = "",
    val name: String = "",
    val description: String = ""
)

data class MovieItem(
    val id: String = "",
    val title: String = "",
    val originalTitle: String = "",
    val overview: String = "",
    val posterUrl: String = "",
    val bannerUrl: String = "",
    val thumbnailUrl: String = "",
    val releaseYear: Int = 2024,
    val durationMinutes: Int = 120,
    val director: String = "",
    val actors: String = "",
    val category: String = "",
    val categories: List<String> = emptyList(),
    val country: String = "",
    val language: String = "Español Latino",
    val rating: Double = 0.0,
    val classification: String = "+13",
    val trailerUrl: String = "",
    val releaseDate: String = "",
    val isPopular: Boolean = false,
    val isFeatured: Boolean = false,
    val isNew: Boolean = false,
    val isPublished: Boolean = true,
    val viewsCount: Int = 0,
    val servers: List<MediaServer> = emptyList()
)

data class SeriesItem(
    val id: String = "",
    val title: String = "",
    val originalTitle: String = "",
    val overview: String = "",
    val posterUrl: String = "",
    val bannerUrl: String = "",
    val thumbnailUrl: String = "",
    val releaseYear: Int = 2024,
    val director: String = "",
    val actors: String = "",
    val category: String = "",
    val categories: List<String> = emptyList(),
    val country: String = "",
    val rating: Double = 0.0,
    val classification: String = "+13",
    val trailerUrl: String = "",
    val isPopular: Boolean = false,
    val isFeatured: Boolean = false,
    val isNew: Boolean = false,
    val isPublished: Boolean = true,
    val contentType: String = "series", // "series" or "anime"
    val viewsCount: Int = 0,
    val totalEpisodes: Int = 0,
    val status: String = "En Emisión", // "En Emisión" or "Finalizado"
    val seasons: List<Season> = emptyList()
)
