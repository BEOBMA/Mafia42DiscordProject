package org.beobma.mafia42discordproject.game.system

import dev.kord.core.behavior.channel.createMessage
import org.beobma.mafia42discordproject.game.player.PlayerData

object FortunetellerNotificationManager {
    suspend fun notifyFortuneResult(
        fortuneteller: PlayerData,
        target: PlayerData,
        shownJobs: List<String>,
        arcanaTargets: List<PlayerData>
    ) {
        if (shownJobs.isEmpty()) return

        val baseMessage = "(${target.member.effectiveName})의 직업은 (${shownJobs[0]}) 또는 (${shownJobs[1]})."
        val imageUrl = SystemImage.FORTUNETELLER_NOTICE.imageUrl

        val message = if (arcanaTargets.isEmpty()) {
            "$imageUrl\n$baseMessage"
        } else {
            val arcanaNames = arcanaTargets.joinToString(", ") { it.member.effectiveName }
            "$imageUrl\n$baseMessage\n(${target.member.effectiveName}), (${arcanaNames}) 셋 중 둘의 직업은 (${shownJobs[0]}), (${shownJobs[1]})."
        }

        runCatching {
            fortuneteller.member.getDmChannel().createMessage(message)
        }
    }

    suspend fun notifyUnavailableTarget(
        fortuneteller: PlayerData,
        target: PlayerData
    ) {
        runCatching {
            fortuneteller.member.getDmChannel()
                .createMessage("[운세] 지정한 대상(${target.member.effectiveName})이 이미 사망하여 오늘 밤 결과를 확인할 수 없습니다.")
        }
    }
}
