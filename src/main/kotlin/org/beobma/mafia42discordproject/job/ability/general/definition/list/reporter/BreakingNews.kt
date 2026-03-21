package org.beobma.mafia42discordproject.job.ability.general.definition.list.reporter

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Reporter
import kotlin.reflect.KClass

class BreakingNews : Ability, JobSpecificExtraAbility {
    override val name: String = "속보"
    override val description: String = "취재 대상이 밤 동안 처형을 했을 경우, 엠바고를 무시하고 기사를 낸다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484614429558706206/76311e24e49ae834.png?ex=69bede6e&is=69bd8cee&hm=5601545e8bd8aeff008043c7f1252eabf76edbcfdbdce828ff45375de1905161&"
    override val targetJob: List<KClass<out Job>> = listOf(Reporter::class)
}
