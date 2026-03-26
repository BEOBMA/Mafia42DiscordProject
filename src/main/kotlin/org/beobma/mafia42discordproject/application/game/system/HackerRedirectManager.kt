package org.beobma.mafia42discordproject.game.system

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.job.definition.list.Hacker

object HackerRedirectManager {
    fun resolveTarget(game: Game, originalTarget: PlayerData?): PlayerData? {
        var current = originalTarget ?: return null
        if (current.state.isDead) return current

        val visitedIds = mutableSetOf(current.member.id)
        while (true) {
            val hacker = current.job as? Hacker ?: return current
            val hackedTargetId = hacker.hackedTargetId ?: return current
            val hackedTarget = game.getPlayer(hackedTargetId) ?: return current
            if (hackedTarget.state.isDead) return current
            if (!visitedIds.add(hackedTarget.member.id)) return current
            current = hackedTarget
        }
    }
}
