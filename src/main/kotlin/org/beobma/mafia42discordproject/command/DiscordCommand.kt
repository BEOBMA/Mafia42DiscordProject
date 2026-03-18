package org.beobma.mafia42discordproject.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.interaction.GuildAutoCompleteInteractionCreateEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent

interface DiscordCommand {
    val name: String
    val description: String

    suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent)

    suspend fun handleAutoComplete(event: GuildAutoCompleteInteractionCreateEvent) = Unit

    suspend fun registerGlobal(kord: Kord) {
        kord.createGlobalChatInputCommand(name, description)
    }

    suspend fun registerGuild(kord: Kord, guildId: Snowflake) {
        kord.createGuildChatInputCommand(guildId, name, description)
    }
}
