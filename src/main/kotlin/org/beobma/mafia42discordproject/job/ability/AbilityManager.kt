package org.beobma.mafia42discordproject.job.ability

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition
import org.beobma.mafia42discordproject.job.evil.Evil

object AbilityManager {
    private val extraAbilityPool = mutableListOf<Ability>()

    fun register(ability: Ability) {
        extraAbilityPool.add(ability)
    }

    // 특정 직업이 획득 가능한 능력만 필터링
    fun getAvailableExtraAbilitiesFor(job: Job): List<Ability> {
        return extraAbilityPool.filter { ability ->
            when (ability) {
                is CommonAbility -> true
                is EvilCommonAbility -> job is Evil
                is CitizenCommonAbility -> job is Definition
                is JobSpecificExtraAbility -> ability.targetJob.isInstance(job)
                else -> false
            }
        }
    }
}