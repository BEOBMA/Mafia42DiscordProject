package org.beobma.mafia42discordproject.game.system.notifications

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.game.system.SystemImage

object PoliceNotificationManager : BaseNotificationManager() {

    // 부모 클래스의 메서드만 구현하면 전송은 자동으로 처리됨!
    suspend fun notifySearchResult(policePlayer: PlayerData, event: GameEvent.PoliceSearchResolved) {
        val targetName = event.target.member.effectiveName
        val image = if (event.isMafia) {
            SystemImage.POLICE_FOUND_MAFIA.imageUrl
        } else {
            SystemImage.POLICE_FOUND_FAIL.imageUrl
        }

        val text = if (event.isMafia) {
            "${targetName}님은 마피아입니다."
        } else {
            "${targetName}님은 마피아가 아닙니다."
        }
        sendDmWithImage(policePlayer, text, image)
    }

    suspend fun notifyWarrantResult(policePlayer: PlayerData, event: GameEvent.PoliceJobRevealed) {
        val jobName = event.revealedJob.name
        val text = "그 사람의 직업은 ${jobName}."
        val imageUrl = SystemImage.POLICE_USE_WARRANT.imageUrl

        // 부모 클래스의 메서드 호출
        sendDmWithImage(policePlayer, text, imageUrl)
    }
}