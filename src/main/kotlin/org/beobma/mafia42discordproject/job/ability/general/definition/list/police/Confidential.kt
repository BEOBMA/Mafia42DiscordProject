package org.beobma.mafia42discordproject.job.ability.general.definition.list.police

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Police
import kotlin.reflect.KClass

class Confidential : Ability, JobSpecificExtraAbility {
    override val name: String = "기밀"
    override val description: String = "두 번째 밤 시작 시에 다른 플레이어 중 한 명을 조사한다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(183).webp"
    override val targetJob: List<KClass<out Job>> = listOf(Police::class)
}