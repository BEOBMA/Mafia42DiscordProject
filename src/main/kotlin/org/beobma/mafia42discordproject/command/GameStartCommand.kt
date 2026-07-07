package org.beobma.mafia42discordproject.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.interaction.string
import org.beobma.mafia42discordproject.game.GameManager
import org.beobma.mafia42discordproject.game.mode.GameStartMode

object GameStartCommand : DiscordCommand {
    override val name: String = "gamestart"
    override val description: String = "게임을 시작합니다."
    override val koreanName: String = "게임시작"
    override val aliases: Set<String> = setOf("게임시작", "시작")

    private const val MODE_OPTION_NAME = "mode"

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        val mode = GameStartMode.parse(event.interaction.command.strings[MODE_OPTION_NAME])
            ?: GameStartMode.NORMAL
        GameManager.start(event, mode)
    }

    override suspend fun handleMessage(event: MessageCreateEvent, args: List<String>) {
        val rawMode = args.firstOrNull()
        val mode = GameStartMode.parse(rawMode)
        if (rawMode != null && mode == null) {
            event.message.channel.createMessage("사용법: !게임시작 [일반|미치광이]")
            return
        }
        GameManager.start(event, mode ?: GameStartMode.NORMAL)
    }

    private fun dev.kord.rest.builder.interaction.ChatInputCreateBuilder.registerOptions() {
        string(MODE_OPTION_NAME, "게임 모드") {
            required = false
            choice("일반", GameStartMode.NORMAL.optionValue)
            choice("미치광이", GameStartMode.MADNESS.optionValue)
        }
    }

    override suspend fun registerGlobal(kord: Kord) {
        kord.createGlobalChatInputCommand(name, description) {
            applyKoreanLocalization(this)
            registerOptions()
        }
    }

    override suspend fun registerGuild(kord: Kord, guildId: Snowflake) {
        kord.createGuildChatInputCommand(guildId, name, description) {
            applyKoreanLocalization(this)
            registerOptions()
        }
    }
}
