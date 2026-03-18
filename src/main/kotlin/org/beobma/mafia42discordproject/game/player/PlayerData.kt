package org.beobma.mafia42discordproject.game.player

import dev.kord.core.entity.Member
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability

data class PlayerData(
    val member: Member,
    var job: Job? = null,
    val extraAbilities: MutableList<Ability> = mutableListOf()
) {
    // 💡 합성(Composition)을 통해 상태 객체를 내부에 포함
    val state: PlayerState = PlayerState()

    val allAbilities: List<Ability>
        get() = (job?.uniqueAbilities ?: emptyList()) + extraAbilities
}