package com.zakratv.app.data.remote

import com.zakratv.app.data.model.StreamLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Golden rule: only 404/410/451 confirm a removed torrent. Anything ambiguous
 * (timeouts, 403, 5xx, redirects) must KEEP the link — never drop one that works.
 */
class LinkHealthTest {

    private fun link(name: String) = StreamLink(name = name, url = "https://cdn.example/$name.mkv")

    @Test
    fun onlyRemovalCodesCountAsDead() {
        assertTrue(LinkHealth.isRemovedCode(404))
        assertTrue(LinkHealth.isRemovedCode(410))
        assertTrue(LinkHealth.isRemovedCode(451))
        // Ambiguous or alive codes must never kill a link that might still play.
        assertFalse(LinkHealth.isRemovedCode(200))
        assertFalse(LinkHealth.isRemovedCode(206))
        assertFalse(LinkHealth.isRemovedCode(302))
        assertFalse(LinkHealth.isRemovedCode(403))
        assertFalse(LinkHealth.isRemovedCode(429))
        assertFalse(LinkHealth.isRemovedCode(500))
        assertFalse(LinkHealth.isRemovedCode(503))
    }

    @Test
    fun dropsOnlyConfirmedDeadAndKeepsOrder() {
        val vivo1 = link("vivo1")
        val muerto = link("muerto")
        val vivo2 = link("vivo2")
        val fueraDelTop = link("fuera-del-top")
        val out = LinkHealth.dropConfirmedDead(
            listOf(vivo1, muerto, vivo2, fueraDelTop),
            topN = 3,
        ) { it == muerto }
        assertEquals(listOf(vivo1, vivo2, fueraDelTop), out)
    }

    @Test
    fun uncertaintyKeepsEverything() {
        val links = listOf(link("a"), link("b"), link("c"))
        // Checker that never confirms death (e.g. all probes timed out) → nothing dropped.
        val out = LinkHealth.dropConfirmedDead(links, topN = 3) { false }
        assertEquals(links, out)
    }

    @Test
    fun linksBeyondProbedTopAreAlwaysKept() {
        val links = (1..20).map { link("l$it") }
        // Even a checker that claims EVERYTHING is dead only affects the probed head.
        val out = LinkHealth.dropConfirmedDead(links, topN = 15) { true }
        assertEquals(links.drop(15), out)
        assertEquals(5, out.size)
    }
}
