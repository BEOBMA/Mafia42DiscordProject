package org.beobma.mafia42discordproject.game.system

import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.list.Frog
import org.beobma.mafia42discordproject.job.evil.Evil

enum class InvestigationTeam {
    CITIZEN,
    MAFIA,
    FROG,
    UNKNOWN;

    companion object {
        fun of(player: PlayerData): InvestigationTeam {
            return of(
                job = player.job,
                isFrogCursed = FrogCurseManager.isCursed(player),
                hallucinatedAsMafia = player.state.isFrogHallucinatedAsMafia
            )
        }

        fun of(
            job: Job?,
            isFrogCursed: Boolean = false,
            hallucinatedAsMafia: Boolean = false
        ): InvestigationTeam {
            return when {
                isFrogCursed && hallucinatedAsMafia -> MAFIA
                isFrogCursed || job is Frog -> FROG
                job is Evil -> MAFIA
                job != null -> CITIZEN
                else -> UNKNOWN
            }
        }

        fun isCitizen(player: PlayerData): Boolean = of(player) == CITIZEN

        fun isMafia(player: PlayerData): Boolean = of(player) == MAFIA
    }
}
