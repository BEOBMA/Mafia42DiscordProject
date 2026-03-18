package org.beobma.mafia42discordproject.job

import dev.kord.gateway.Command

abstract class Job {
    abstract val name: String
    abstract val description: String
}