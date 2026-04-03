package org.beobma.mafia42discordproject.discord

import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.channel.edit
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.behavior.interaction.response.respond
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import dev.kord.core.entity.User
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.lavalink.LavalinkManager

object DiscordMessageManager {
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
        mainChannel.createMessage(content)
    }

    suspend fun Game.sendMainChannerImage(imageLink: String) {
        sendMainChannerCombinedMessage(imageLink)
    }

    suspend fun Game.sendMainChannelMessageWithImage(imageLink: String, message: String) {
        sendMainChannerCombinedMessage(imageLink, message)
    }

    suspend fun Game.playGameSound(soundPath: String) {
        val voiceChannelId = this.voiceChannelId ?: return
        runCatching {
            LavalinkManager.play(
                kord = this.guild.kord,
                guildId = this.guild.id,
                voiceChannelId = voiceChannelId,
                source = soundPath
            )
        }.onFailure { error ->
            println("⚠️ 사운드 재생 실패: ${error.message}")
        }
    }

    suspend fun Game.sendMainChannerMessageAndSound(msg: String, soundPath: String) {
        coroutineScope {
            launch { sendMainChannerCombinedMessage(msg) }
            launch { playGameSound(soundPath) }
        }
    }

    suspend fun Game.sendMainChannelMessageWithImageAndSound(imageLink: String, message: String, soundPath: String) {
        coroutineScope {
            launch { sendMainChannelMessageWithImage(imageLink, message) }
            launch { playGameSound(soundPath) }
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
            val deferred = event.interaction.deferEphemeralResponse()
            deferred.respond {
                this.content = content.takeIf { it.isNotBlank() } ?: "처리되었습니다."
            }
        }
    }
}
