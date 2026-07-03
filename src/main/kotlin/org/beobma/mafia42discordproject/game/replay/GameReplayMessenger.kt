package org.beobma.mafia42discordproject.game.replay

import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.channel.TextChannel
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.player.PlayerData

object GameReplayMessenger {
    suspend fun sendTrackedDm(
        game: Game,
        recipient: PlayerData,
        content: String,
        title: String = "개인 DM",
        actor: PlayerData? = null
    ) {
        GameReplayLogger.logDirectMessage(game, recipient, content, title, actor)
        recipient.member.getDmChannel().createMessage(content)
    }

    suspend fun sendTrackedChannelMessage(
        game: Game,
        channel: TextChannel,
        content: String,
        visibility: ReplayVisibility,
        title: String = "채널 메시지",
        actor: PlayerData? = null
    ) {
        GameReplayLogger.logSystem(
            game = game,
            title = title,
            body = content,
            visibility = visibility,
            actor = actor
        )
        channel.createMessage(content)
    }
}
