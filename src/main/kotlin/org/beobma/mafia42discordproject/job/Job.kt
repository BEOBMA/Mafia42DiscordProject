package org.beobma.mafia42discordproject.job

import dev.kord.gateway.Command
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility

abstract class Job {
    abstract val name: String
    abstract val description: String
    abstract val uniqueAbilities: List<JobUniqueAbility> // 직업 고유 기본능력
}