package org.beobma.mafia42discordproject.game.system.notifications

import org.beobma.mafia42discordproject.game.player.PlayerData
import dev.kord.core.behavior.channel.createMessage
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.system.GameEvent

abstract class BaseNotificationManager {
    protected suspend fun sendDmWithImage(targetPlayer: PlayerData, text: String, imageUrl: String? = null) {
        val finalMessage = if (imageUrl != null) "$imageUrl\n$text" else text

        try {
            val dmChannel = targetPlayer.member.getDmChannel()
            dmChannel.createMessage(finalMessage)
        } catch (e: Exception) {
            println("[DM 전송 실패] ${targetPlayer.member.effectiveName}님에게 메시지를 보낼 수 없습니다. 사유: ${e.message}")
        }
    }
}