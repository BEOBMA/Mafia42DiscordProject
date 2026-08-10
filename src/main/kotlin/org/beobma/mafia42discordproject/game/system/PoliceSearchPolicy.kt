package org.beobma.mafia42discordproject.game.system

import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.evil.list.Mafia

internal object PoliceSearchPolicy {
    fun isMafia(player: PlayerData): Boolean = isMafia(
        job = player.job,
        isFrogCursed = FrogCurseManager.isCursed(player),
        hallucinatedAsMafia = player.state.isFrogHallucinatedAsMafia
    )

    fun isMafia(
        job: Job?,
        isFrogCursed: Boolean = false,
        hallucinatedAsMafia: Boolean = false
    ): Boolean = when {
        isFrogCursed && hallucinatedAsMafia -> true
        isFrogCursed -> false
        else -> job is Mafia
    }

    fun shouldShareMafiaDiscovery(isMafia: Boolean): Boolean = isMafia

    fun toMafiaDiscovery(
        event: GameEvent.PoliceSearchResolved,
        resolvedAt: DiscoveryStep
    ): GameEvent.JobDiscovered? {
        if (!shouldShareMafiaDiscovery(event.isMafia)) return null
        val actualJob = event.target.job ?: return null

        return GameEvent.JobDiscovered(
            discoverer = event.police,
            target = event.target,
            actualJob = actualJob,
            revealedJob = Mafia(),
            sourceAbilityName = "수색",
            resolvedAt = resolvedAt,
            notifyTarget = false,
            revealsExactJob = false
        )
    }
}
