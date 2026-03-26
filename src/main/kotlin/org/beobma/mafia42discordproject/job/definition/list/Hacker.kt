package org.beobma.mafia42discordproject.job.definition.list

import dev.kord.common.entity.Snowflake
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.hacker.HackerAbility
import org.beobma.mafia42discordproject.job.definition.Definition

class Hacker : Job(), Definition {
    override val name: String = "해커"
    override val description: String = "[해킹] 낮에 플레이어 한 명을 골라 그날 밤에 즉시 직업을 알아낸다.\n[프록시] 낮에 플레이어 한 명을 골라 밤 동안 자신에게 발동되는 능력을 우회 적용시킨다."
    override val jobImage: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(62).webp"
    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(HackerAbility())

    var hackedTargetId: Snowflake? = null
    var hasResolvedHackDiscovery: Boolean = false
}
