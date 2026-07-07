package org.beobma.mafia42discordproject.game.replay

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

}
