package com.zakratv.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- TMDb wire models ---

@Serializable
data class TmdbPagedResponse(
    val page: Int = 1,
    val results: List<TmdbMediaDto> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 1,
    @SerialName("total_results") val totalResults: Int = 0,
)

@Serializable
data class TmdbMediaDto(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    @SerialName("original_title") val originalTitle: String? = null,
    @SerialName("original_name") val originalName: String? = null,
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
    @SerialName("media_type") val mediaType: String? = null,
    @SerialName("original_language") val originalLanguage: String? = null,
    val popularity: Double = 0.0,
) {
    fun toMediaItem(forcedType: MediaType? = null): MediaItem {
        val type = forcedType ?: when (mediaType) {
            "tv" -> MediaType.SERIES
            else -> MediaType.MOVIE
        }
        val displayTitle = when (type) {
            MediaType.MOVIE -> title ?: originalTitle ?: "Sin título"
            MediaType.SERIES -> name ?: originalName ?: "Sin título"
        }
        val original = when (type) {
            MediaType.MOVIE -> originalTitle ?: title.orEmpty()
            MediaType.SERIES -> originalName ?: name.orEmpty()
        }
        val date = when (type) {
            MediaType.MOVIE -> releaseDate
            MediaType.SERIES -> firstAirDate
        }
        return MediaItem(
            id = id,
            mediaType = type,
            title = displayTitle,
            originalTitle = original,
            overview = overview.orEmpty(),
            posterPath = posterPath,
            backdropPath = backdropPath,
            releaseDate = date,
            voteAverage = voteAverage,
            genreIds = genreIds,
            originalLanguage = originalLanguage,
            popularity = popularity,
        )
    }
}

@Serializable
data class TmdbExternalIds(
    @SerialName("imdb_id") val imdbId: String? = null,
)

@Serializable
data class TmdbMovieDetail(
    val id: Int,
    val title: String? = null,
    @SerialName("original_title") val originalTitle: String? = null,
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
    val genres: List<TmdbGenre> = emptyList(),
    @SerialName("original_language") val originalLanguage: String? = null,
    val popularity: Double = 0.0,
    @SerialName("external_ids") val externalIds: TmdbExternalIds? = null,
    @SerialName("imdb_id") val imdbId: String? = null,
) {
    fun toMediaItem(): MediaItem = MediaItem(
        id = id,
        mediaType = MediaType.MOVIE,
        title = title ?: originalTitle ?: "Sin título",
        originalTitle = originalTitle ?: title.orEmpty(),
        overview = overview.orEmpty(),
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseDate = releaseDate,
        voteAverage = voteAverage,
        genreIds = genreIds.ifEmpty { genres.map { it.id } },
        imdbId = imdbId ?: externalIds?.imdbId,
        originalLanguage = originalLanguage,
        popularity = popularity,
    )
}

@Serializable
data class TmdbTvDetail(
    val id: Int,
    val name: String? = null,
    @SerialName("original_name") val originalName: String? = null,
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    val genres: List<TmdbGenre> = emptyList(),
    @SerialName("original_language") val originalLanguage: String? = null,
    val popularity: Double = 0.0,
    @SerialName("external_ids") val externalIds: TmdbExternalIds? = null,
    val seasons: List<TmdbSeasonDto> = emptyList(),
) {
    fun toMediaItem(): MediaItem = MediaItem(
        id = id,
        mediaType = MediaType.SERIES,
        title = name ?: originalName ?: "Sin título",
        originalTitle = originalName ?: name.orEmpty(),
        overview = overview.orEmpty(),
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseDate = firstAirDate,
        voteAverage = voteAverage,
        genreIds = genres.map { it.id },
        imdbId = externalIds?.imdbId,
        originalLanguage = originalLanguage,
        popularity = popularity,
    )
}

@Serializable
data class TmdbGenre(val id: Int, val name: String = "")

@Serializable
data class TmdbSeasonDto(
    @SerialName("season_number") val seasonNumber: Int = 0,
    val name: String = "",
    @SerialName("episode_count") val episodeCount: Int = 0,
    @SerialName("poster_path") val posterPath: String? = null,
)

@Serializable
data class TmdbSeasonDetail(
    @SerialName("season_number") val seasonNumber: Int = 0,
    val name: String = "",
    val episodes: List<TmdbEpisodeDto> = emptyList(),
)

@Serializable
data class TmdbEpisodeDto(
    val id: Int,
    val name: String = "",
    val overview: String = "",
    @SerialName("episode_number") val episodeNumber: Int = 0,
    @SerialName("season_number") val seasonNumber: Int = 0,
    @SerialName("still_path") val stillPath: String? = null,
    @SerialName("air_date") val airDate: String? = null,
) {
    fun toEpisode(): Episode = Episode(
        id = id,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        name = name,
        overview = overview,
        stillPath = stillPath,
        airDate = airDate,
    )
}

@Serializable
data class TmdbGenreList(val genres: List<TmdbGenre> = emptyList())

// --- Stream discovery (Torrentio-style) ---

@Serializable
data class StreamAddonResponse(val streams: List<AddonStream> = emptyList())

@Serializable
data class AddonStream(
    val name: String? = null,
    val title: String? = null,
    val description: String? = null,
    val url: String? = null,
    val infoHash: String? = null,
    val behaviorHints: BehaviorHints? = null,
    val fileIdx: Int? = null,
)

@Serializable
data class BehaviorHints(
    val bingeGroup: String? = null,
    val filename: String? = null,
    val videoSize: Long? = null,
    val notWebReady: Boolean? = null,
)

// --- Real-Debrid ---

@Serializable
data class RdUser(
    val id: Int = 0,
    val username: String = "",
    val email: String = "",
    val points: Int = 0,
    val type: String = "",
    val premium: Int = 0,
)

@Serializable
data class RdDeviceCode(
    @SerialName("device_code") val deviceCode: String,
    @SerialName("user_code") val userCode: String,
    @SerialName("interval") val interval: Int = 5,
    @SerialName("expires_in") val expiresIn: Int = 1800,
    @SerialName("verification_url") val verificationUrl: String = "https://real-debrid.com/device",
    @SerialName("direct_verification_url") val directVerificationUrl: String? = null,
)

@Serializable
data class RdTokenResponse(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Int = 0,
    @SerialName("token_type") val tokenType: String? = null,
    val error: String? = null,
    @SerialName("error_code") val errorCode: Int? = null,
)

@Serializable
data class RdUnrestrict(
    val id: String = "",
    val filename: String = "",
    val mimeType: String = "",
    val filesize: Long = 0,
    val link: String = "",
    val host: String = "",
    val chunks: Int = 0,
    val crc: Int = 0,
    val download: String = "",
    val streamable: Int = 0,
)

@Serializable
data class RdTorrentAdded(
    val id: String = "",
    val uri: String = "",
)

@Serializable
data class RdTorrentInfo(
    val id: String = "",
    val filename: String = "",
    val status: String = "",
    val progress: Double = 0.0,
    val links: List<String> = emptyList(),
    val files: List<RdTorrentFile> = emptyList(),
)

@Serializable
data class RdTorrentFile(
    val id: Int = 0,
    val path: String = "",
    val bytes: Long = 0,
    val selected: Int = 0,
)

@Serializable
data class RdInstantAvailability(
    val raw: String = "",
)
