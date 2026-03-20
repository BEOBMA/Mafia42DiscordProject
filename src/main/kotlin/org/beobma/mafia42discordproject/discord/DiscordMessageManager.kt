package org.beobma.mafia42discordproject.discord

import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.channel.edit
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.User
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import org.beobma.mafia42discordproject.game.Game

object DiscordMessageManager {
    fun mention(user: User): String = user.mention

    fun mentions(users: List<User>): String = users.joinToString("\n") { "• ${it.mention}" }

    suspend fun Game.sendMainChannerMessage(msg: String) {
        val mainChannel = this.mainChannel ?: return
        mainChannel.createMessage(msg)
    }

    suspend fun Game.sendMainChannerImage(imageLink: String) {
        val mainChannel = this.mainChannel ?: return
        mainChannel.createMessage {
            content = imageLink
        }
    }

    suspend fun respondPublic(event: GuildChatInputCommandInteractionCreateEvent, content: String) {
        event.interaction.respondPublic {
            this.content = content
        }
    }

    suspend fun respondEphemeral(event: GuildChatInputCommandInteractionCreateEvent, content: String) {
        val deferred = event.interaction.deferEphemeralResponse()
        deferred.respond {
            this.content = content
        }
    }
}
