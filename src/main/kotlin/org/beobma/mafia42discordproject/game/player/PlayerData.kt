package org.beobma.mafia42discordproject.game.player

import dev.kord.core.entity.Member
import org.beobma.mafia42discordproject.job.Job

data class PlayerData(
    val member: Member,
    var job: Job? = null
)