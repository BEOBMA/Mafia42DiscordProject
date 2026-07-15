package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.CommonAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.definition.Definition

class MentalPatient : Job(), Definition {
    override val name: String = "정신병자"
    override val description: String =
        "[혼란] 해당 직업에 배정된 후 게임에 시작되면 무작위 특수 직업 중 하나로 배정된 것으로 표기되고, 이후 해당 직업의 능력들을 선택한다. 자신은 해당 직업의 능력을 모두 사용 가능하나, 실제로 해당 효과는 발동되지 않고 모두 무효로 처리된다. 이 사실은 자신이 알 수 없다."
    override val jobImage: String =
        "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(19).webp"
    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(Confusion())

    var displayedJob: Job? = null

    fun activeAbilitySourceAbilities(): List<Ability> {
        return (displayedJob?.abilities ?: emptyList()) + extraAbilities.filter(::isActuallyAvailableUtilityAbility)
    }

    fun abilitiesExposedToGameSystems(): List<Ability> {
        return abilities + extraAbilities.filter(::isActuallyAvailableUtilityAbility)
    }

    companion object {
        const val JOB_NAME: String = "정신병자"

        fun isActuallyAvailableUtilityAbility(ability: Ability): Boolean {
            return ability is CommonAbility
        }
    }
}

class Confusion : JobUniqueAbility {
    override val name: String = "혼란"
    override val description: String =
        "게임 시작 시 무작위 특수 직업으로 표시되고 해당 직업의 능력을 선택할 수 있으나, 직접적인 직업 효과는 실제로 발동되지 않는다."
    override val image: String =
        "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(19).webp"
}
