package com.zakratv.app.data.ranking

import com.zakratv.app.data.model.StreamLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exercises shipped StreamRanker: cached/premium/direct RD always above non-cached magnets.
 */
class StreamRankerTest {

    private fun link(
        name: String,
        url: String,
        cached: Boolean = false,
        premium: Boolean = false,
        direct: Boolean = false,
        quality: String = "1080p",
        source: String = "",
        language: String = "",
        seeds: Int = 0,
    ) = StreamLink(
        name = name,
        url = url,
        quality = quality,
        language = language,
        isCached = cached,
        isPremium = premium,
        isDirect = direct,
        source = source,
        seeds = seeds,
    )

    @Test
    fun cachedPremiumDirectRanksAboveMagnet() {
        val magnet = link(
            name = "4K HDR magnet",
            url = "magnet:?xt=urn:btih:ABCDEF",
            quality = "2160p",
            seeds = 500,
        )
        val cached = link(
            name = "RD Cached 1080p",
            url = "https://download.real-debrid.com/d/xyz/file.mkv",
            cached = true,
            premium = true,
            direct = true,
            quality = "1080p",
            source = "Real-Debrid",
        )
        val sorted = StreamRanker.sort(listOf(magnet, cached))
        assertEquals(cached.url, sorted.first().url)
        assertTrue(StreamRanker.rankScore(cached) > StreamRanker.rankScore(magnet))
    }

    @Test
    fun pickBestPrefersHighestQualityAmongCached() {
        val c720 = link(
            "RD 720",
            "https://cdn.example/a.mkv",
            cached = true,
            premium = true,
            direct = true,
            quality = "720p",
            source = "Real-Debrid",
        )
        val c4k = link(
            "RD 4K HDR",
            "https://cdn.example/b.mkv",
            cached = true,
            premium = true,
            direct = true,
            quality = "2160p",
            source = "Real-Debrid",
        )
        val best = StreamRanker.pickBest(listOf(c720, c4k))
        assertEquals(c4k.url, best!!.url)
    }

    @Test
    fun nonCachedHttpStillBeatsMagnet() {
        val magnet = link("mag", "magnet:?xt=urn:btih:1", quality = "2160p", seeds = 999)
        val http = link(
            "hoster",
            "https://hoster.example/file.mp4",
            direct = true,
            quality = "720p",
        )
        val sorted = StreamRanker.sort(listOf(magnet, http))
        assertEquals(http.url, sorted.first().url)
    }

    @Test
    fun isPlayableDirectRequiresHttpAndFlags() {
        val good = link(
            "rd",
            "https://download.real-debrid.com/x",
            cached = true,
            direct = true,
        )
        val magnet = link("m", "magnet:?xt=urn:btih:zz")
        assertTrue(StreamRanker.isPlayableDirect(good))
        assertFalse(StreamRanker.isPlayableDirect(magnet))
    }

    @Test
    fun latinoRanksAboveCachedEnglishAndRussian() {
        // The user's core complaint: movies came out in English/Russian. Latino must win,
        // even when the English/Russian option is RD-cached and the Latino one only http.
        val cachedEnglish = link(
            "English 1080p",
            "https://cdn.example/en.mkv",
            cached = true,
            premium = true,
            direct = true,
            quality = "1080p",
            language = "en",
            source = "Real-Debrid",
        )
        val cachedRussian = link(
            "Русский 2160p",
            "https://cdn.example/ru.mkv",
            cached = true,
            premium = true,
            direct = true,
            quality = "2160p",
            language = "ru",
            source = "Real-Debrid",
        )
        val latino = link(
            "Latino 720p 🇲🇽",
            "https://cdn.example/lat.mkv",
            direct = true,
            quality = "720p",
            language = "es-LAT",
        )
        val sorted = StreamRanker.sort(listOf(cachedEnglish, cachedRussian, latino))
        assertEquals(latino.url, sorted.first().url)
        // Russian (unknown audio) must be dead last of the three.
        assertEquals(cachedRussian.url, sorted.last().url)
    }

    @Test
    fun spanishLabelBreaksTiesAmongSimilarLinks() {
        val en = link(
            "English 1080p",
            "https://cdn.example/en.mkv",
            cached = true,
            premium = true,
            direct = true,
            quality = "1080p",
            language = "en",
            source = "Real-Debrid",
        )
        val es = link(
            "Latino 1080p",
            "https://cdn.example/es.mkv",
            cached = true,
            premium = true,
            direct = true,
            quality = "1080p",
            language = "es-LAT",
            source = "Real-Debrid",
        )
        val sorted = StreamRanker.sort(listOf(en, es))
        assertEquals(es.url, sorted.first().url)
    }
}
