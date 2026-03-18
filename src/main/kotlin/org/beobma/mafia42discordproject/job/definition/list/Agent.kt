package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

class Agent : Job(), Definition {
    override val name: String = "요원"
    override val description: String = "[공작] 낮마다 지령을 받아 시민 한 명의 직업을 알아낸다."
}