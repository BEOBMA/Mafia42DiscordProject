package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

class Hacker : Job(), Definition {
    override val name: String = "해커"
    override val description: String = "[해킹] 낮에 플레이어 한 명을 골라 밤이 될 때 직업을 알아낸다.\n" +
            "[프록시] 낮에 플레이어 한 명을 골라 밤 동안 자신에게 발동되는 능력을 우회 적용시킨다."
}