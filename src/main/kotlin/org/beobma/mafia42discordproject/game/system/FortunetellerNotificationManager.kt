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

        val fortuneMessage = shownJobs.joinToString(" 또는 ")
        val baseMessage = "[운세] ${target.member.effectiveName}님의 직업은 ${fortuneMessage} 중 하나입니다."

        val message = if (arcanaTargets.isEmpty()) {
            baseMessage
        } else {
            val arcanaNames = arcanaTargets.joinToString(", ") { it.member.effectiveName }
            "$baseMessage\n[아르카나] ${target.member.effectiveName}, ${arcanaNames} 중에 운세 직업(${shownJobs.joinToString(", ")})이 모두 포함되어 있습니다."
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
