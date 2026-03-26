package org.beobma.mafia42discordproject.job.ability.general.definition.list.politician

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Politician
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import kotlin.reflect.KClass

class Dictatorship : Ability, JobSpecificExtraAbility {
    override val name: String = "독재"
    override val description: String = "같은 팀 플레이어가 모두 사망하였을 경우, 모든 투표는 정치인에 의해서 결정된다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(135).webp"
    override val targetJob: List<KClass<out Job>> = listOf(Politician::class)
}