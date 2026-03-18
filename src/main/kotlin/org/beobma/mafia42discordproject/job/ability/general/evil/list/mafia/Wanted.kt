package org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.AssistanceCommonAbility
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import kotlin.reflect.KClass

class Wanted : Ability, JobSpecificExtraAbility {
    override val name: String = "수배"
    override val description: String = "첫날 낮이 될 때 접선하지 않은 마피아팀의 직업을 알 수 있다."
    override val targetJob: List<KClass<out Job>> = listOf(Mafia::class)
}