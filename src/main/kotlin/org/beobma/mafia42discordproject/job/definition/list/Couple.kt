package org.beobma.mafia42discordproject.job.definition.list

import dev.kord.common.entity.Snowflake
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

enum class CoupleRole {
    MALE,
    FEMALE
}

class Couple : Job(), Definition {
    override val name: String = "연인"
    override val description: String = "[연애] 밤에 다른 연인과 서로 대화가 가능하다.\n[희생] 연인 두 명이 모두 생존하고 있을 때, 연인 한명이 마피아에게 지목당할 경우 다른 연인이 대신 죽게 된다."
    override val jobImage: String = "https://media.discordapp.net/attachments/1483977619258212392/1484548919030579251/chrome_eXhMsE2RJZ.png?ex=69bea16b&is=69bd4feb&hm=ca67d3bfd1f9e3a76e88e01b23abd4e9e39353bc8324de6167598b17d1a3d05f&=&format=webp&quality=lossless"

    var role: CoupleRole? = null
    var pairedPlayerId: Snowflake? = null
    val avengedMafiaIds: MutableSet<Snowflake> = mutableSetOf()
}
