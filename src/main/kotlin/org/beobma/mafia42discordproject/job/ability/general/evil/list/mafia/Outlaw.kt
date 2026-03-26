package org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.AssistanceCommonAbility
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import kotlin.reflect.KClass

class Outlaw : Ability, JobSpecificExtraAbility {
    override val name: String = "무법자"
    override val description: String = "경찰계열 직업을 처형 대상으로 지목할 경우 무조건 처형시킨다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(172).webp"
    override val targetJob: List<KClass<out Job>> = listOf(Mafia::class)
}