package org.beobma.mafia42discordproject.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.string
import org.beobma.mafia42discordproject.discord.DiscordMessageManager
import org.beobma.mafia42discordproject.game.GameManager

object PasswordCommand : DiscordCommand {
    override val name: String = "password"
    override val description: String = "암구호 메시지를 마피아 팀 채널에 전송합니다."
    override val koreanName: String = "암구호"
    override val aliases: Set<String> = setOf("암구호")
    private const val messageOptionName = "message"

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        val message = event.interaction.command.strings[messageOptionName].orEmpty()
        val result = GameManager.sendPasswordChat(event.interaction.user.id, message)
        DiscordMessageManager.respondEphemeral(event, result.message)
    }

    override suspend fun registerGlobal(kord: Kord) {
        kord.createGlobalChatInputCommand(name, description) {
            applyKoreanLocalization(this)
            string(messageOptionName, "암구호 내용") {
                required = true
            }
        }
    }

    override suspend fun registerGuild(kord: Kord, guildId: Snowflake) {
        kord.createGuildChatInputCommand(guildId, name, description) {
            applyKoreanLocalization(this)
            string(messageOptionName, "암구호 내용") {
                required = true
            }
        }
    }
}
