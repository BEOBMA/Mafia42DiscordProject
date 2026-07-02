package org.beobma.mafia42discordproject.game.player

import dev.kord.core.entity.Member
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.definition.list.MentalPatient

data class PlayerData(
    val member: Member,
    var job: Job? = null
) {
    val state: PlayerState = PlayerState()
    val allAbilities: List<Ability>
        get() {
            val currentJob = job ?: return emptyList()
            if (currentJob is MentalPatient) {
                return currentJob.abilitiesExposedToGameSystems()
            }
            return currentJob.abilities + currentJob.extraAbilities
        }
}
