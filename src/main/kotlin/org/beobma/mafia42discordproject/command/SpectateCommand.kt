package org.beobma.mafia42discordproject.command

import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import org.beobma.mafia42discordproject.discord.DiscordMessageManager
import org.beobma.mafia42discordproject.game.GameManager

object SpectateCommand : DiscordCommand {
    override val name: String = "spectate"
    override val description: String = "현재 음성채널에서 관전자로 등록합니다."
    override val koreanName: String = "관전"
    override val aliases: Set<String> = setOf("관전")

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        val result = GameManager.markSpectator(event)
        DiscordMessageManager.respondEphemeral(event, result.message)
    }

    override suspend fun handleMessage(event: MessageCreateEvent, args: List<String>) {
        val result = GameManager.markSpectator(event)
        event.message.channel.createMessage(result.message)
    }
}
