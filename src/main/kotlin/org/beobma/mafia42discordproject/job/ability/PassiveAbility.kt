package org.beobma.mafia42discordproject.job.ability

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.game.player.PlayerData

interface PassiveAbility {
    val priority: Int get() = 0

    fun onPhaseChanged(game: Game, owner: PlayerData, newPhase: GamePhase) {}
    fun onTargeted(game: Game, owner: PlayerData, attacker: PlayerData, ability: ActiveAbility) {}
    fun onEventObserved(game: Game, owner: PlayerData, event: GameEvent) {}
}