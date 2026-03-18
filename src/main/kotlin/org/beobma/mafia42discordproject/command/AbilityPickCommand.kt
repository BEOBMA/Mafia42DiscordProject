package org.beobma.mafia42discordproject.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.interaction.integer
import org.beobma.mafia42discordproject.discord.DiscordMessageManager
import org.beobma.mafia42discordproject.game.GameManager

object AbilityPickCommand : DiscordCommand {
    override val name: String = "abilitypick"
    override val description: String = "부가 능력 선택 단계에서 제시된 능력 번호를 선택합니다."

    private const val pickNumberOption = "번호"

    override suspend fun registerGlobal(kord: Kord) {
        kord.createGlobalChatInputCommand(name, description) {
            integer(pickNumberOption, "현재 제시된 능력 번호 (1~3)") {
                required = true
                choice("1번", 1)
                choice("2번", 2)
                choice("3번", 3)
            }
        }
    }

    override suspend fun registerGuild(kord: Kord, guildId: Snowflake) {
        kord.createGuildChatInputCommand(guildId, name, description) {
            integer(pickNumberOption, "현재 제시된 능력 번호 (1~3)") {
                required = true
                choice("1번", 1)
                choice("2번", 2)
                choice("3번", 3)
            }
        }
    }

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        val pickNumber = event.interaction.command.integers[pickNumberOption]?.toInt()
        if (pickNumber == null) {
            DiscordMessageManager.respondEphemeral(event, "선택 번호를 확인할 수 없습니다. 다시 시도해 주세요.")
            return
        }

        val resultMessage = GameManager.selectExtraAbility(event.interaction.user.id, pickNumber)
        DiscordMessageManager.respondEphemeral(event, resultMessage)
    }

    override suspend fun handleMessage(event: MessageCreateEvent, args: List<String>) {
        val userId = event.message.author?.id ?: return
        val pickNumber = args.firstOrNull()?.toIntOrNull()

        if (pickNumber == null) {
            DiscordMessageManager.sendChannelMessage(
                event.message.channel,
                "${event.message.author?.mention.orEmpty()} 사용법: `!abilitypick <1|2|3>`"
            )
            return
        }

        val resultMessage = GameManager.selectExtraAbility(userId, pickNumber)
        DiscordMessageManager.sendChannelMessage(event.message.channel, "${event.message.author?.mention.orEmpty()} $resultMessage")
    }
}
