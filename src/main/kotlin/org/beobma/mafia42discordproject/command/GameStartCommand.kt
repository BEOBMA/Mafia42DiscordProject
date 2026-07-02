package org.beobma.mafia42discordproject.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.interaction.string
import org.beobma.mafia42discordproject.game.GameManager

object GameStartCommand : DiscordCommand {
    override val name: String = "gamestart"
    override val description: String = "게임을 시작합니다."
    override val koreanName: String = "게임시작"
    override val aliases: Set<String> = setOf("게임시작", "시작")

    private const val modeOptionName = "mode"

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        val mode = GameManager.GameStartMode.parse(event.interaction.command.strings[modeOptionName])
            ?: GameManager.GameStartMode.NORMAL
        GameManager.start(event, mode)
    }

    override suspend fun handleMessage(event: MessageCreateEvent, args: List<String>) {
        val rawMode = args.firstOrNull()
        val mode = GameManager.GameStartMode.parse(rawMode)
        if (rawMode != null && mode == null) {
            event.message.channel.createMessage("사용법: !게임시작 [일반|미치광이]")
            return
        }
        GameManager.start(event, mode ?: GameManager.GameStartMode.NORMAL)
    }

    private fun dev.kord.rest.builder.interaction.ChatInputCreateBuilder.registerOptions() {
        string(modeOptionName, "게임 모드") {
            required = false
            choice("일반", GameManager.GameStartMode.NORMAL.optionValue)
            choice("미치광이", GameManager.GameStartMode.MADNESS.optionValue)
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
