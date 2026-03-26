package org.beobma.mafia42discordproject.game.system

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.MafiaAbility
import org.beobma.mafia42discordproject.job.definition.list.Frog
import org.beobma.mafia42discordproject.job.evil.list.Mafia

object FrogCurseManager {
    fun isCursed(player: PlayerData): Boolean = player.state.isFrogCursed

    fun applyCurse(target: PlayerData, currentDay: Int) {
        target.state.isFrogCursed = true
        target.state.frogCurseExpiresAfterDay = currentDay
    }

    fun clearExpiredAtNightStart(game: Game) {
        game.playerDatas.forEach { player ->
            val expiresAfterDay = player.state.frogCurseExpiresAfterDay ?: return@forEach
            if (expiresAfterDay < game.dayCount) {
                player.state.isFrogCursed = false
                player.state.frogCurseExpiresAfterDay = null
            }
        }
    }

    fun displayedJob(target: PlayerData): Job? {
        val actualJob = target.job ?: return null
        if (isCursed(target)) return Frog()
        return SwindlerManager.disguisedJobOf(target) ?: actualJob
    }

    fun canUseActiveAbility(caster: PlayerData, ability: ActiveAbility): Boolean {
        if (!isCursed(caster)) return true
        return caster.job is Mafia && ability is MafiaAbility
    }

    fun shouldSuppressPassive(player: PlayerData): Boolean = isCursed(player)
}
