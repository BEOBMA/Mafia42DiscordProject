package org.beobma.mafia42discordproject.job.ability.general.definition.list.other

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Cabal
import org.beobma.mafia42discordproject.job.definition.list.Nurse
import kotlin.reflect.KClass

class Postmortem : Ability, JobSpecificExtraAbility {
    override val name: String = "검시"
    override val description: String = "자신의 능력과 관련된 접선하지 않은 직업을 가진 사람이 사망한 경우, 그 사실을 알게 된다."
    override val targetJob: List<KClass<out Job>> = listOf(Nurse::class, Cabal::class)
}