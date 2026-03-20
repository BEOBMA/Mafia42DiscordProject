package org.beobma.mafia42discordproject.discord

import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.channel.edit
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.User
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import org.beobma.mafia42discordproject.game.Game

object DiscordMessageManager {
    private val imageUrlRegex = Regex(
        pattern = """^https?://\S+\.(png|jpe?g|gif|webp)(\?\S*)?$""",
        option = RegexOption.IGNORE_CASE
    )

    fun mention(user: User): String = user.mention

    fun mentions(users: List<User>): String = users.joinToString("\n") { "• ${it.mention}" }

    fun blindImageLinkIfNeeded(message: String): String {
        val trimmedMessage = message.trim()
        if (trimmedMessage.isBlank()) return ""
        if (trimmedMessage.startsWith("||") && trimmedMessage.endsWith("||")) return trimmedMessage
        return if (imageUrlRegex.matches(trimmedMessage)) "||$trimmedMessage||" else trimmedMessage
    }

    suspend fun Game.sendMainChannerMessage(msg: String) {
        sendMainChannerCombinedMessage(msg)
    }

    suspend fun Game.sendMainChannerCombinedMessage(vararg messages: String) {
        val mainChannel = this.mainChannel ?: return
        val content = buildString {
            messages
                .map(::blindImageLinkIfNeeded)
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
