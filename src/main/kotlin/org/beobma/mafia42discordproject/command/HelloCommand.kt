package org.beobma.mafia42discordproject.command

import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import org.beobma.mafia42discordproject.discord.DiscordMessageManager

object HelloCommand : DiscordCommand {
    override val name: String = "hello"
    override val description: String = "봇이 인사합니다."

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        val mention = DiscordMessageManager.mention(event.interaction.user)
        DiscordMessageManager.respondPublic(event, "$mention 반가워요! Kotlin + Kord 봇이 동작 중입니다.")
    }

    override suspend fun handleMessage(event: MessageCreateEvent, args: List<String>) {
        DiscordMessageManager.sendChannelMessage(event.message.channel, "${event.message.author?.mention.orEmpty()} 반가워요! Kotlin + Kord 봇이 동작 중입니다.")
    }
}
