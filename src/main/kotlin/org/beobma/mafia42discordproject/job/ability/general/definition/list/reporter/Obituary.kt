package org.beobma.mafia42discordproject.job.ability.general.definition.list.reporter

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Reporter
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import kotlin.reflect.KClass

class Obituary : Ability, JobSpecificExtraAbility {
    override val name: String = "부고"
    override val description: String = "취재 대상이 사망하더라도 취재에 실패하지 않는다."
    override val targetJob: List<KClass<out Job>> = listOf(Reporter::class)
}