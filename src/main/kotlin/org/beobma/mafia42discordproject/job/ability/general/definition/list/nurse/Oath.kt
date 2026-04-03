package org.beobma.mafia42discordproject.job.ability.general.definition.list.nurse

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Nurse
import kotlin.reflect.KClass

class Oath : Ability, JobSpecificExtraAbility {
    override val name: String = "선서"
    override val description: String = "게임 시작 시 의사에게 간호사의 존재 여부를 알린다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(181).webp"
    override val targetJob: List<KClass<out Job>> = listOf(Nurse::class)
}
