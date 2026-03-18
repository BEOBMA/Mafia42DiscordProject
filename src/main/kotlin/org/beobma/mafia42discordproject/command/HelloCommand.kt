package org.beobma.mafia42discordproject.command

import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent

object HelloCommand : DiscordCommand {
    override val name: String = "hello"
    override val description: String = "봇이 인사합니다."

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        event.interaction.respondPublic {
            val mention = event.interaction.user.mention
            content = "$mention 반가워요! Kotlin + Kord 봇이 동작 중입니다."
        }
    }
}
