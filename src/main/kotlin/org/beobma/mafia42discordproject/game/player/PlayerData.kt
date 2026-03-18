package org.beobma.mafia42discordproject.game.player

import org.beobma.mafia42discordproject.job.Job

data class PlayerData(
    val playerID: String,
    var job: Job
)