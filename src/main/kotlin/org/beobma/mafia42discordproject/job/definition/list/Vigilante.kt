package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

class Vigilante : Job(), Definition {
    override val name: String = "자경단원"
    override val description: String = "[숙청] 게임당 한 번, 낮에 플레이어 한 명을 선택해 마피아 여부를 알아낼 수 있으며 밤에 마피아를 처형할 수 있다. (1회용)"
}