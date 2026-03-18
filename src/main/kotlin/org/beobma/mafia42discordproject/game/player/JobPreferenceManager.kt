package org.beobma.mafia42discordproject.game.player

import org.beobma.mafia42discordproject.job.Job

object JobPreferenceManager {
    private val preferencesByUserId: MutableMap<ULong, List<Job>> = mutableMapOf()

    fun save(userId: ULong, jobs: List<Job>) {
        preferencesByUserId[userId] = jobs.toList()
    }

    fun get(userId: ULong): List<Job>? = preferencesByUserId[userId]
}
