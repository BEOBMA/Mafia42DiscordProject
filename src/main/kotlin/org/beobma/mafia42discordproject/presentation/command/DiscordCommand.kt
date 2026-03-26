package org.beobma.mafia42discordproject.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.interaction.GuildAutoCompleteInteractionCreateEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent

interface DiscordCommand {
    val name: String
    val description: String
    val koreanName: String? get() = null
    val aliases: Set<String> get() = emptySet()

    suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent)

    suspend fun handleAutoComplete(event: GuildAutoCompleteInteractionCreateEvent) = Unit

    suspend fun handleMessage(event: MessageCreateEvent, args: List<String>) {
        event.message.channel.createMessage("`!$name` 명령어는 현재 슬래시 명령어(`/$name`)로만 지원됩니다.")
    }

    suspend fun registerGlobal(kord: Kord) {
        kord.createGlobalChatInputCommand(name, description) {
            this@DiscordCommand.applyKoreanLocalization(this)
        }
    }

    suspend fun registerGuild(kord: Kord, guildId: Snowflake) {
        kord.createGuildChatInputCommand(guildId, name, description) {
            this@DiscordCommand.applyKoreanLocalization(this)
        }
    }
}
