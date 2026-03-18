package org.beobma.mafia42discordproject.command

import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent

interface DiscordCommand {
    val name: String
    val description: String

    suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent)
}
