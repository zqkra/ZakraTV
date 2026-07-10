package com.zakratv.app.data.ranking

import com.zakratv.app.data.model.MediaItem
import com.zakratv.app.data.model.StreamLink

/**
 * Strict Spanish-first ranking. Avoids false positives like "spa" inside unrelated words
 * or labeling English-only streams as Spanish.
 */
object LanguagePreference {

    val SPANISH_LANG_CODES: Set<String> = setOf(
        "es", "es-es", "es-mx", "es-ar", "es-lat", "es-la", "es-us", "es-cl", "es-co", "spa",
        "es-419", "es-lat", "lat", "latino",
    )

    /**
     * Flag emojis that Real-Debrid / Torrentio put on releases. Latin-American flags
     * are the strongest Latino signal ("por su mayoría en RD le ponen una bandera de MX").
     */
    val LATINO_FLAGS: Set<String> = setOf(
        "🇲🇽", // MX México
        "🇦🇷", // AR Argentina
        "🇨🇴", // CO Colombia
        "🇨🇱", // CL Chile
        "🇵🇪", // PE Perú
        "🇻🇪", // VE Venezuela
        "🇪🇨", // EC Ecuador
        "🇬🇹", // GT Guatemala
        "🇨🇺", // CU Cuba
        "🇧🇴", // BO Bolivia
        "🇩🇴", // DO Rep. Dominicana
        "🇭🇳", // HN Honduras
        "🇵🇾", // PY Paraguay
        "🇸🇻", // SV El Salvador
        "🇳🇮", // NI Nicaragua
        "🇨🇷", // CR Costa Rica
        "🇵🇦", // PA Panamá
        "🇺🇾", // UY Uruguay
        "🇵🇷", // PR Puerto Rico
    )

    /** Spain flag. */
    const val SPAIN_FLAG: String = "🇪🇸" // ES España

    /** Whole-token / explicit Latino audio signals only. */
    private val LATINO_PATTERNS = listOf(
        Regex("""\blatino\b""", RegexOption.IGNORE_CASE),
        Regex("""\blatin\b""", RegexOption.IGNORE_CASE),
        Regex("""\blatam\b""", RegexOption.IGNORE_CASE),
        Regex("""\bes[-_]?419\b""", RegexOption.IGNORE_CASE),
        Regex("""\bes[-_]?mx\b""", RegexOption.IGNORE_CASE),
        Regex("""\bes[-_]?la\b""", RegexOption.IGNORE_CASE),
        Regex("""\bes[-_]?lat\b""", RegexOption.IGNORE_CASE),
        Regex("""\bes[-_]?ar\b""", RegexOption.IGNORE_CASE),
        // Bare "LAT" tag common in release names: "1080p LAT", "Dual.LAT", "[LAT]"
        Regex("""[\s.\-_\[(]lat[\s.\-_\])]""", RegexOption.IGNORE_CASE),
        Regex("""\bdual\s*lat""", RegexOption.IGNORE_CASE),
        Regex("""\bdoblaje\s*lat""", RegexOption.IGNORE_CASE),
        Regex("""\baudio\s*lat""", RegexOption.IGNORE_CASE),
        Regex("""\bespañol\s*latino\b""", RegexOption.IGNORE_CASE),
        Regex("""\bespanol\s*latino\b""", RegexOption.IGNORE_CASE),
        Regex("""\bspanish\s*latin""", RegexOption.IGNORE_CASE),
        Regex("""\blatin\s*spanish\b""", RegexOption.IGNORE_CASE),
        Regex("""\bmexican\b""", RegexOption.IGNORE_CASE),
        Regex("""\bmexico\b""", RegexOption.IGNORE_CASE),
        Regex("""\bméxico\b""", RegexOption.IGNORE_CASE),
    )

    private val SPAIN_PATTERNS = listOf(
        Regex("""\bcastellano\b""", RegexOption.IGNORE_CASE),
        Regex("""\bcast\b""", RegexOption.IGNORE_CASE),
        Regex("""\bes[-_]?es\b""", RegexOption.IGNORE_CASE),
        Regex("""\bespaña\b""", RegexOption.IGNORE_CASE),
        Regex("""\bespana\b""", RegexOption.IGNORE_CASE),
        Regex("""\beuropean\s*spanish\b""", RegexOption.IGNORE_CASE),
        Regex("""\bspanish\s*spain\b""", RegexOption.IGNORE_CASE),
    )

    private val SPANISH_GENERIC_PATTERNS = listOf(
        Regex("""\bspanish\b""", RegexOption.IGNORE_CASE),
        Regex("""\bespañol\b""", RegexOption.IGNORE_CASE),
        Regex("""\bespanol\b""", RegexOption.IGNORE_CASE),
        Regex("""\bsub(?:s|titles?)?\s*es\b""", RegexOption.IGNORE_CASE),
        Regex("""\bsub\s*español\b""", RegexOption.IGNORE_CASE),
        Regex("""\bsub\s*espanol\b""", RegexOption.IGNORE_CASE),
        Regex("""\baudio\s*es\b""", RegexOption.IGNORE_CASE),
        Regex("""\besp\b""", RegexOption.IGNORE_CASE),
    )

    private val ENGLISH_PATTERNS = listOf(
        Regex("""\benglish\b""", RegexOption.IGNORE_CASE),
        Regex("""\binglés\b""", RegexOption.IGNORE_CASE),
        Regex("""\bingles\b""", RegexOption.IGNORE_CASE),
        Regex("""\ben[-_]?us\b""", RegexOption.IGNORE_CASE),
        Regex("""\ben[-_]?gb\b""", RegexOption.IGNORE_CASE),
        Regex("""\beng\b""", RegexOption.IGNORE_CASE),
    )

    private val MULTI_PATTERNS = listOf(
        Regex("""\bmulti\b""", RegexOption.IGNORE_CASE),
        Regex("""\bdual\b""", RegexOption.IGNORE_CASE),
        Regex("""\btruefrench\b""", RegexOption.IGNORE_CASE),
    )

    enum class StreamLangKind {
        LATINO, SPAIN, SPANISH_GENERIC, MULTI, ENGLISH, OTHER, UNKNOWN
    }

    fun languageScore(originalLanguage: String?, title: String = "", overview: String = ""): Int {
        val lang = originalLanguage?.trim()?.lowercase().orEmpty()
        if (lang in SPANISH_LANG_CODES || lang == "es" || lang.startsWith("es-")) return 100
        val haystack = "$title $overview"
        return when {
            LATINO_PATTERNS.any { it.containsMatchIn(haystack) } -> 50
            SPAIN_PATTERNS.any { it.containsMatchIn(haystack) } -> 45
            SPANISH_GENERIC_PATTERNS.any { it.containsMatchIn(haystack) } -> 40
            lang == "en" -> 10
            lang.isEmpty() || lang == "xx" || lang == "und" -> 5
            else -> 0
        }
    }

    fun compareMediaSpanishFirst(a: MediaItem, b: MediaItem): Int {
        val scoreDiff = languageScore(b.originalLanguage, b.title, b.overview) -
            languageScore(a.originalLanguage, a.title, a.overview)
        if (scoreDiff != 0) return scoreDiff
        return b.popularity.compareTo(a.popularity)
    }

    fun sortMediaSpanishFirst(items: List<MediaItem>): List<MediaItem> =
        items.sortedWith(::compareMediaSpanishFirst)

    fun classifyStreamLanguage(link: StreamLink): StreamLangKind {
        val text = listOf(link.name, link.quality, link.language, link.source)
            .joinToString(" ")
        val lang = link.language.trim().lowercase()

        // Flag emojis are the strongest real-world signal on RD/Torrentio releases.
        if (LATINO_FLAGS.any { text.contains(it) }) return StreamLangKind.LATINO
        if (text.contains(SPAIN_FLAG)) return StreamLangKind.SPAIN

        // Explicit language field next (most reliable from our extractors)
        when {
            lang in setOf("es-lat", "es-la", "es-mx", "es-ar", "es-latam", "es-419", "lat", "latino") ||
                (lang.contains("lat") && lang.startsWith("es")) -> return StreamLangKind.LATINO
            lang == "es-es" || lang.contains("castell") -> return StreamLangKind.SPAIN
            lang in SPANISH_LANG_CODES || lang == "es" || lang.startsWith("es-") ->
                return StreamLangKind.SPANISH_GENERIC
            lang.startsWith("en") -> return StreamLangKind.ENGLISH
        }

        val isLatino = LATINO_PATTERNS.any { it.containsMatchIn(text) }
        val isSpain = SPAIN_PATTERNS.any { it.containsMatchIn(text) }
        val isSpanish = SPANISH_GENERIC_PATTERNS.any { it.containsMatchIn(text) }
        val isEnglish = ENGLISH_PATTERNS.any { it.containsMatchIn(text) }
        val isMulti = MULTI_PATTERNS.any { it.containsMatchIn(text) }

        // English-only stream must NOT become Spanish
        if (isEnglish && !isLatino && !isSpain && !isSpanish) return StreamLangKind.ENGLISH
        if (isLatino) return StreamLangKind.LATINO
        if (isSpain) return StreamLangKind.SPAIN
        if (isSpanish) return StreamLangKind.SPANISH_GENERIC
        if (isMulti) return StreamLangKind.MULTI
        if (isEnglish) return StreamLangKind.ENGLISH
        if (text.isBlank() && lang.isBlank()) return StreamLangKind.UNKNOWN
        return StreamLangKind.OTHER
    }

    /**
     * Boost for ranking: Latino > Spain > Spanish > Multi > English.
     * English-only gets small boost only, never Spanish-tier.
     */
    fun streamLanguageBoost(link: StreamLink): Int = when (classifyStreamLanguage(link)) {
        StreamLangKind.LATINO -> 80
        StreamLangKind.SPAIN -> 55
        StreamLangKind.SPANISH_GENERIC -> 40
        StreamLangKind.MULTI -> 15
        StreamLangKind.ENGLISH -> 5
        StreamLangKind.OTHER -> 0
        StreamLangKind.UNKNOWN -> 0
    }

    /**
     * Primary ranking tier by language: Latino must come first, always.
     * Used as the dominant sort key so a Latino stream ranks above English/Russian/other,
     * regardless of cache status. Other/Unknown (e.g. Russian, French) sink to the bottom.
     */
    fun streamLanguageRank(link: StreamLink): Int = when (classifyStreamLanguage(link)) {
        StreamLangKind.LATINO -> 6
        StreamLangKind.SPAIN -> 5
        StreamLangKind.SPANISH_GENERIC -> 4
        StreamLangKind.MULTI -> 3
        StreamLangKind.ENGLISH -> 1
        StreamLangKind.OTHER -> 0
        StreamLangKind.UNKNOWN -> 0
    }

    /** Plain Spanish UI labels (no emoji — Fire Stick safe). */
    fun streamLanguageLabel(link: StreamLink): String = when (classifyStreamLanguage(link)) {
        StreamLangKind.LATINO -> "Latino"
        StreamLangKind.SPAIN -> "España"
        StreamLangKind.SPANISH_GENERIC -> "Español"
        StreamLangKind.MULTI -> "Multi"
        StreamLangKind.ENGLISH -> "Inglés"
        StreamLangKind.OTHER -> link.language.takeIf { it.isNotBlank() }?.uppercase().orEmpty()
        StreamLangKind.UNKNOWN -> ""
    }

    /** True only when we have positive Spanish audio/sub evidence. */
    fun isSpanishStream(link: StreamLink): Boolean = when (classifyStreamLanguage(link)) {
        StreamLangKind.LATINO, StreamLangKind.SPAIN, StreamLangKind.SPANISH_GENERIC -> true
        else -> false
    }

    const val TMDB_LANGUAGE = "es-MX"
    const val TMDB_LANGUAGE_FALLBACK = "es-ES"
}
