package org.beobma.mafia42discordproject.command

import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import org.beobma.mafia42discordproject.game.GameManager

object GameStartCommand : DiscordCommand {
    override val name: String = "gameStart"
    override val description: String = "게임을 시작합니다."

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        event.interaction.respondPublic {
            val mention = event.interaction.user.mention
            content = "${mention}이(가) 게임을 시작했습니다."
            GameManager.start()
        }
    }
}
