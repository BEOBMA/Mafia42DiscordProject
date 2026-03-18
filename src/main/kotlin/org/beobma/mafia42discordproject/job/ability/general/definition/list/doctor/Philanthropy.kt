package org.beobma.mafia42discordproject.job.ability.general.definition.list.doctor

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Doctor
import kotlin.reflect.KClass

class Philanthropy : Ability, JobSpecificExtraAbility {
    override val name: String = "박애"
    override val description: String = "경찰계열 직업과 의사를 제외한 플레이어를 치료할 경우 모든 부가 능력을 무시하고 성공한다."
    override val targetJob: List<KClass<out Job>> = listOf(Doctor::class)
}