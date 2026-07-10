package com.zakratv.app.data.ranking

import com.zakratv.app.data.model.StreamLink
import com.zakratv.app.data.update.UpdateVersionLogic

/**
 * Isolated JVM tests — shipped LanguagePreference + StreamRanker + UpdateVersionLogic.
 */
object StandaloneRankingHarness {
    @JvmStatic
    fun main(args: Array<String>) {
        var failed = 0
        fun check(name: String, cond: Boolean) {
            if (cond) println("PASS $name")
            else {
                println("FAIL $name")
                failed++
            }
        }

        // --- False-positive traps: English must NOT become Spanish ---
        val engOnly = StreamLink(
            name = "BluRay 1080p x264 English AAC",
            url = "https://cdn.example/en.mkv",
            quality = "1080p",
            language = "en",
            isCached = true,
            isPremium = true,
            isDirect = true,
            source = "Real-Debrid",
        )
        check(
            "english_not_labeled_spanish",
            LanguagePreference.streamLanguageLabel(engOnly) == "Inglés",
        )
        check(
            "english_not_isSpanish",
            !LanguagePreference.isSpanishStream(engOnly),
        )
        // "spa" must not match inside random tokens like "space" or "span"
        val spaceTrap = StreamLink(
            name = "Open Matte Space Opera 1080p",
            url = "https://cdn.example/x.mkv",
            isCached = true,
            isDirect = true,
        )
        check(
            "space_not_spanish",
            !LanguagePreference.isSpanishStream(spaceTrap) &&
                LanguagePreference.streamLanguageLabel(spaceTrap) != "Español",
        )

        // --- Real Latino / Spain ---
        val latino = StreamLink(
            name = "WEB-DL 1080p Latino Dual",
            url = "https://cdn.example/lat.mkv",
            quality = "1080p",
            language = "es-LAT",
            isCached = true,
            isPremium = true,
            isDirect = true,
            source = "Real-Debrid",
        )
        val spain = StreamLink(
            name = "Castellano 1080p",
            url = "https://cdn.example/es.mkv",
            quality = "1080p",
            language = "es-ES",
            isCached = true,
            isPremium = true,
            isDirect = true,
            source = "Real-Debrid",
        )
        check("label_latino", LanguagePreference.streamLanguageLabel(latino) == "Latino")
        check("label_spain", LanguagePreference.streamLanguageLabel(spain) == "España")
        check(
            "latino_boost_gt_english",
            LanguagePreference.streamLanguageBoost(latino) >
                LanguagePreference.streamLanguageBoost(engOnly),
        )
        check(
            "latino_boost_gt_spain",
            LanguagePreference.streamLanguageBoost(latino) >
                LanguagePreference.streamLanguageBoost(spain),
        )
        check(
            "sort_latino_first",
            StreamRanker.sort(listOf(engOnly, spain, latino)).first().url == latino.url,
        )

        // Bare "latin" without latino/latam should NOT auto-promote (strict patterns)
        val latinNoise = StreamLink(
            name = "The Latin Grammy Special 720p",
            url = "https://cdn.example/noise.mkv",
            isCached = true,
            isDirect = true,
        )
        check(
            "latin_grammy_not_latino_audio",
            LanguagePreference.classifyStreamLanguage(latinNoise) !=
                LanguagePreference.StreamLangKind.LATINO,
        )

        // Multi is multi, not Spanish
        val multi = StreamLink(
            name = "MULTi 1080p",
            url = "https://cdn.example/m.mkv",
            isCached = true,
            isDirect = true,
        )
        check(
            "multi_label",
            LanguagePreference.streamLanguageLabel(multi) == "Multi",
        )

        // Suspended filter: real host failures only
        val unsupportedHost = StreamLink(
            name = "hoster unsupported by Real-Debrid",
            url = "https://x/fail",
            isCached = true,
            isDirect = true,
            source = "Real-Debrid",
        )
        val torrentDead = StreamLink(
            name = "Some Movie",
            url = "magnet:?xt=urn:btih:1",
            source = "status: dead torrent",
        )
        check(
            "suspended_unsupported_out",
            StreamRanker.isSuspendedOrDead(unsupportedHost),
        )
        check(
            "suspended_torrent_dead_out",
            StreamRanker.isSuspendedOrDead(torrentDead),
        )
        check(
            "suspended_out_of_sort",
            StreamRanker.sort(listOf(unsupportedHost, latino)).none { it.url == unsupportedHost.url },
        )

        // Movie titles with dead/terror/error MUST stay playable (no bare-substring trap)
        fun rdCached(name: String, url: String) = StreamLink(
            name = name,
            url = url,
            quality = "1080p",
            isCached = true,
            isPremium = true,
            isDirect = true,
            source = "Real-Debrid",
        )
        val deadpool = rdCached("Deadpool 2016 1080p BluRay", "https://cdn.example/deadpool.mkv")
        val elTerror = rdCached("El Terror 1975 WEB-DL", "https://cdn.example/terror.mkv")
        val deadZone = rdCached("The Dead Zone 1983 720p", "https://cdn.example/deadzone.mkv")
        val errorMovie = rdCached("Error 404 2024", "https://cdn.example/error404.mkv")
        check("title_deadpool_kept", !StreamRanker.isSuspendedOrDead(deadpool))
        check("title_el_terror_kept", !StreamRanker.isSuspendedOrDead(elTerror))
        check("title_dead_zone_kept", !StreamRanker.isSuspendedOrDead(deadZone))
        check("title_error_404_kept", !StreamRanker.isSuspendedOrDead(errorMovie))
        check(
            "title_deadpool_in_sort",
            StreamRanker.sort(listOf(deadpool, unsupportedHost)).any { it.url == deadpool.url },
        )
        check(
            "title_deadpool_pickable",
            StreamRanker.pickBest(listOf(deadpool))?.url == deadpool.url,
        )

        // Update version pure logic
        val newer = UpdateVersionLogic.ReleaseInfo(
            tagName = "v1.4.0",
            name = "Zakra TV v1.4.0",
            body = "versionCode: 5",
            assets = listOf(
                UpdateVersionLogic.AssetInfo(
                    "ZakraTV.apk",
                    "https://github.com/zqkra/ZakraTV/releases/download/v1.4.0/ZakraTV.apk",
                ),
            ),
        )
        check(
            "update_detects_newer",
            UpdateVersionLogic.evaluateRelease(newer, 4, "1.3.0") != null,
        )
        check(
            "update_same_ignored",
            UpdateVersionLogic.evaluateRelease(newer, 5, "1.4.0") == null,
        )

        if (failed > 0) {
            System.err.println("$failed checks failed")
            kotlin.system.exitProcess(1)
        }
        println("ALL_PASS language+update")
    }
}
