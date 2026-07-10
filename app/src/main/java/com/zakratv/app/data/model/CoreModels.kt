package com.zakratv.app.data.model

enum class MediaType { MOVIE, SERIES }

data class MediaItem(
    val id: Int,
    val mediaType: MediaType,
    val title: String,
    val originalTitle: String = "",
    val overview: String = "",
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val releaseDate: String? = null,
    val voteAverage: Double = 0.0,
    val genreIds: List<Int> = emptyList(),
    val imdbId: String? = null,
    val originalLanguage: String? = null,
    val popularity: Double = 0.0,
) {
    val year: Int?
        get() = releaseDate?.take(4)?.toIntOrNull()

    fun posterUrl(size: String = "w342"): String? =
        posterPath?.let { "https://image.tmdb.org/t/p/$size$it" }

    fun backdropUrl(size: String = "w780"): String? =
        backdropPath?.let { "https://image.tmdb.org/t/p/$size$it" }
}

data class Season(
    val seasonNumber: Int,
    val name: String,
    val episodeCount: Int,
    val posterPath: String? = null,
)

data class Episode(
    val id: Int,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val name: String,
    val overview: String = "",
    val stillPath: String? = null,
    val airDate: String? = null,
) {
    fun stillUrl(size: String = "w300"): String? =
        stillPath?.let { "https://image.tmdb.org/t/p/$size$it" }
}

/**
 * Stream candidate for playback. Ranking prefers Real-Debrid cached/premium direct HTTPS.
 */
data class StreamLink(
    val name: String,
    val url: String,
    val quality: String = "",
    val size: String = "",
    val language: String = "",
    val isCached: Boolean = false,
    val isPremium: Boolean = false,
    val isDirect: Boolean = false,
    val source: String = "",
    val seeds: Int = 0,
) {
    val playableUrl: String get() = url
}

data class CatalogSection(
    val id: String,
    val title: String,
    val items: List<MediaItem>,
)

data class CatalogFilter(
    val language: String = "es",
    val genreId: Int? = null,
    val year: Int? = null,
    val sortBy: String = "popularity.desc",
)
