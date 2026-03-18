package org.beobma.mafia42discordproject.command

import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import org.beobma.mafia42discordproject.game.GameManager

object GameStartCommand : DiscordCommand {
    override val name: String = "gamestart"
    override val description: String = "게임을 시작합니다."

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        GameManager.start(event)
    }
}
