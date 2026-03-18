package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

class Agent : Job(), Definition {
    override val name: String = "시민"
    override val description: String = ""
}