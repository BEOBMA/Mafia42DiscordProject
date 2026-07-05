package org.beobma.mafia42discordproject.discord

import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.User
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GameManager
import org.beobma.mafia42discordproject.game.replay.GameReplayLogger
import org.beobma.mafia42discordproject.game.replay.ReplayVisibility
import org.beobma.mafia42discordproject.lavalink.LavalinkManager

object DiscordMessageManager {
    private val replayTrackedEphemeralCommands = setOf(
        "use",
        "daytime",
        "shaman-relay",
        "spirit-relay",
        "megaphone",
        "secret-letter",
        "will",
        "perjury",
        "password",
        "gamestop"
    )

    fun mention(user: User): String = user.mention

    fun mentions(users: List<User>): String = users.joinToString("\n") { "• ${it.mention}" }

    suspend fun Game.sendMainChannerMessage(msg: String) {
        sendMainChannerCombinedMessage(msg)
    }

    suspend fun Game.sendMainChannerCombinedMessage(vararg messages: String) {
        val mainChannel = this.mainChannel ?: return
        val content = buildString {
            messages
                .map(String::trim)
                .filter(String::isNotBlank)
                .forEachIndexed { index, message ->
                    if (index > 0) appendLine()
                    append(message)
                }
        }
        if (content.isBlank()) return
        GameReplayLogger.logSystem(
            game = this,
            title = "공개 메시지",
            body = content,
            visibility = ReplayVisibility.PUBLIC
        )
        mainChannel.createMessage(content)
    }

    suspend fun Game.sendMainChannelMessageWithImage(imageLink: String, message: String) {
        sendMainChannerCombinedMessage(imageLink, message)
    }

    suspend fun Game.playGameSound(soundPath: String, volume: Int = 100) {
        val voiceChannelId = this.voiceChannelId ?: return
        runCatching {
            LavalinkManager.play(
                kord = this.guild.kord,
                guildId = this.guild.id,
                voiceChannelId = voiceChannelId,
                source = soundPath,
                volume = volume
            )
        }.onFailure { error ->
            println("⚠️ 사운드 재생 실패: ${error.message}")
        }
    }

    suspend fun Game.sendMainChannerMessageAndSound(msg: String, soundPath: String, soundVolume: Int = 100) {
        coroutineScope {
            launch { sendMainChannerCombinedMessage(msg) }
            launch { playGameSound(soundPath, soundVolume) }
        }
    }

    suspend fun Game.sendMainChannelMessageWithImageAndSound(
        imageLink: String,
        message: String,
        soundPath: String,
        soundVolume: Int = 100
    ) {
        coroutineScope {
            launch { sendMainChannelMessageWithImage(imageLink, message) }
            launch { playGameSound(soundPath, soundVolume) }
        }
    }

    suspend fun respondPublic(event: GuildChatInputCommandInteractionCreateEvent, content: String) {
        InteractionErrorHandler.runSafely("slash-public:${event.interaction.command.rootName}") {
            event.interaction.respondPublic {
                this.content = content.takeIf { it.isNotBlank() } ?: "처리되었습니다."
            }
        }
    }

    suspend fun respondEphemeral(event: GuildChatInputCommandInteractionCreateEvent, content: String) {
        InteractionErrorHandler.runSafely("slash-ephemeral:${event.interaction.command.rootName}") {
            val responseContent = content.takeIf { it.isNotBlank() } ?: "처리했습니다."
            if (event.interaction.command.rootName in replayTrackedEphemeralCommands) {
                val game = GameManager.getCurrentGameFor(event.interaction.user.id)
                val recipient = game?.getPlayer(event.interaction.user.id)
                if (game != null && recipient != null) {
                    GameReplayLogger.logEphemeral(game, recipient, responseContent)
                }
            }
            val deferred = event.interaction.deferEphemeralResponse()
            deferred.respond {
                this.content = responseContent
            }
        }
    }
}
