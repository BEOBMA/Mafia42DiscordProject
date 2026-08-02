package org.beobma.mafia42discordproject.game.system

import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.job.Job

object ProphetApostlePolicy {
    fun isCitizenTeamSurvivor(player: PlayerData): Boolean {
        return isCitizenTeamSurvivor(
            job = player.job,
            isDead = player.state.isDead,
            isFrogCursed = FrogCurseManager.isCursed(player),
            hallucinatedAsMafia = player.state.isFrogHallucinatedAsMafia
        )
    }

    fun isCitizenTeamSurvivor(
        job: Job?,
        isDead: Boolean,
        isFrogCursed: Boolean,
        hallucinatedAsMafia: Boolean = false
    ): Boolean {
        return !isDead &&
            InvestigationTeam.of(
                job = job,
                isFrogCursed = isFrogCursed,
                hallucinatedAsMafia = hallucinatedAsMafia
            ) == InvestigationTeam.CITIZEN
    }
}
