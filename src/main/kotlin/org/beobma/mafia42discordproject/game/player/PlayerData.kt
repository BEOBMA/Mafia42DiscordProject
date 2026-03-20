package org.beobma.mafia42discordproject.game.player

import dev.kord.core.entity.Member
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability

data class PlayerData(
    val member: Member,
    var job: Job? = null
) {
    val state: PlayerState = PlayerState()
    val allAbilities: List<Ability>
        get() = (job?.abilities ?: emptyList()) + (job?.extraAbilities ?: emptyList())
}
