package org.beobma.mafia42discordproject.game.system

import org.beobma.mafia42discordproject.game.GameManager
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.replay.GameReplayLogger

object FortunetellerNotificationManager {
    suspend fun notifyFortuneResult(
        fortuneteller: PlayerData,
        target: PlayerData,
        shownJobs: List<String>,
        arcanaTargets: List<PlayerData>
    ) {
        if (shownJobs.isEmpty()) return

        val baseMessage = "${target.member.effectiveName}의 직업은 ${shownJobs[0]} 또는 ${shownJobs[1]}"
        val imageUrl = SystemImage.FORTUNETELLER_NOTICE.imageUrl

        val message = if (arcanaTargets.isEmpty()) {
            "$imageUrl\n$baseMessage"
        } else {
            val arcanaNames = arcanaTargets.joinToString(", ") { it.member.effectiveName }
            "$imageUrl\n$baseMessage\n${target.member.effectiveName}, $arcanaNames 셋 중 둘의 직업은 ${shownJobs[0]}, ${shownJobs[1]}"
        }

        runCatching {
            GameManager.getCurrentGameFor(fortuneteller.member.id)?.let { game ->
                GameReplayLogger.logDirectMessage(game, fortuneteller, message, "예언 결과")
            }
            fortuneteller.member.getDmChannel().createMessage(message)
        }
    }
}
