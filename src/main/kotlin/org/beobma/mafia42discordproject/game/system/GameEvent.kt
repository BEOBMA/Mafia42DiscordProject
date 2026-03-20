package org.beobma.mafia42discordproject.game.system

import org.beobma.mafia42discordproject.game.DawnPresentation
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.job.Job

sealed class GameEvent {
    data class PlayerHealed(
        val healer: PlayerData,
        val target: PlayerData,
        var defenseTier: DefenseTier
    ) : GameEvent()

    data class HealSucceeded(
        val healer: PlayerData,
        val target: PlayerData,
        val blockedAttack: AttackEvent
    ) : GameEvent()

    data class PlayerDied(
        val victim: PlayerData,
        val isLynch: Boolean = false,
        var isCleanedUp: Boolean = false
    ) : GameEvent()

    data class JobDiscovered(
        val discoverer: PlayerData,
        val target: PlayerData,
        val actualJob: Job,
        var revealedJob: Job,
        val sourceAbilityName: String? = null,
        val resolvedAt: DiscoveryStep,
        var isFalsified: Boolean = false,
        var note: String? = null,
        val sharedByPaparazzi: Boolean = false,
        val triggeredByTact: Boolean = false
    ) : GameEvent()


    data class CalculateVoteWeight(
        val voter: PlayerData,
        var weight: Int = 1
    ) : GameEvent()

    data class DecideExecution(
        val target: PlayerData,
        var isApproved: Boolean,
        var overrideReason: String? = null
    ) : GameEvent()

    data class VoteExecution(
        val target: PlayerData,
        var isCancelled: Boolean = false,
        var cancelReason: String? = null
    ) : GameEvent()

    data class ResolveDawnPresentation(
        val dayCount: Int,
        val attacks: List<AttackEvent>,
        val deaths: List<PlayerData>,
        var presentation: DawnPresentation
    ) : GameEvent()
}
