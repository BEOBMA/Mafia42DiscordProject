package org.beobma.mafia42discordproject.game.system

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HackerRedirectManagerTest {
    @Test
    fun `selection preserves the hacker while resolving the effective target`() {
        val hacker = ProxyPlayer(id = 1, isDead = false, proxyTargetId = 2)
        val target = ProxyPlayer(id = 2, isDead = false)
        val players = listOf(hacker, target).associateBy(ProxyPlayer::id)

        val selection = resolveSelection(hacker, players)

        assertEquals(hacker, selection.selectedTarget)
        assertEquals(target, selection.effectiveTarget)
    }

    @Test
    fun `dead hacker still redirects abilities to a living proxy target`() {
        val hacker = ProxyPlayer(id = 1, isDead = true, proxyTargetId = 2)
        val target = ProxyPlayer(id = 2, isDead = false)
        val players = listOf(hacker, target).associateBy(ProxyPlayer::id)

        val resolved = resolve(hacker, players)

        assertEquals(target, resolved)
        assertFalse(hacker.wasProxyCleared)
    }

    @Test
    fun `proxy is cleared when the hacked target is dead`() {
        val hacker = ProxyPlayer(id = 1, isDead = true, proxyTargetId = 2)
        val target = ProxyPlayer(id = 2, isDead = true)
        val players = listOf(hacker, target).associateBy(ProxyPlayer::id)

        val resolved = resolve(hacker, players)

        assertEquals(hacker, resolved)
        assertTrue(hacker.wasProxyCleared)
        assertEquals(null, hacker.proxyTargetId)
    }

    @Test
    fun `proxy chains resolve to the final living target`() {
        val firstHacker = ProxyPlayer(id = 1, isDead = true, proxyTargetId = 2)
        val secondHacker = ProxyPlayer(id = 2, isDead = false, proxyTargetId = 3)
        val target = ProxyPlayer(id = 3, isDead = false)
        val players = listOf(firstHacker, secondHacker, target).associateBy(ProxyPlayer::id)

        assertEquals(target, resolve(firstHacker, players))
    }

    private fun resolve(
        originalTarget: ProxyPlayer,
        players: Map<Int, ProxyPlayer>
    ): ProxyPlayer? {
        return resolveSelection(originalTarget, players).effectiveTarget
    }

    private fun resolveSelection(
        selectedTarget: ProxyPlayer,
        players: Map<Int, ProxyPlayer>
    ): HackerTargetSelection<ProxyPlayer> {
        return HackerRedirectManager.resolveTargetSelectionChain(
            selectedTarget = selectedTarget,
            playerId = ProxyPlayer::id,
            proxyTargetId = ProxyPlayer::proxyTargetId,
            findPlayer = players::get,
            isProxySuppressed = ProxyPlayer::isProxySuppressed,
            isDead = ProxyPlayer::isDead,
            clearProxy = { player ->
                player.proxyTargetId = null
                player.wasProxyCleared = true
            }
        )
    }

    private data class ProxyPlayer(
        val id: Int,
        val isDead: Boolean,
        var proxyTargetId: Int? = null,
        val isProxySuppressed: Boolean = false,
        var wasProxyCleared: Boolean = false
    )
}
