package org.beobma.mafia42discordproject.game.system

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.job.JobManager

object PrivateJobKnowledgeManager {
    fun rememberExactJob(
        game: Game?,
        observer: PlayerData,
        target: PlayerData,
        displayedJobName: String
    ) {
        if (JobManager.findByName(displayedJobName) == null) return
        game?.privateDisplayedJobNamesByObserver
            ?.getOrPut(observer.member.id, ::mutableMapOf)
            ?.set(target.member.id, displayedJobName)
    }
}
