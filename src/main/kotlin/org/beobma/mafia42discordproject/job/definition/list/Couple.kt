package org.beobma.mafia42discordproject.job.definition.list

import dev.kord.common.entity.Snowflake
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.couple.CoupleAbility
import org.beobma.mafia42discordproject.job.definition.Definition

enum class CoupleRole {
    MALE,
    FEMALE
}

class Couple : Job(), Definition {
    override val name: String = "연인"
    override val description: String = "[연애] 밤에 다른 연인과 서로 대화가 가능하다.\n[희생] 연인 두 명이 모두 생존하고 있을 때, 연인 한명이 마피아에게 지목당할 경우 다른 연인이 대신 죽게 된다."
    override val jobImage: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(82).webp"
    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(CoupleAbility())
    var role: CoupleRole? = null
    var pairedPlayerId: Snowflake? = null
    val avengedMafiaIds: MutableSet<Snowflake> = mutableSetOf()
}
