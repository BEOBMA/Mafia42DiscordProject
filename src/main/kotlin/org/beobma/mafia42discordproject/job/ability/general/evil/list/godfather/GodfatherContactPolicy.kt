package org.beobma.mafia42discordproject.job.ability.general.evil.list.godfather

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.job.evil.list.Godfather
import org.beobma.mafia42discordproject.job.evil.list.Mafia

object GodfatherContactPolicy {
    fun canContactMafia(game: Game): Boolean {
        val aliveMafiaCount = game.playerDatas.count { !it.state.isDead && it.job is Mafia }
        return game.dayCount >= 3 || aliveMafiaCount == 0
    }

    fun canUseExecution(game: Game, player: PlayerData): Boolean {
        if (player.state.isDead) return false
        if (player.job !is Godfather) return false
        return player.state.hasContactedMafiaByInformant || canContactMafia(game)
    }
}
