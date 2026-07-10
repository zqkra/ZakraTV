package com.zakratv.app.data.ranking

import com.zakratv.app.data.model.MediaItem
import com.zakratv.app.data.model.MediaType
import com.zakratv.app.data.model.StreamLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exercises shipped LanguagePreference helpers (no re-implementation).
 */
class LanguagePreferenceTest {

    @Test
    fun spanishOriginalLanguageScoresHighest() {
        val es = LanguagePreference.languageScore("es", "El laberinto", "Una historia")
        val en = LanguagePreference.languageScore("en", "The Maze", "A story")
        val fr = LanguagePreference.languageScore("fr", "Le labyrinthe", "")
        assertTrue("es ($es) must beat en ($en)", es > en)
        assertTrue("es ($es) must beat fr ($fr)", es > fr)
        assertEquals(100, es)
    }

    @Test
    fun latinoHintInTitleBoostsScore() {
        val withHint = LanguagePreference.languageScore("en", "Película Latino 1080p", "")
        val plainEn = LanguagePreference.languageScore("en", "Movie English", "")
        assertTrue(withHint > plainEn)
    }

    @Test
    fun sortMediaSpanishFirstPutsSpanishAhead() {
        val items = listOf(
            MediaItem(1, MediaType.MOVIE, "English Hit", originalLanguage = "en", popularity = 99.0),
            MediaItem(2, MediaType.MOVIE, "Éxito Español", originalLanguage = "es", popularity = 10.0),
            MediaItem(3, MediaType.MOVIE, "Français", originalLanguage = "fr", popularity = 50.0),
        )
        val sorted = LanguagePreference.sortMediaSpanishFirst(items)
        assertEquals(2, sorted.first().id)
        assertEquals("es", sorted.first().originalLanguage)
    }

    @Test
    fun streamLanguageBoostDetectsSpanishLabels() {
        val es = StreamLink(
            name = "Audio Latino 1080p",
            url = "https://example.com/a.mkv",
            language = "es-LAT",
        )
        val en = StreamLink(
            name = "English 1080p",
            url = "https://example.com/b.mkv",
            language = "en",
        )
        assertTrue(
            LanguagePreference.streamLanguageBoost(es) >
                LanguagePreference.streamLanguageBoost(en)
        )
    }

    @Test
    fun tmdbLanguageDefaultIsSpanish() {
        assertTrue(LanguagePreference.TMDB_LANGUAGE.startsWith("es"))
    }

    @Test
    fun mexicoFlagIsDetectedAsLatino() {
        val flagged = StreamLink(
            name = "Pelicula.2024.1080p 🇲🇽",
            url = "https://example.com/a.mkv",
        )
        assertEquals(
            LanguagePreference.StreamLangKind.LATINO,
            LanguagePreference.classifyStreamLanguage(flagged),
        )
        assertEquals("Latino", LanguagePreference.streamLanguageLabel(flagged))
    }

    @Test
    fun spainFlagIsDetectedAsSpain() {
        val flagged = StreamLink(
            name = "Pelicula.2024.1080p 🇪🇸",
            url = "https://example.com/b.mkv",
        )
        assertEquals(
            LanguagePreference.StreamLangKind.SPAIN,
            LanguagePreference.classifyStreamLanguage(flagged),
        )
    }

    @Test
    fun realWorldTorrentioTitlesClassifyCorrectly() {
        fun kind(title: String) =
            LanguagePreference.classifyStreamLanguage(StreamLink(name = title, url = "https://x/y"))

        // Real Torrentio/RD release names captured live for tt0468569.
        assertEquals(
            LanguagePreference.StreamLangKind.LATINO,
            kind("Batman.The.Dark.Knight.2008.1080p.REMUX.ENG.And.ESP.LATINO.DTS-HD.Master.DDP5.1.MKV"),
        )
        assertEquals(
            LanguagePreference.StreamLangKind.LATINO,
            kind("The.dark.knight.2008.1080P-Dual-Lat 👤 13 💾 2.34 GB ⚙️ Cinecalidad Dual Audio"),
        )
        assertEquals(
            LanguagePreference.StreamLangKind.SPAIN,
            kind("El caballero oscuro [4K UHDremux][2160p][HDR10][AC3 5.1 Castellano]"),
        )
        // Explicit English marker → ENGLISH.
        assertEquals(
            LanguagePreference.StreamLangKind.ENGLISH,
            kind("The Dark Knight 2008 1080p BluRay English DTS x264-GROUP"),
        )
        // No language marker at all → OTHER (we must NOT guess Spanish; it sinks below Latino).
        assertEquals(
            LanguagePreference.StreamLangKind.OTHER,
            kind("The.Dark.Knight.2008.UHD.2160p.REMUX.Hybrid.HDR10.DV.DTS-HD.TrueHD.5.1.H265-KC"),
        )
    }

    @Test
    fun latinoOutranksEverythingElse() {
        val latino = StreamLink(name = "Dual LAT 🇲🇽", url = "https://x/a", language = "es-LAT")
        val english = StreamLink(name = "English", url = "https://x/b", language = "en")
        val russian = StreamLink(name = "Русский", url = "https://x/c", language = "ru")
        assertTrue(
            LanguagePreference.streamLanguageRank(latino) >
                LanguagePreference.streamLanguageRank(english),
        )
        assertTrue(
            LanguagePreference.streamLanguageRank(english) >
                LanguagePreference.streamLanguageRank(russian),
        )
    }
}
