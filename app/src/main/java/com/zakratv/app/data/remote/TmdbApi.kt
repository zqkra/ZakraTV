package com.zakratv.app.data.remote

import com.zakratv.app.BuildConfig
import com.zakratv.app.data.model.CatalogFilter
import com.zakratv.app.data.model.Episode
import com.zakratv.app.data.model.MediaItem
import com.zakratv.app.data.model.MediaType
import com.zakratv.app.data.model.Season
import com.zakratv.app.data.model.TmdbGenre
import com.zakratv.app.data.model.TmdbGenreList
import com.zakratv.app.data.model.TmdbMovieDetail
import com.zakratv.app.data.model.TmdbPagedResponse
import com.zakratv.app.data.model.TmdbSeasonDetail
import com.zakratv.app.data.model.TmdbTvDetail
import com.zakratv.app.data.ranking.LanguagePreference
import com.zakratv.app.data.ranking.LanguagePreference.sortMediaSpanishFirst
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request

class TmdbApi(
    private val apiKey: String = BuildConfig.TMDB_API_KEY,
    private val language: String = LanguagePreference.TMDB_LANGUAGE,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val base = "https://api.themoviedb.org/3"

    fun hasApiKey(): Boolean = apiKey.isNotBlank()

    suspend fun trending(page: Int = 1): List<MediaItem> =
        paged("$base/trending/all/week", page).map {
            it.toMediaItem(
                when (it.mediaType) {
                    "tv" -> MediaType.SERIES
                    else -> MediaType.MOVIE
                }
            )
        }.let(::sortMediaSpanishFirst)

    suspend fun popularMovies(page: Int = 1, filter: CatalogFilter = CatalogFilter()): List<MediaItem> =
        discover("movie", page, filter).map { it.toMediaItem(MediaType.MOVIE) }
            .let(::sortMediaSpanishFirst)

    suspend fun popularSeries(page: Int = 1, filter: CatalogFilter = CatalogFilter()): List<MediaItem> =
        discover("tv", page, filter).map { it.toMediaItem(MediaType.SERIES) }
            .let(::sortMediaSpanishFirst)

    suspend fun nowPlaying(page: Int = 1): List<MediaItem> =
        paged("$base/movie/now_playing", page).map { it.toMediaItem(MediaType.MOVIE) }
            .let(::sortMediaSpanishFirst)

    suspend fun topRatedMovies(page: Int = 1): List<MediaItem> =
        paged("$base/movie/top_rated", page).map { it.toMediaItem(MediaType.MOVIE) }
            .let(::sortMediaSpanishFirst)

    suspend fun topRatedSeries(page: Int = 1): List<MediaItem> =
        paged("$base/tv/top_rated", page).map { it.toMediaItem(MediaType.SERIES) }
            .let(::sortMediaSpanishFirst)

    /**
     * Robust search: multi + movie + tv, merged and de-duplicated.
     * Works better for series titles in Spanish/English.
     */
    suspend fun search(query: String, page: Int = 1): List<MediaItem> {
        if (query.isBlank()) return emptyList()
        val q = query.trim()
        val multi = searchEndpoint("multi", q, page)
        val movies = searchEndpoint("movie", q, page).map { it.copy(mediaType = "movie") }
        val series = searchEndpoint("tv", q, page).map { it.copy(mediaType = "tv") }

        val merged = LinkedHashMap<String, MediaItem>()
        fun putAll(list: List<com.zakratv.app.data.model.TmdbMediaDto>) {
            for (dto in list) {
                val type = when (dto.mediaType) {
                    "tv" -> MediaType.SERIES
                    "movie" -> MediaType.MOVIE
                    else -> continue
                }
                val item = dto.toMediaItem(type)
                val key = "${type.name}:${item.id}"
                if (key !in merged) merged[key] = item
            }
        }
        // Prefer series/movies explicit endpoints when multi misses media_type
        putAll(series)
        putAll(movies)
        putAll(multi.filter { it.mediaType == "movie" || it.mediaType == "tv" })
        // multi items without media_type (shouldn't happen) skip
        return sortMediaSpanishFirst(merged.values.toList())
    }

    private fun searchEndpoint(
        path: String,
        query: String,
        page: Int,
    ): List<com.zakratv.app.data.model.TmdbMediaDto> {
        val url = "$base/search/$path".toHttpUrl().newBuilder()
            .addQueryParameter("api_key", apiKey)
            .addQueryParameter("language", language)
            .addQueryParameter("query", query)
            .addQueryParameter("page", page.toString())
            .addQueryParameter("include_adult", "false")
            .build()
        return try {
            val body = get(url.toString())
            json.decodeFromString(TmdbPagedResponse.serializer(), body).results
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun movieDetail(id: Int): MediaItem {
        val url = "$base/movie/$id".toHttpUrl().newBuilder()
            .addQueryParameter("api_key", apiKey)
            .addQueryParameter("language", language)
            .addQueryParameter("append_to_response", "external_ids")
            .build()
        val body = get(url.toString())
        return json.decodeFromString(TmdbMovieDetail.serializer(), body).toMediaItem()
    }

    suspend fun seriesDetail(id: Int): Pair<MediaItem, List<Season>> {
        val url = "$base/tv/$id".toHttpUrl().newBuilder()
            .addQueryParameter("api_key", apiKey)
            .addQueryParameter("language", language)
            .addQueryParameter("append_to_response", "external_ids")
            .build()
        val body = get(url.toString())
        val detail = json.decodeFromString(TmdbTvDetail.serializer(), body)
        val seasons = detail.seasons
            .filter { it.seasonNumber > 0 }
            .map {
                Season(
                    seasonNumber = it.seasonNumber,
                    name = it.name,
                    episodeCount = it.episodeCount,
                    posterPath = it.posterPath,
                )
            }
        return detail.toMediaItem() to seasons
    }

    suspend fun seasonEpisodes(tvId: Int, season: Int): List<Episode> {
        val url = "$base/tv/$tvId/season/$season".toHttpUrl().newBuilder()
            .addQueryParameter("api_key", apiKey)
            .addQueryParameter("language", language)
            .build()
        val body = get(url.toString())
        return json.decodeFromString(TmdbSeasonDetail.serializer(), body)
            .episodes
            .map { it.toEpisode() }
    }

    suspend fun movieGenres(): List<TmdbGenre> {
        val url = "$base/genre/movie/list".toHttpUrl().newBuilder()
            .addQueryParameter("api_key", apiKey)
            .addQueryParameter("language", language)
            .build()
        return json.decodeFromString(TmdbGenreList.serializer(), get(url.toString())).genres
    }

    suspend fun tvGenres(): List<TmdbGenre> {
        val url = "$base/genre/tv/list".toHttpUrl().newBuilder()
            .addQueryParameter("api_key", apiKey)
            .addQueryParameter("language", language)
            .build()
        return json.decodeFromString(TmdbGenreList.serializer(), get(url.toString())).genres
    }

    private suspend fun discover(type: String, page: Int, filter: CatalogFilter): List<com.zakratv.app.data.model.TmdbMediaDto> {
        val builder = "$base/discover/$type".toHttpUrl().newBuilder()
            .addQueryParameter("api_key", apiKey)
            .addQueryParameter("language", language)
            .addQueryParameter("page", page.toString())
            .addQueryParameter("sort_by", filter.sortBy)
            .addQueryParameter("include_adult", "false")
        // Spanish-first metadata via language=; do NOT hard-filter catalog empty
        if (filter.language == "es") {
            builder.addQueryParameter("watch_region", "MX")
        }
        filter.genreId?.let { builder.addQueryParameter("with_genres", it.toString()) }
        filter.year?.let {
            if (type == "movie") builder.addQueryParameter("primary_release_year", it.toString())
            else builder.addQueryParameter("first_air_date_year", it.toString())
        }
        val body = get(builder.build().toString())
        return json.decodeFromString(TmdbPagedResponse.serializer(), body).results
    }

    private suspend fun paged(path: String, page: Int): List<com.zakratv.app.data.model.TmdbMediaDto> {
        val url = path.toHttpUrl().newBuilder()
            .addQueryParameter("api_key", apiKey)
            .addQueryParameter("language", language)
            .addQueryParameter("page", page.toString())
            .build()
        val body = get(url.toString())
        return json.decodeFromString(TmdbPagedResponse.serializer(), body).results
    }

    private fun get(url: String): String {
        if (!hasApiKey()) {
            throw IllegalStateException("Falta TMDB_API_KEY en local.properties")
        }
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .get()
            .build()
        HttpClientFactory.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("TMDb HTTP ${response.code}")
            }
            return response.body?.string().orEmpty()
        }
    }
}
