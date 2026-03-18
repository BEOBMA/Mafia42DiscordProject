package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

class Doctor : Job(), Definition {
    override val name: String = "의사"
    override val description: String = "[치료] 밤이 되면 플레이어 한 명을 처형으로부터 치료한다."
}