package com.zakratv.app.data.remote

import com.zakratv.app.data.model.AddonStream
import com.zakratv.app.data.model.MediaType
import com.zakratv.app.data.model.StreamAddonResponse
import com.zakratv.app.data.model.StreamLink
import com.zakratv.app.data.ranking.LanguagePreference
import com.zakratv.app.data.ranking.StreamRanker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import okhttp3.Request
import java.util.Locale

/**
 * Torrentio + Real-Debrid. Filters suspended hosts; ranks 🇲🇽 Latino first.
 */
class StreamProvider(
    private val realDebrid: RealDebridApi,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    suspend fun findStreams(
        imdbId: String,
        type: MediaType,
        season: Int? = null,
        episode: Int? = null,
    ): List<StreamLink> {
        if (imdbId.isBlank()) return emptyList()
        val cleanId = imdbId.trim().let { if (it.startsWith("tt")) it else "tt$it" }
        val typePath = if (type == MediaType.SERIES) "series" else "movie"
        val idPath = if (type == MediaType.SERIES && season != null && episode != null) {
            "$cleanId:$season:$episode"
        } else {
            cleanId
        }

        val links = mutableListOf<StreamLink>()
        val token = realDebrid.getToken()
        if (token.isNotBlank()) {
            links += fetchAddon(torrentioUrl(token, typePath, idPath), preferRd = true)
        }
        if (links.none { it.isCached || it.isDirect }) {
            links += fetchAddon(publicTorrentioUrl(typePath, idPath), preferRd = false)
        }
        if (realDebrid.hasToken()) {
            // Resolve the best magnets, Latino/Spanish first so a Latino torrent becomes
            // a playable RD link even when it wasn't cached yet.
            val magnets = links
                .filter { !StreamRanker.isHttpUrl(it.url) && !StreamRanker.isSuspendedOrDead(it) }
                .sortedByDescending { LanguagePreference.streamLanguageRank(it) }
                .take(6)
            links += resolveTopMagnets(magnets)
        }
        val ranked = StreamRanker.sort(links.distinctBy { it.url.lowercase(Locale.US) })
        return dropRemovedLinks(ranked)
    }

    /**
     * Probes the top links in parallel (1-byte ranged GET) and drops ONLY those whose
     * torrent was confirmed removed (404/410/451). Network errors, timeouts or odd
     * codes keep the link — working links are never discarded by accident.
     */
    private suspend fun dropRemovedLinks(links: List<StreamLink>): List<StreamLink> = coroutineScope {
        if (links.isEmpty()) return@coroutineScope links
        val head = links.take(HEALTH_CHECK_TOP)
        val deadFlags = head.map { link ->
            async(Dispatchers.IO) { isConfirmedRemoved(link) }
        }.awaitAll()
        val dead = head.filterIndexed { i, _ -> deadFlags[i] }.toHashSet()
        LinkHealth.dropConfirmedDead(links, HEALTH_CHECK_TOP) { it in dead }
    }

    private fun isConfirmedRemoved(link: StreamLink): Boolean {
        if (!StreamRanker.isHttpUrl(link.url)) return false // magnets can't be probed here
        return try {
            val request = Request.Builder()
                .url(link.url)
                .header("Range", "bytes=0-0")
                .header("User-Agent", "ZakraTV/1.6")
                .get()
                .build()
            HttpClientFactory.probeClient.newCall(request).execute().use { resp ->
                LinkHealth.isRemovedCode(resp.code)
            }
        } catch (_: Exception) {
            false // doubt → keep the link
        }
    }

    /**
     * Torrentio configured for a Spanish-speaking audience:
     * - language=latino,spanish → Latino/España releases are prioritized and flag-tagged (🇲🇽/🇪🇸).
     * - a provider list that KEEPS Latino/Spanish sources (cinecalidad, mejortorrent…) and drops
     *   Russian/anime trackers (rutor, rutracker…) so movies stop coming out in Russian.
     */
    private fun torrentioUrl(token: String, typePath: String, idPath: String): String {
        val config = "$SPANISH_CONFIG|realdebrid=$token"
        return "https://torrentio.strem.fun/$config/stream/$typePath/$idPath.json"
    }

    private fun publicTorrentioUrl(typePath: String, idPath: String): String =
        "https://torrentio.strem.fun/$SPANISH_CONFIG/stream/$typePath/$idPath.json"

    private fun fetchAddon(url: String, preferRd: Boolean): List<StreamLink> {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "ZakraTV/1.0")
                .get()
                .build()
            HttpClientFactory.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return emptyList()
                val parsed = json.decodeFromString(StreamAddonResponse.serializer(), body)
                parsed.streams.mapNotNull { mapStream(it, preferRd) }
                    .filterNot { StreamRanker.isSuspendedOrDead(it) }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun mapStream(s: AddonStream, preferRd: Boolean): StreamLink? {
        val label = listOfNotNull(s.name, s.title).joinToString(" ").ifBlank { "Stream" }
        val desc = s.description.orEmpty()
        val rawUrl = s.url?.takeIf { it.isNotBlank() }
            ?: s.infoHash?.let { hash -> "magnet:?xt=urn:btih:$hash" }
            ?: return null

        val text = "$label $desc".lowercase(Locale.US)
        if (StreamRanker.isSuspendedOrDead(
                StreamLink(name = label, url = rawUrl, source = desc),
            )
        ) {
            return null
        }

        val isHttp = StreamRanker.isHttpUrl(rawUrl)
        val cached = preferRd && isHttp && (
            text.contains("cached") ||
                text.contains("rd+") ||
                text.contains("[rd") ||
                text.contains("real-debrid") ||
                text.contains("⚡") ||
                isHttp
            )
        return StreamLink(
            name = label.take(120),
            url = rawUrl,
            quality = extractQuality(text),
            size = extractSize(text),
            language = extractLanguage(text),
            isCached = cached,
            isPremium = preferRd && isHttp,
            isDirect = isHttp,
            source = if (preferRd) "Real-Debrid" else "Torrentio",
            seeds = extractSeeds(text),
        )
    }

    private fun resolveTopMagnets(magnets: List<StreamLink>): List<StreamLink> {
        val out = mutableListOf<StreamLink>()
        for (m in magnets) {
            try {
                val direct = realDebrid.resolveMagnetToDirect(m.url) ?: continue
                out += m.copy(
                    url = direct,
                    isCached = true,
                    isPremium = true,
                    isDirect = true,
                    source = "Real-Debrid",
                    name = "RD · ${m.name}",
                )
            } catch (_: Exception) {
                // skip
            }
        }
        return out
    }

    private fun extractQuality(text: String): String {
        val patterns = listOf("2160p", "4k", "1080p", "720p", "480p", "360p")
        for (p in patterns) if (text.contains(p)) return p.uppercase(Locale.US)
        if (text.contains("uhd")) return "4K"
        return ""
    }

    private fun extractLanguage(text: String): String {
        val t = " $text "
        // Flag emojis on RD/Torrentio releases are the most reliable audio marker.
        if (LanguagePreference.LATINO_FLAGS.any { t.contains(it) }) return "es-LAT"
        if (t.contains(LanguagePreference.SPAIN_FLAG)) return "es-ES"
        // Strict token checks — avoid "spa" inside random words
        return when {
            Regex("""\blatino\b|\blatin\b|\blatam\b|\bes-419\b|\bes-mx\b|\bes-la\b|\bes-lat\b|\bdual\s*lat|\bdoblaje\s*lat|\baudio\s*lat|[\s.\-_\[(]lat[\s.\-_\])]""", RegexOption.IGNORE_CASE)
                .containsMatchIn(t) -> "es-LAT"
            Regex("""\bcastellano\b|\bes-es\b|\bespaña\b|\bespana\b""", RegexOption.IGNORE_CASE)
                .containsMatchIn(t) -> "es-ES"
            Regex("""\bespañol\b|\bespanol\b|\bspanish\b""", RegexOption.IGNORE_CASE)
                .containsMatchIn(t) -> "es"
            Regex("""\benglish\b|\bingles\b|\binglés\b|\ben-us\b|\ben-gb\b""", RegexOption.IGNORE_CASE)
                .containsMatchIn(t) -> "en"
            Regex("""\bmulti\b|\bdual\b""", RegexOption.IGNORE_CASE).containsMatchIn(t) -> "multi"
            else -> ""
        }
    }

    companion object {
        /** How many top-ranked links get health-probed (what the UI actually shows). */
        private const val HEALTH_CHECK_TOP = 15

        /**
         * Torrentio config prefix shared by the RD and public URLs.
         * `providers` keeps mainstream + Latino/Spanish sources and excludes Russian/anime
         * trackers; `language=latino,spanish` floats those audios to the top with flags.
         */
        const val SPANISH_CONFIG =
            "providers=yts,eztv,rarbg,1337x,thepiratebay,kickasstorrents,torrentgalaxy,magnetdl," +
                "torrent9,ilcorsaronero,cinecalidad,mejortorrent,wolfmax4k,comando,bludv" +
                "|language=latino,spanish|sort=qualitysize"
    }

    private fun extractSize(text: String): String {
        val regex = Regex("""(\d+(?:\.\d+)?)\s*(GB|MB|TB)""", RegexOption.IGNORE_CASE)
        return regex.find(text)?.value.orEmpty()
    }

    private fun extractSeeds(text: String): Int {
        val regex = Regex("""seeds?\s*[:=]?\s*(\d+)""", RegexOption.IGNORE_CASE)
        return regex.find(text)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
    }
}
