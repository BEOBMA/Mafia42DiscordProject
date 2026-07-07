package org.beobma.mafia42discordproject.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.user
import org.beobma.mafia42discordproject.discord.DiscordMessageManager
import org.beobma.mafia42discordproject.game.GameManager

object PerjuryCommand : DiscordCommand {
    override val name: String = "perjury"
    override val description: String = "투표 시간에 가짜 투표를 행사합니다."
    override val koreanName: String = "위증"
    override val aliases: Set<String> = setOf("위증")
    private const val TARGET_OPTION_NAME = "target"

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        val target = event.interaction.command.users[TARGET_OPTION_NAME]
        if (target == null) {
            DiscordMessageManager.respondEphemeral(event, "위증 대상을 지정해 주세요.")
            return
        }

        val result = GameManager.castPerjuryVote(
            memberId = event.interaction.user.id,
            targetId = target.id
        )
        DiscordMessageManager.respondEphemeral(event, result.message)
    }

    override suspend fun registerGlobal(kord: Kord) {
        kord.createGlobalChatInputCommand(name, description) {
            applyKoreanLocalization(this)
            user(TARGET_OPTION_NAME, "가짜 투표 대상") {
                required = true
            }
        }
    }

    override suspend fun registerGuild(kord: Kord, guildId: Snowflake) {
        kord.createGuildChatInputCommand(guildId, name, description) {
            applyKoreanLocalization(this)
            user(TARGET_OPTION_NAME, "가짜 투표 대상") {
                required = true
            }
        }
    }
}
