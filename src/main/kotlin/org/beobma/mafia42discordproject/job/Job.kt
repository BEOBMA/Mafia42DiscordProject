package org.beobma.mafia42discordproject.job

import dev.kord.gateway.Command
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility

abstract class Job {
    abstract val name: String
    abstract val description: String
    open val uniqueAbilities: List<JobUniqueAbility> = listOf()
}