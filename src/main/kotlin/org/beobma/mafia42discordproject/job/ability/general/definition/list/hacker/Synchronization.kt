package org.beobma.mafia42discordproject.job.ability.general.definition.list.hacker

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Hacker
import kotlin.reflect.KClass

class Synchronization : Ability, JobSpecificExtraAbility {
    override val name: String = "동기화"
    override val description: String = "해킹당한 대상이 시민 팀일 경우, 해커의 존재를 대상에게 알려준다."
    override val targetJob: List<KClass<out Job>> = listOf(Hacker::class)
}