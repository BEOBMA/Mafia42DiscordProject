package org.beobma.mafia42discordproject.job.ability.general.definition.list.doctor

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Doctor
import kotlin.reflect.KClass

class Screening : Ability, JobSpecificExtraAbility {
    override val name: String = "검진"
    override val description: String = "치료한 플레이어의 직업을 알아낸다."
    override val targetJob: List<KClass<out Job>> = listOf(Doctor::class)
}