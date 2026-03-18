package org.beobma.mafia42discordproject.job.ability.general.definition.list.priest

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Police
import org.beobma.mafia42discordproject.job.definition.list.Priest
import kotlin.reflect.KClass

class Blessing : Ability, JobSpecificExtraAbility {
    override val name: String = "축복"
    override val description: String = "소생 능력 대상의 직업을 보존한다."
    override val targetJob: List<KClass<out Job>> = listOf(Priest::class)
}