package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

class Detective : Job(), Definition {
    override val name: String = "사립탐정"
    override val description: String = "[추리] 밤마다 플레이어 한 명을 선택하여 그 플레이어가 누구에게 능력을 사용하였는지 알아낼 수 있다."
}