package com.zakratv.app.data.repository

import com.zakratv.app.data.local.PrefsStore
import com.zakratv.app.data.model.CatalogFilter
import com.zakratv.app.data.model.CatalogSection
import com.zakratv.app.data.model.Episode
import com.zakratv.app.data.model.MediaItem
import com.zakratv.app.data.model.MediaType
import com.zakratv.app.data.model.Season
import com.zakratv.app.data.model.StreamLink
import com.zakratv.app.data.model.TmdbGenre
import com.zakratv.app.data.remote.RealDebridApi
import com.zakratv.app.data.remote.StreamProvider
import com.zakratv.app.data.remote.TmdbApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class CatalogRepository(
    private val tmdb: TmdbApi,
    private val realDebrid: RealDebridApi,
    private val streams: StreamProvider,
    private val prefs: PrefsStore,
) {
    suspend fun homeSections(): List<CatalogSection> = withContext(Dispatchers.IO) {
        coroutineScope {
            val trending = async { runCatching { tmdb.trending() }.getOrDefault(emptyList()) }
            val movies = async { runCatching { tmdb.popularMovies() }.getOrDefault(emptyList()) }
            val series = async { runCatching { tmdb.popularSeries() }.getOrDefault(emptyList()) }
            val now = async { runCatching { tmdb.nowPlaying() }.getOrDefault(emptyList()) }
            val topM = async { runCatching { tmdb.topRatedMovies() }.getOrDefault(emptyList()) }
            listOf(
                CatalogSection("trending", "Tendencias", trending.await()),
                CatalogSection("now", "En cartelera", now.await()),
                CatalogSection("movies", "Películas populares", movies.await()),
                CatalogSection("series", "Series populares", series.await()),
                CatalogSection("top", "Mejor valoradas", topM.await()),
            ).filter { it.items.isNotEmpty() }
        }
    }

    suspend fun movies(page: Int = 1, filter: CatalogFilter = CatalogFilter()): List<MediaItem> =
        withContext(Dispatchers.IO) {
            runCatching { tmdb.popularMovies(page, filter) }.getOrDefault(emptyList())
        }

    suspend fun series(page: Int = 1, filter: CatalogFilter = CatalogFilter()): List<MediaItem> =
        withContext(Dispatchers.IO) {
            runCatching { tmdb.popularSeries(page, filter) }.getOrDefault(emptyList())
        }

    suspend fun trending(page: Int = 1): List<MediaItem> =
        withContext(Dispatchers.IO) {
            runCatching { tmdb.trending(page) }.getOrDefault(emptyList())
        }

    suspend fun detail(item: MediaItem): MediaItem = withContext(Dispatchers.IO) {
        when (item.mediaType) {
            MediaType.MOVIE -> runCatching { tmdb.movieDetail(item.id) }.getOrDefault(item)
            MediaType.SERIES -> runCatching { tmdb.seriesDetail(item.id).first }.getOrDefault(item)
        }
    }

    suspend fun seasons(tvId: Int): List<Season> = withContext(Dispatchers.IO) {
        runCatching { tmdb.seriesDetail(tvId).second }.getOrDefault(emptyList())
    }

    suspend fun episodes(tvId: Int, season: Int): List<Episode> = withContext(Dispatchers.IO) {
        runCatching { tmdb.seasonEpisodes(tvId, season) }.getOrDefault(emptyList())
    }

    suspend fun movieGenres(): List<TmdbGenre> = withContext(Dispatchers.IO) {
        runCatching { tmdb.movieGenres() }.getOrDefault(emptyList())
    }

    suspend fun findStreams(
        item: MediaItem,
        season: Int? = null,
        episode: Int? = null,
    ): List<StreamLink> = withContext(Dispatchers.IO) {
        // Always refresh detail so series get imdb_id from external_ids
        val detailed = runCatching { detail(item) }.getOrDefault(item)
        var imdb = detailed.imdbId?.trim().orEmpty()
        if (imdb.isBlank()) {
            // Second try: force series/movie detail again
            imdb = runCatching {
                when (item.mediaType) {
                    MediaType.MOVIE -> tmdb.movieDetail(item.id).imdbId
                    MediaType.SERIES -> tmdb.seriesDetail(item.id).first.imdbId
                }
            }.getOrNull()?.orEmpty().orEmpty()
        }
        if (imdb.isBlank()) return@withContext emptyList()
        if (!imdb.startsWith("tt")) imdb = "tt$imdb"
        runCatching {
            streams.findStreams(imdb, detailed.mediaType, season, episode)
        }.getOrDefault(emptyList())
    }

    suspend fun search(query: String): List<MediaItem> =
        withContext(Dispatchers.IO) {
            val q = query.trim()
            if (q.length < 2) return@withContext emptyList()
            runCatching { tmdb.search(q) }.getOrDefault(emptyList())
        }

    suspend fun myListItems(): List<MediaItem> = withContext(Dispatchers.IO) {
        val keys = prefs.myListKeys()
        keys.mapNotNull { key ->
            val parts = key.split(":")
            if (parts.size != 2) return@mapNotNull null
            val type = parts[0]
            val id = parts[1].toIntOrNull() ?: return@mapNotNull null
            runCatching {
                when (type) {
                    "movie" -> tmdb.movieDetail(id)
                    "tv" -> tmdb.seriesDetail(id).first
                    else -> null
                }
            }.getOrNull()
        }
    }

    suspend fun toggleList(item: MediaItem): Boolean = withContext(Dispatchers.IO) {
        val type = if (item.mediaType == MediaType.MOVIE) "movie" else "tv"
        runCatching { prefs.toggleMyList(PrefsStore.listKey(type, item.id)) }.getOrDefault(false)
    }

    suspend fun isInList(item: MediaItem): Boolean = withContext(Dispatchers.IO) {
        val type = if (item.mediaType == MediaType.MOVIE) "movie" else "tv"
        runCatching { prefs.isInMyList(PrefsStore.listKey(type, item.id)) }.getOrDefault(false)
    }

    fun rd(): RealDebridApi = realDebrid
}
