package org.beobma.mafia42discordproject.job.definition.list

import dev.kord.common.entity.Snowflake
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.mercenary.MercenaryAbility
import org.beobma.mafia42discordproject.job.definition.Definition

class Mercenary : Job(), Definition {
    override val name: String = "용병"
    override val description: String = "[의뢰] 게임 시작 시 시민 팀 플레이어 한 명이 의뢰인으로 지정되며 낮이 될 때 의뢰를 받는다. 의뢰를 받은 후 의뢰인이 밤에 살해당할 경우, 밤마다 플레이어 한 명을 처형할 수 있게 된다."
    override val jobImage: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(58).webp"
    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(MercenaryAbility())

    var clientPlayerId: Snowflake? = null
    var hasReceivedContract: Boolean = false
    var hasExecutionAuthority: Boolean = false
    var clientKilledByPlayerId: Snowflake? = null
    var firstTrackedTargetId: Snowflake? = null
}
