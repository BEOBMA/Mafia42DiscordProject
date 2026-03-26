package org.beobma.mafia42discordproject.job.ability.general.definition.list.doctor

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Doctor
import kotlin.reflect.KClass

class Calm : Ability, JobSpecificExtraAbility {
    override val name: String = "진정"
    override val description: String = "치료대상으로 지목한 플레이어의 해로운 효과를 해제시킨다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(168).webp"
    override val targetJob: List<KClass<out Job>> = listOf(Doctor::class)
}