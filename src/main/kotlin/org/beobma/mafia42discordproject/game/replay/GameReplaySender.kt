package org.beobma.mafia42discordproject.game.replay

import dev.kord.core.behavior.channel.createMessage
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.web.WebNotepadServer

object GameReplaySender {

    suspend fun sendReplay(game: Game, renderData: ReplayRenderData) {
        if (game.hasSentReplay) return

        runCatching {
            val mainChannel = game.mainChannel ?: return
            mainChannel.createMessage {
                content = "게임 리플레이를 웹에서 확인하세요.\n${WebNotepadServer.replayUrl(renderData)}"
            }
            game.hasSentReplay = true
        }.onFailure { error ->
            println("[GameReplaySender] replay send failed: ${error.message}")
        }
    }
}
