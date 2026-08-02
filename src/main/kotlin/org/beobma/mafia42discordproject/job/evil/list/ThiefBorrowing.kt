package org.beobma.mafia42discordproject.job.evil.list

import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.job.Job

inline fun <reified T : Job> PlayerData.actualOrStolenJob(): T? {
    return job as? T ?: (job as? Thief)?.stolenJob as? T
}

fun PlayerData.hasStolenJob(jobName: String): Boolean {
    return (job as? Thief)?.stolenJob?.name == jobName
}
