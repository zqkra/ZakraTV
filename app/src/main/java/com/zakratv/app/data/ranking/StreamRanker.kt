package com.zakratv.app.data.ranking

import com.zakratv.app.data.model.StreamLink

/**
 * Ranking: RD cached/premium first, Spanish Latino next.
 * Suspended/dead host detection uses token/phrase rules so movie titles
 * like Deadpool / El Terror / The Dead Zone are NOT dropped.
 */
object StreamRanker {

    private val QUALITY_ORDER = listOf(
        "2160p", "4k", "uhd", "1080p", "fhd", "720p", "hd", "480p", "360p", "sd",
    )

    /**
     * Unambiguous multi-word / long tokens (safe with word boundaries).
     * Short ambiguous words (dead, error, failed, …) require host/torrent context.
     */
    private val SUSPENDED_SAFE = listOf(
        Regex("""\bunsupported\b""", RegexOption.IGNORE_CASE),
        Regex("""\bnot\s+supported\b""", RegexOption.IGNORE_CASE),
        Regex("""\binfringing\b""", RegexOption.IGNORE_CASE),
        Regex("""\bnot\s+available\b""", RegexOption.IGNORE_CASE),
        Regex("""\bno\s+longer\b""", RegexOption.IGNORE_CASE),
        Regex("""\bhoster\s+offline\b""", RegexOption.IGNORE_CASE),
        Regex("""\bsuspended\b""", RegexOption.IGNORE_CASE),
        Regex("""\bpremiumize\s+only\b""", RegexOption.IGNORE_CASE),
        Regex("""\brddownloader\b""", RegexOption.IGNORE_CASE),
        Regex("""\[\s*(?:dead|error|failed|unsupported|banned)\s*]""", RegexOption.IGNORE_CASE),
    )

    /** Ambiguous status words only when near host/torrent/download context. */
    private val SUSPENDED_CONTEXT = listOf(
        Regex(
            """\b(?:host(?:er)?s?|torrent|link|download|status|magnet|real-?debrid|\brd\b)\b[\s\S]{0,48}\b(?:dead|failed|error|banned|expired|offline|removed|disabled|restricted|virus)\b""",
            RegexOption.IGNORE_CASE,
        ),
        Regex(
            """\b(?:dead|failed|error|banned|expired|offline|removed|disabled|restricted|virus)\b[\s\S]{0,48}\b(?:host(?:er)?s?|torrent|link|download|status|magnet|real-?debrid|\brd\b)\b""",
            RegexOption.IGNORE_CASE,
        ),
        Regex(
            """\b(?:dead|failed)\s+(?:torrent|link|host(?:er)?)\b""",
            RegexOption.IGNORE_CASE,
        ),
        Regex(
            """\bstatus\s*[:=]\s*(?:dead|error|failed|banned)\b""",
            RegexOption.IGNORE_CASE,
        ),
    )

    fun qualityScore(quality: String, name: String = ""): Int {
        val text = "$quality $name".lowercase()
        QUALITY_ORDER.forEachIndexed { index, token ->
            // word-ish boundary for short tokens like "hd"
            val re = if (token.length <= 3) {
                Regex("""(?<![a-z0-9])$token(?![a-z0-9])""", RegexOption.IGNORE_CASE)
            } else {
                Regex(Regex.escape(token), RegexOption.IGNORE_CASE)
            }
            if (re.containsMatchIn(text)) {
                return (QUALITY_ORDER.size - index) * 10
            }
        }
        return 0
    }

    fun isSuspendedOrDead(link: StreamLink): Boolean {
        // Full text for unambiguous phrases (unsupported, [dead], not available, …)
        val all = listOf(link.name, link.url, link.source, link.quality).joinToString(" ")
        if (SUSPENDED_SAFE.any { it.containsMatchIn(all) }) return true

        // Ambiguous words (dead/error/failed) ONLY against source+quality — never movie titles
        // (Deadpool, El Terror, Error 404 must remain playable).
        val statusBlob = listOf(link.source, link.quality).joinToString(" ")
        if (statusBlob.isNotBlank() && SUSPENDED_CONTEXT.any { it.containsMatchIn(statusBlob) }) {
            return true
        }
        return false
    }

    fun rankScore(link: StreamLink): Int {
        if (isSuspendedOrDead(link)) return -1_000_000
        var score = 0
        val http = isHttpUrl(link.url)

        // Magnets / non-resolved links can't play right now → always below any HTTP stream.
        if (!http) score -= 500_000

        // PRIMARY preference: language. Latino first, then España, Español, Multi, Inglés.
        // Russian / French / other unknown audio sinks to the bottom (rank 0).
        score += LanguagePreference.streamLanguageRank(link) * 10_000

        // SECONDARY: Real-Debrid cached / premium / direct HTTPS quality.
        if (link.isCached) score += 3_000
        if (link.isPremium) score += 1_500
        if (link.isDirect && http) score += 800
        if (http) score += 400
        score += qualityScore(link.quality, link.name)
        score += link.seeds.coerceAtMost(200)
        val src = (link.source + " " + link.name).lowercase()
        if (src.contains("real-debrid") || src.contains("realdebrid") || src.contains("[rd")) {
            score += 300
        }
        return score
    }

    fun compare(a: StreamLink, b: StreamLink): Int = rankScore(b) - rankScore(a)

    fun sort(links: List<StreamLink>): List<StreamLink> =
        links.filterNot { isSuspendedOrDead(it) }.sortedWith(::compare)

    fun pickBest(links: List<StreamLink>): StreamLink? = sort(links).firstOrNull()

    fun isHttpUrl(url: String): Boolean =
        url.startsWith("http://", ignoreCase = true) ||
            url.startsWith("https://", ignoreCase = true)

    fun isPlayableDirect(link: StreamLink): Boolean =
        !isSuspendedOrDead(link) &&
            isHttpUrl(link.url) &&
            (link.isDirect || link.isCached || link.isPremium)

    fun displayLabel(link: StreamLink, index: Int): String {
        val lang = LanguagePreference.streamLanguageLabel(link)
        val parts = mutableListOf<String>()
        if (link.isCached || link.isPremium) parts += "RD"
        if (link.quality.isNotBlank()) parts += link.quality
        if (lang.isNotBlank()) parts += lang
        if (link.size.isNotBlank()) parts += link.size
        val head = parts.joinToString(" · ").ifBlank { "Enlace" }
        val name = link.name
            .replace(Regex("""https?://\S+"""), "")
            .take(36)
            .trim()
        return if (index == 0) {
            "Reproducir · $head${if (name.isNotBlank()) " · $name" else ""}"
        } else {
            "$head${if (name.isNotBlank()) " · $name" else ""}"
        }.take(90)
    }
}
