package org.beobma.mafia42discordproject.job.ability

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData

interface ActiveAbility : Ability {
    val usablePhase: GamePhase // 능력 발동 시점
    fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult
}