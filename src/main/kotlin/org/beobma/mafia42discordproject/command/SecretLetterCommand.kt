package org.beobma.mafia42discordproject.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.interaction.user
import org.beobma.mafia42discordproject.discord.DiscordMessageManager
import org.beobma.mafia42discordproject.game.GameManager

object SecretLetterCommand : DiscordCommand {
    override val name: String = "secret-letter"
    override val description: String = "특정 대상에게 밀서를 보냅니다."
    override val koreanName: String = "밀서"
    override val aliases: Set<String> = setOf("밀서")
    private const val TARGET_OPTION_NAME = "target"
    private const val MESSAGE_OPTION_NAME = "message"

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        val target = event.interaction.command.users[TARGET_OPTION_NAME]
        if (target == null) {
            DiscordMessageManager.respondEphemeral(event, "밀서 대상을 지정해 주세요.")
            return
        }

        val message = event.interaction.command.strings[MESSAGE_OPTION_NAME].orEmpty()
        val result = GameManager.sendSecretLetter(
            memberId = event.interaction.user.id,
            targetId = target.id,
            message = message
        )
        DiscordMessageManager.respondEphemeral(event, result.message)
    }

    override suspend fun registerGlobal(kord: Kord) {
        kord.createGlobalChatInputCommand(name, description) {
            applyKoreanLocalization(this)
            user(TARGET_OPTION_NAME, "밀서를 받을 대상") {
                required = true
            }
            string(MESSAGE_OPTION_NAME, "밀서 내용") {
                required = true
            }
        }
    }

    override suspend fun registerGuild(kord: Kord, guildId: Snowflake) {
        kord.createGuildChatInputCommand(guildId, name, description) {
            applyKoreanLocalization(this)
            user(TARGET_OPTION_NAME, "밀서를 받을 대상") {
                required = true
            }
            string(MESSAGE_OPTION_NAME, "밀서 내용") {
                required = true
            }
        }
    }
}
