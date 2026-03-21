package org.beobma.mafia42discordproject.job.definition.list

import dev.kord.common.entity.Snowflake
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.hacker.HackerAbility
import org.beobma.mafia42discordproject.job.definition.Definition

class Hacker : Job(), Definition {
    override val name: String = "해커"
    override val description: String = "[해킹] 낮에 플레이어 한 명을 골라 밤이 될 때 직업을 알아낸다.\n[프록시] 낮에 플레이어 한 명을 골라 밤 동안 자신에게 발동되는 능력을 우회 적용시킨다."
    override val jobImage: String = "https://media.discordapp.net/attachments/1483977619258212392/1484548914672566302/chrome_AbVsNjjvSV.png?ex=69bea16a&is=69bd4fea&hm=8717acb1f11b7f2ae7330a166b86f7dec66e8d5b4b280fe3577e41286871ca96&=&format=webp&quality=lossless"
    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(HackerAbility())

    var hackedTargetId: Snowflake? = null
    var hasResolvedHackDiscovery: Boolean = false
}
