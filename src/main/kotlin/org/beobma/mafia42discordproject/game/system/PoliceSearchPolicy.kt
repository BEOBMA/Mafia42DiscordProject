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
}
