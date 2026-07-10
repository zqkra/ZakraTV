package com.zakratv.app.data.remote

import com.zakratv.app.data.model.StreamLink

/**
 * Decides which links get dropped after the health probe.
 *
 * GOLDEN RULE: only links CONFIRMED as removed are dropped — HTTP 404 (not found),
 * 410 (gone) and 451 (removed for legal reasons). Timeouts, network errors, 403s,
 * 5xx or anything ambiguous KEEPS the link: never remove one that might still play.
 */
object LinkHealth {

    val REMOVED_HTTP_CODES: Set<Int> = setOf(404, 410, 451)

    fun isRemovedCode(code: Int): Boolean = code in REMOVED_HTTP_CODES

    /**
     * Drops from the first [topN] links only those the [isDead] checker confirms;
     * links beyond [topN] (not probed) are always kept untouched.
     */
    fun dropConfirmedDead(
        links: List<StreamLink>,
        topN: Int,
        isDead: (StreamLink) -> Boolean,
    ): List<StreamLink> {
        if (links.isEmpty()) return links
        val head = links.take(topN).filterNot(isDead)
        val tail = links.drop(topN)
        return head + tail
    }
}
