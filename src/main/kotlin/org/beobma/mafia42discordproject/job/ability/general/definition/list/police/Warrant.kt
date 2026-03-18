package org.beobma.mafia42discordproject.job.ability.general.definition.list.police

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Police
import kotlin.reflect.KClass

class Warrant : Ability, JobSpecificExtraAbility {
    override val name: String = "영장"
    override val description: String = "이미 조사했던 플레이어를 한 번 더 조사할 경우 직업을 알아낸다."
    override val targetJob: List<KClass<out Job>> = listOf(Police::class)
}