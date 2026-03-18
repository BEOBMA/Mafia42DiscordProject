package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

class Fortuneteller : Job(), Definition {
    override val name: String = "점쟁이"
    override val description: String = "[운세] 밤마다 한 명을 선택한다. 선택한 플레이어 및 그와 다른 팀인 플레이어의 직업이 제시된다."
}