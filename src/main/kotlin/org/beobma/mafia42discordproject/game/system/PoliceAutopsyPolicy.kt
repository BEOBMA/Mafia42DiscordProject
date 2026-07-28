package org.beobma.mafia42discordproject.game.system

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.evil.list.Mafia

internal object PoliceAutopsyPolicy {
    fun isMafia(job: Job?): Boolean = job is Mafia
}
