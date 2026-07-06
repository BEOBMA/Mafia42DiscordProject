package org.beobma.mafia42discordproject.job.ability.general.definition.list.reporter

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Reporter
import kotlin.reflect.KClass

class BreakingNews : Ability, JobSpecificExtraAbility {
    override val name: String = "속보"
    override val description: String = "취재 대상이 마피아라면 두 번째 밤이 될 때 기사를 낼 수 있다."
    override val image: String = ReporterAssets.BREAKING_NEWS_ABILITY_IMAGE_URL
    override val targetJob: List<KClass<out Job>> = listOf(Reporter::class)
}
