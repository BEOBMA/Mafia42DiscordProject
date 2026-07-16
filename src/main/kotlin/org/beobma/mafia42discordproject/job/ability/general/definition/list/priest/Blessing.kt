package org.beobma.mafia42discordproject.job.ability.general.definition.list.priest

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Priest
import kotlin.reflect.KClass

class Blessing : Ability, JobSpecificExtraAbility {
    override val name: String = "축복"
    override val description: String = "하루 동안 ‘소생’ 능력 대상 플레이어를 대상으로 지목할 수 없게 한다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/priest_ability_blessing.webp"
    override val targetJob: List<KClass<out Job>> = listOf(Priest::class)
}