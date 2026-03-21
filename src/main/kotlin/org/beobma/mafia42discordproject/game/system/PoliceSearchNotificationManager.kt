package org.beobma.mafia42discordproject.game.system

import dev.kord.core.behavior.channel.createMessage
import org.beobma.mafia42discordproject.game.player.PlayerData

object PoliceSearchNotificationManager {
    suspend fun notifyPoliceSearchResults(events: List<GameEvent>) {
        events.filterIsInstance<GameEvent.PoliceSearchResolved>()
            .forEach { event ->
                sendPoliceDm(event.police, buildSearchResultMessage(event))
            }

        events.filterIsInstance<GameEvent.PoliceJobRevealed>()
            .forEach { event ->
                sendPoliceDm(event.police, buildRevealedJobMessage(event))
            }
    }

    private suspend fun sendPoliceDm(police: PlayerData, message: String) {
        if (message.isBlank()) return

        runCatching {
            police.member.getDmChannel().createMessage(message)
        }
    }

    private fun buildSearchResultMessage(event: GameEvent.PoliceSearchResolved): String {
        val targetName = event.target.member.effectiveName

        return if (event.isMafia) {
            "그 사람의 직업은 마피아"
        } else {
            "${targetName}님은 마피아가 아닙니다."
        }
    }

    private fun buildRevealedJobMessage(event: GameEvent.PoliceJobRevealed): String {
        val jobName = event.revealedJob.name
        return "그 사람의 직업은 ${jobName}."
    }
}
