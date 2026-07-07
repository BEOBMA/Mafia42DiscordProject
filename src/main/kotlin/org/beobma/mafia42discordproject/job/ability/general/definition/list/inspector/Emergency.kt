package org.beobma.mafia42discordproject.job.ability.general.definition.list.inspector

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Inspector
import kotlin.reflect.KClass

class Emergency : Ability, JobSpecificExtraAbility {
    override val name: String = "긴급"
    override val description: String = "지목한 플레이어의 정보를 즉시 알아낸다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/Inspector_ability_2.webp"
    override val targetJob: List<KClass<out Job>> = listOf(Inspector::class)
}
