package org.beobma.mafia42discordproject.job

import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility

abstract class Job {
    abstract val name: String
    abstract val description: String
    open val abilities: MutableList<JobUniqueAbility> = mutableListOf()
}
