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
    private const val targetOptionName = "target"
    private const val messageOptionName = "message"

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        val target = event.interaction.command.users[targetOptionName]
        if (target == null) {
            DiscordMessageManager.respondEphemeral(event, "밀서 대상을 지정해 주세요.")
            return
        }

        val message = event.interaction.command.strings[messageOptionName].orEmpty()
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
            user(targetOptionName, "밀서를 받을 대상") {
                required = true
            }
            string(messageOptionName, "밀서 내용") {
                required = true
            }
        }
    }

    override suspend fun registerGuild(kord: Kord, guildId: Snowflake) {
        kord.createGuildChatInputCommand(guildId, name, description) {
            applyKoreanLocalization(this)
            user(targetOptionName, "밀서를 받을 대상") {
                required = true
            }
            string(messageOptionName, "밀서 내용") {
                required = true
            }
        }
    }
}
