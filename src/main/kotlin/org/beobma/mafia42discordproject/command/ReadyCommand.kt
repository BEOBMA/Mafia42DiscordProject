package org.beobma.mafia42discordproject.command

import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import org.beobma.mafia42discordproject.discord.DiscordMessageManager
import org.beobma.mafia42discordproject.game.GameManager

object ReadyCommand : DiscordCommand {
    override val name: String = "ready"
    override val description: String = "현재 음성채널에서 플레이어로 준비합니다."
    override val koreanName: String = "준비"
    override val aliases: Set<String> = setOf("준비")

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        val result = GameManager.markReady(event)
        DiscordMessageManager.respondEphemeral(event, result.message)
    }

    override suspend fun handleMessage(event: MessageCreateEvent, args: List<String>) {
        val result = GameManager.markReady(event)
        event.message.channel.createMessage(result.message)
    }
}
