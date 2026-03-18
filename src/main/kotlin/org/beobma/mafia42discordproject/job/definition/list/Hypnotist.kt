package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

class Hypnotist : Job(), Definition {
    override val name: String = "최면술사"
    override val description: String = "[최면] 하루에 한 번, 밤에 다른 플레이어에게 최면을 걸거나 낮에 최면에 걸린 플레이어들을 깨워 시민팀이 아니라면 직업을 알아낸다."
}