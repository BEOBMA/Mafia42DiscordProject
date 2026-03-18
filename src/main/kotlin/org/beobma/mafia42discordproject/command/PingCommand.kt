package org.beobma.mafia42discordproject.command

import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import org.beobma.mafia42discordproject.discord.DiscordMessageManager

object PingCommand : DiscordCommand {
    override val name: String = "ping"
    override val description: String = "봇 응답 속도를 확인합니다."

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        DiscordMessageManager.respondPublic(event, "Pong! 🏓")
    }
}
