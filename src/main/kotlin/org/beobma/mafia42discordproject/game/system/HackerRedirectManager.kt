package org.beobma.mafia42discordproject.game.system

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.job.definition.list.Hacker
import org.beobma.mafia42discordproject.job.evil.list.Thief
import org.beobma.mafia42discordproject.job.evil.list.actualOrStolenJob

internal data class HackerTargetSelection<Player>(
    val selectedTarget: Player?,
    val effectiveTarget: Player?
)

object HackerRedirectManager {
    fun resolveTarget(game: Game, originalTarget: PlayerData?): PlayerData? {
        return resolveSelection(game, originalTarget).effectiveTarget
    }

    internal fun resolveSelection(game: Game, selectedTarget: PlayerData?): HackerTargetSelection<PlayerData> {
        return resolveTargetSelectionChain(
            selectedTarget = selectedTarget,
            playerId = { it.member.id },
            proxyTargetId = { player ->
                (player.job as? Thief)?.stolenHackerTargetId
                    ?: player.actualOrStolenJob<Hacker>()?.hackedTargetId
            },
            findPlayer = game::getPlayer,
            isProxySuppressed = FrogCurseManager::shouldSuppressPassive,
            isDead = { it.state.isDead },
            clearProxy = ::clearProxy
        )
    }

    internal fun <Player, PlayerId> resolveTargetSelectionChain(
        selectedTarget: Player?,
        playerId: (Player) -> PlayerId,
        proxyTargetId: (Player) -> PlayerId?,
        findPlayer: (PlayerId) -> Player?,
        isProxySuppressed: (Player) -> Boolean,
        isDead: (Player) -> Boolean,
        clearProxy: (Player) -> Unit
    ): HackerTargetSelection<Player> {
        return HackerTargetSelection(
            selectedTarget = selectedTarget,
            effectiveTarget = resolveTargetChain(
                originalTarget = selectedTarget,
                playerId = playerId,
                proxyTargetId = proxyTargetId,
                findPlayer = findPlayer,
                isProxySuppressed = isProxySuppressed,
                isDead = isDead,
                clearProxy = clearProxy
            )
        )
    }

    internal fun <Player, PlayerId> resolveTargetChain(
        originalTarget: Player?,
        playerId: (Player) -> PlayerId,
        proxyTargetId: (Player) -> PlayerId?,
        findPlayer: (PlayerId) -> Player?,
        isProxySuppressed: (Player) -> Boolean,
        isDead: (Player) -> Boolean,
        clearProxy: (Player) -> Unit
    ): Player? {
        var current = originalTarget ?: return null
        val visitedPlayerIds = mutableSetOf(playerId(current))
        while (true) {
            val hackedTargetId = proxyTargetId(current) ?: return current
            if (isProxySuppressed(current)) return current

            val hackedTarget = findPlayer(hackedTargetId)
            if (hackedTarget == null || isDead(hackedTarget)) {
                clearProxy(current)
                return current
            }
            if (!visitedPlayerIds.add(playerId(hackedTarget))) return current
            current = hackedTarget
        }
    }

    fun releaseProxiesTargeting(game: Game, deadTarget: PlayerData) {
        game.playerDatas.forEach { proxyOwner ->
            val thief = proxyOwner.job as? Thief
            val hacker = proxyOwner.actualOrStolenJob<Hacker>()
            if (
                thief?.stolenHackerTargetId == deadTarget.member.id ||
                hacker?.hackedTargetId == deadTarget.member.id
            ) {
                clearProxy(proxyOwner)
            }
        }
    }

    private fun clearProxy(proxyOwner: PlayerData) {
        (proxyOwner.job as? Thief)?.stolenHackerTargetId = null
        proxyOwner.actualOrStolenJob<Hacker>()?.hackedTargetId = null
    }
}
