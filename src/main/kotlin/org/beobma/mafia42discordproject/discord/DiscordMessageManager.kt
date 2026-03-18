package org.beobma.mafia42discordproject.discord

import dev.kord.color.Color
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.Member
import dev.kord.core.entity.User
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent

object DiscordMessageManager {
    private val defaultEmbedColor = Color(0x5865F2)

    fun mention(user: User): String = user.mention

    fun mentions(users: List<User>): String = users.joinToString("\n") { "• ${it.mention}" }

    suspend fun respondPublic(event: GuildChatInputCommandInteractionCreateEvent, content: String) {
        event.interaction.respondPublic {
            embed {
                description = content
                color = defaultEmbedColor
            }
        }
    }

    suspend fun respondEphemeral(event: GuildChatInputCommandInteractionCreateEvent, content: String) {
        val deferred = event.interaction.deferEphemeralResponse()
        deferred.respond {
            embed {
                description = content
                color = defaultEmbedColor
            }
        }
    }

    suspend fun sendChannelMessage(channel: MessageChannelBehavior, content: String) {
        channel.createMessage {
            embed {
                description = content
                color = defaultEmbedColor
            }
        }
    }

    suspend fun sendDirectMessage(member: Member, content: String) {
        member.getDmChannel().createMessage {
            embed {
                description = content
                color = defaultEmbedColor
            }
        }
    }
}
