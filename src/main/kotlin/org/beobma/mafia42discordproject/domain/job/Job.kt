package org.beobma.mafia42discordproject.job

import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility

abstract class Job {
    abstract val name: String
    abstract val description: String
    open val jobImage: String? = null
    open val abilities: MutableList<JobUniqueAbility> = mutableListOf()
    val extraAbilities: MutableList<Ability> = mutableListOf()
}
