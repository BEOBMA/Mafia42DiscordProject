package org.beobma.mafia42discordproject.game.replay

import dev.kord.core.behavior.channel.createMessage
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.utils.io.ByteReadChannel
import org.beobma.mafia42discordproject.game.Game

object GameReplaySender {

    suspend fun sendReplay(game: Game, renderData: ReplayRenderData) {
        if (game.hasSentReplay) return

        runCatching {
            val mainChannel = game.mainChannel ?: return
            val images = GameReplayImageRenderer.render(renderData)
            images.forEachIndexed { index, image ->
                mainChannel.createMessage {
                    content = if (images.size == 1) {
                        "게임 리플레이 이미지입니다."
                    } else {
                        "게임 리플레이 이미지입니다. (${index + 1}/${images.size})"
                    }
                    addFile(
                        image.fileName,
                        ChannelProvider(image.bytes.size.toLong()) {
                            ByteReadChannel(image.bytes)
                        }
                    )
                }
            }
            game.hasSentReplay = true
        }.onFailure { error ->
            println("[GameReplaySender] replay send failed: ${error.message}")
        }
    }
}
