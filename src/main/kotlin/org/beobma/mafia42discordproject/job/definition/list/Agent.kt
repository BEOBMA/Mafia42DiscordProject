package org.beobma.mafia42discordproject.job.definition.list

import dev.kord.common.entity.Snowflake
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.agent.AgentOperation
import org.beobma.mafia42discordproject.job.definition.Definition

class Agent : Job(), Definition {
    override val name: String = "요원"
    override val description: String = "[공작] 낮마다 지령을 받아 시민 한 명의 직업을 알아낸다."
    override val jobImage: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(85).webp"
    val discoveredCitizenTargetIds: MutableSet<Snowflake> = mutableSetOf()
    val discoveredCitizenTargetDayById: MutableMap<Snowflake, Int> = mutableMapOf()
    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(AgentOperation())
}
