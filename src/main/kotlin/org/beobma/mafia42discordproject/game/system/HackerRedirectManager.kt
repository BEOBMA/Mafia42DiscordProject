package org.beobma.mafia42discordproject.game.system

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.job.definition.list.Hacker
import org.beobma.mafia42discordproject.job.evil.list.Thief
import org.beobma.mafia42discordproject.job.evil.list.actualOrStolenJob

object HackerRedirectManager {
    fun resolveTarget(game: Game, originalTarget: PlayerData?): PlayerData? {
        var current = originalTarget ?: return null
        if (current.state.isDead) return current

        val thiefTargetId = (current.job as? Thief)?.stolenHackerTargetId
        val hacker = current.actualOrStolenJob<Hacker>()
        if (hacker == null && thiefTargetId == null) return current
        if (FrogCurseManager.shouldSuppressPassive(current)) return current
        val hackedTargetId = thiefTargetId ?: hacker?.hackedTargetId ?: return current
        val hackedTarget = game.getPlayer(hackedTargetId) ?: return current
        return hackedTarget.takeUnless { it.state.isDead } ?: current
    }
}
