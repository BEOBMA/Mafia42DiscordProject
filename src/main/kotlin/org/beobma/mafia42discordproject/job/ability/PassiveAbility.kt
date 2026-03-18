package org.beobma.mafia42discordproject.job.ability

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.event.GameEvent
import org.beobma.mafia42discordproject.game.player.PlayerData

interface PassiveAbility {
    fun onPhaseChanged(game: Game, owner: PlayerData, newPhase: GamePhase)
    fun onTargeted(game: Game, owner: PlayerData, attacker: PlayerData, ability: ActiveAbility) {}
    fun onEventObserved(game: Game, owner: PlayerData, event: GameEvent) {}
}