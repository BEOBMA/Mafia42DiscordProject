package org.beobma.mafia42discordproject.job.evil.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.evil.Evil

class Mafia : Job(), Evil {
    override val name: String = "마피아"
    override val description: String = ""
}