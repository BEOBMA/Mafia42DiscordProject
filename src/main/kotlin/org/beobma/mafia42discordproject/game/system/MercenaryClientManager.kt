package org.beobma.mafia42discordproject.game.system

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.list.Mercenary

object MercenaryClientManager {
    fun assignRandomClient(game: Game, mercenaryPlayer: PlayerData): PlayerData? {
        val mercenary = mercenaryPlayer.job as? Mercenary ?: return null
        val client = game.playerDatas
            .filter { candidate -> isEligibleClient(candidate, mercenaryPlayer) }
            .randomOrNull()

        mercenary.clientPlayerId = client?.member?.id
        return client
    }

    fun isEligibleClient(candidate: PlayerData, mercenaryPlayer: PlayerData): Boolean {
        return isEligibleClient(
            job = candidate.job,
            isDead = candidate.state.isDead,
            isSelf = candidate.member.id == mercenaryPlayer.member.id,
            isFrogCursed = FrogCurseManager.isCursed(candidate),
            hallucinatedAsMafia = candidate.state.isFrogHallucinatedAsMafia
        )
    }

    fun isEligibleClient(
        job: Job?,
        isDead: Boolean,
        isSelf: Boolean,
        isFrogCursed: Boolean = false,
        hallucinatedAsMafia: Boolean = false
    ): Boolean {
        return !isDead &&
            !isSelf &&
            InvestigationTeam.of(
                job = job,
                isFrogCursed = isFrogCursed,
                hallucinatedAsMafia = hallucinatedAsMafia
            ) == InvestigationTeam.CITIZEN
    }
}
