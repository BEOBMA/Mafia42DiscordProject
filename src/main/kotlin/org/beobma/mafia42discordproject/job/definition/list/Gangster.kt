package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

class Gangster : Job(), Definition {
    override val name: String = "건달"
    override val description: String = "[공갈] 밤마다 플레이어 한 명을 선택하여 다음날 투표시 해당 플레이어의 투표권을 빼앗는다."
}