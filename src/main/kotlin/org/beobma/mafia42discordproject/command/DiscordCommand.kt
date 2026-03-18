package org.beobma.mafia42discordproject.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.event.interaction.GuildAutoCompleteInteractionCreateEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.GuildMessageCreateEvent

interface DiscordCommand {
    val name: String
    val description: String

    suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent)

    suspend fun handleAutoComplete(event: GuildAutoCompleteInteractionCreateEvent) = Unit

    suspend fun handleMessage(event: GuildMessageCreateEvent, args: List<String>) {
        event.message.channel.createMessage("`!$name` 명령어는 현재 슬래시 명령어(`/$name`)로만 지원됩니다.")
    }

    suspend fun registerGlobal(kord: Kord) {
        kord.createGlobalChatInputCommand(name, description)
    }

    suspend fun registerGuild(kord: Kord, guildId: Snowflake) {
        kord.createGuildChatInputCommand(guildId, name, description)
    }
}
