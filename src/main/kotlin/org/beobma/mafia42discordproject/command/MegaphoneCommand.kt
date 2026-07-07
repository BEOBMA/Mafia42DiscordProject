package org.beobma.mafia42discordproject.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.string
import org.beobma.mafia42discordproject.discord.DiscordMessageManager
import org.beobma.mafia42discordproject.game.GameManager

object MegaphoneCommand : DiscordCommand {
    override val name: String = "megaphone"
    override val description: String = "확성기 메시지를 전송합니다."
    override val koreanName: String = "확성기"
    override val aliases: Set<String> = setOf("확성기")
    private const val MESSAGE_OPTION_NAME = "message"

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        val message = event.interaction.command.strings[MESSAGE_OPTION_NAME].orEmpty()
        val result = GameManager.useMegaphone(event.interaction.user.id, message)
        DiscordMessageManager.respondEphemeral(event, result.message, trackReplay = !result.isSuccess)
    }

    override suspend fun registerGlobal(kord: Kord) {
        kord.createGlobalChatInputCommand(name, description) {
            applyKoreanLocalization(this)
            string(MESSAGE_OPTION_NAME, "전송할 확성기 메시지") {
                required = true
            }
        }
    }

    override suspend fun registerGuild(kord: Kord, guildId: Snowflake) {
        kord.createGuildChatInputCommand(guildId, name, description) {
            applyKoreanLocalization(this)
            string(MESSAGE_OPTION_NAME, "전송할 확성기 메시지") {
                required = true
            }
        }
    }
}
