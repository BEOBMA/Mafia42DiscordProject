package org.beobma.mafia42discordproject.job.ability.general.definition.list.couple

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Couple
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import kotlin.reflect.KClass

class Resentment : Ability, JobSpecificExtraAbility {
    override val name: String = "원한"
    override val description: String = "짝 연인을 죽인 마피아를 투표한다면 2표로 인정된다."
    override val targetJob: List<KClass<out Job>> = listOf(Couple::class)
}