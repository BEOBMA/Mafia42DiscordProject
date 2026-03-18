package org.beobma.mafia42discordproject.command

import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import org.beobma.mafia42discordproject.game.GameManager

object GameStopCommand : DiscordCommand {
    override val name: String = "gamestop"
    override val description: String = "게임을 종료합니다."

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        GameManager.stop(event)
    }

    override suspend fun handleMessage(event: MessageCreateEvent, args: List<String>) {
        GameManager.stop(event)
    }
}
