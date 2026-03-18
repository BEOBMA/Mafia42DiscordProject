package org.beobma.mafia42discordproject.job.evil.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.evil.Evil

class Villain : Job(), Evil {
    override val name: String = "악인"
    override val description: String = ""
}