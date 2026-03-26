package org.beobma.mafia42discordproject.job.ability.general.definition.list.doctor

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Doctor
import kotlin.reflect.KClass

class Calm : Ability, JobSpecificExtraAbility {
    override val name: String = "진정"
    override val description: String = "치료대상으로 지목한 플레이어의 해로운 효과를 해제시킨다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484605942694936697/055c98e5c6f40d6b.png?ex=69bed687&is=69bd8507&hm=c6b782ea070e0ce210e71b304b3bd87054661cfe8389ee1776c73e33f4320700&"
    override val targetJob: List<KClass<out Job>> = listOf(Doctor::class)
}