package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

class Police : Job(), Definition {
    override val name: String = "경찰"
    override val description: String = "[수색] 밤마다 플레이어 한 명을 조사하여 그 플레이어의 마피아 여부를 알아낼 수 있다."
}