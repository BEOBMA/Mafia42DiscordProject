package org.beobma.mafia42discordproject.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.interaction.string
import org.beobma.mafia42discordproject.discord.DiscordMessageManager
import org.beobma.mafia42discordproject.game.GameManager

object PlaySoundCommand : DiscordCommand {
    override val name: String = "playsound"
    override val description: String = "외부 오디오 URL을 재생합니다."
    override val koreanName: String = "소리재생"
    override val aliases: Set<String> = setOf("소리재생")

    private const val sourceOptionName = "source"

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        val source = event.interaction.command.strings[sourceOptionName]?.trim()
        if (source.isNullOrBlank()) {
            DiscordMessageManager.respondEphemeral(event, "사용법: /playsound source:<외부 오디오 URL>")
            return
        }

        val result = GameManager.playSound(source)
        if (result.isSuccess) {
            DiscordMessageManager.respondEphemeral(event, "사운드 재생 완료: `$source`")
            return
        }

        val reason = result.exceptionOrNull()?.message ?: "알 수 없는 오류"
        DiscordMessageManager.respondEphemeral(event, "사운드 재생 실패: `$source`\n사유: $reason")
    }

    override suspend fun handleMessage(event: MessageCreateEvent, args: List<String>) {
        val source = args.joinToString(" ").trim()
        if (source.isBlank()) {
            event.message.channel.createMessage("사용법: !playsound <외부 오디오 URL>")
            return
        }

        val result = GameManager.playSound(source)
        if (result.isSuccess) {
            event.message.channel.createMessage("사운드 재생 완료: `$source`")
            return
        }

        val reason = result.exceptionOrNull()?.message ?: "알 수 없는 오류"
        event.message.channel.createMessage("사운드 재생 실패: `$source`\n사유: $reason")
    }

    private fun dev.kord.rest.builder.interaction.ChatInputCreateBuilder.registerOptions() {
        string(sourceOptionName, "재생할 외부 오디오 URL") {
            required = true
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
