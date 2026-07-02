package org.beobma.mafia42discordproject.job.ability.general.definition.list.police

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Police
import kotlin.reflect.KClass

class Confidential : Ability, JobSpecificExtraAbility {
    override val name: String = "기밀"
    override val description: String = "조사를 한 번 한 상태로 게임을 시작한다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/police_confidential.webp"
    override val targetJob: List<KClass<out Job>> = listOf(Police::class)
}