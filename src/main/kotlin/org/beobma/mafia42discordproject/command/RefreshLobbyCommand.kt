package org.beobma.mafia42discordproject.command

import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import org.beobma.mafia42discordproject.discord.DiscordMessageManager
import org.beobma.mafia42discordproject.game.GameManager

object RefreshLobbyCommand : DiscordCommand {
    override val name: String = "refresh"
    override val description: String = "현재 음성채널 인원으로 게임 대기 목록을 새로고침합니다."
    override val koreanName: String = "새로고침"
    override val aliases: Set<String> = setOf("새로고침")

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        val result = GameManager.refreshLobby(event)
        DiscordMessageManager.respondPublic(event, result.message)
    }

    override suspend fun handleMessage(event: MessageCreateEvent, args: List<String>) {
        val result = GameManager.refreshLobby(event)
        event.message.channel.createMessage(result.message)
    }
}
