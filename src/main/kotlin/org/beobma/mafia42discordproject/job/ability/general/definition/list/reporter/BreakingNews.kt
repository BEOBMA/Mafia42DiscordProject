package org.beobma.mafia42discordproject.job.ability.general.definition.list.reporter

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Reporter
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import kotlin.reflect.KClass

class BreakingNews : Ability, JobSpecificExtraAbility {
    override val name: String = "속보"
    override val description: String = "취재 대상이 밤 동안 처형을 했을 경우, 엠바고를 무시하고 기사를 낸다."
    override val targetJob: List<KClass<out Job>> = listOf(Reporter::class)
}