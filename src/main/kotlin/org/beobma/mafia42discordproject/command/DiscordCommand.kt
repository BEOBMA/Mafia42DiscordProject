package org.beobma.mafia42discordproject.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.interaction.GuildAutoCompleteInteractionCreateEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import org.beobma.mafia42discordproject.discord.DiscordMessageManager

interface DiscordCommand {
    val name: String
    val description: String

    suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent)

    suspend fun handleAutoComplete(event: GuildAutoCompleteInteractionCreateEvent) = Unit

    suspend fun handleMessage(event: MessageCreateEvent, args: List<String>) {
        DiscordMessageManager.sendChannelMessage(event.message.channel, "`!$name` 명령어는 현재 슬래시 명령어(`/$name`)로만 지원됩니다.")
    }

    suspend fun registerGlobal(kord: Kord) {
        kord.createGlobalChatInputCommand(name, description)
    }

    suspend fun registerGuild(kord: Kord, guildId: Snowflake) {
        kord.createGuildChatInputCommand(guildId, name, description)
    }
}
