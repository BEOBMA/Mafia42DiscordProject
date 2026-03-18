package org.beobma.mafia42discordproject.job.ability.general.definition.list.agent

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Agent
import org.beobma.mafia42discordproject.job.definition.list.Detective
import org.beobma.mafia42discordproject.job.definition.list.Gangster
import org.beobma.mafia42discordproject.job.definition.list.Ghoul
import org.beobma.mafia42discordproject.job.definition.list.Martyr
import org.beobma.mafia42discordproject.job.definition.list.Nurse
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import kotlin.reflect.KClass

class Humint : Ability, JobSpecificExtraAbility {
    override val name: String = "휴민트"
    override val description: String = "지령 대상이 다음 낮부터 요원에게 능력을 사용할 경우 요원의 정보를 전달한다."
    override val targetJob: List<KClass<out Job>> = listOf(Agent::class)
}