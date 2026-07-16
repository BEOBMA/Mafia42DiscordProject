package org.beobma.mafia42discordproject.job.ability.general.definition.list.priest

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Priest
import kotlin.reflect.KClass

class Blessing : Ability, JobSpecificExtraAbility {
    override val name: String = "축복"
    override val description: String = "‘소생’ 능력 대상 플레이어가 부활한 순간부터 다음 날 밤이 끝날 때까지 능력 대상으로 지목할 수 없게 한다. 투표에는 적용되지 않는다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/priest_ability_blessing.webp"
    override val targetJob: List<KClass<out Job>> = listOf(Priest::class)
}
