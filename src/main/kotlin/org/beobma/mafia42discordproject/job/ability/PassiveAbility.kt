@file:Suppress("SameReturnValue", "SameReturnValue", "SameReturnValue", "SameReturnValue")

package org.beobma.mafia42discordproject.job.ability

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.game.player.PlayerData

@Suppress("SameReturnValue", "SameReturnValue", "SameReturnValue", "SameReturnValue")
interface PassiveAbility {
    val priority: Int get() = 0

    fun onPhaseChanged(game: Game, owner: PlayerData, newPhase: GamePhase) {}
    fun onEventObserved(game: Game, owner: PlayerData, event: GameEvent) {}
    fun onDeceasedChat(game: Game, owner: PlayerData, event: GameEvent) {}
}