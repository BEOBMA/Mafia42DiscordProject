package org.beobma.mafia42discordproject.game.system

import dev.kord.core.behavior.channel.createMessage
import org.beobma.mafia42discordproject.game.player.PlayerData

object PoliceSearchNotificationManager {
    suspend fun notifyPoliceSearchResult(event: GameEvent.PoliceSearchResolved) {
        sendPoliceDm(event.police, buildSearchResultMessage(event))
    }

    suspend fun notifyPoliceRevealedJob(event: GameEvent.PoliceJobRevealed) {
        sendPoliceDm(event.police, buildRevealedJobMessage(event))
    }

    suspend fun notifyPoliceSearchResults(events: List<GameEvent>) {
        events.filterIsInstance<GameEvent.PoliceSearchResolved>()
            .forEach { event ->
                notifyPoliceSearchResult(event)
            }

        events.filterIsInstance<GameEvent.PoliceJobRevealed>()
            .forEach { event ->
                notifyPoliceRevealedJob(event)
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
        val mafiaFoundImageUrl = SystemImage.POLICE_FOUND_MAFIA
        val mafiaFoundFailImageUrl = SystemImage.POLICE_FOUND_FAIL

        return if (event.isMafia) {
            "$mafiaFoundImageUrl\n${targetName}님은 마피아입니다."
        } else {
            "$mafiaFoundFailImageUrl\n${targetName}님은 마피아가 아닙니다."
        }
    }

    private fun buildRevealedJobMessage(event: GameEvent.PoliceJobRevealed): String {
        val jobName = event.revealedJob.name
        val warrantImageUrl = SystemImage.POLICE_USE_WARRANT

        return "$warrantImageUrl\n그 사람의 직업은 ${jobName}."
    }
}
