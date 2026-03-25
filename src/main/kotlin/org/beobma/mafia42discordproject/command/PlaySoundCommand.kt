package org.beobma.mafia42discordproject.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.interaction.string
import org.beobma.mafia42discordproject.discord.InteractionErrorHandler
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

        val guild = event.interaction.guild
        val voiceChannelId = event.interaction.user.getVoiceStateOrNull()?.channelId
        if (voiceChannelId == null) {
            DiscordMessageManager.respondEphemeral(event, "음성 채널에 먼저 입장해 주세요.")
            return
        }

        val deferred = event.interaction.deferEphemeralResponse()

        val result = GameManager.playSound(source, guild, voiceChannelId)
        if (result.isSuccess) {
            InteractionErrorHandler.runSafely("slash-ephemeral:${event.interaction.command.rootName}") {
                deferred.respond {
                    this.content = "사운드 재생 완료: `$source`"
                }
            }
            return
        }

        val reason = result.exceptionOrNull()?.message ?: "알 수 없는 오류"
        InteractionErrorHandler.runSafely("slash-ephemeral:${event.interaction.command.rootName}") {
            deferred.respond {
                this.content = "사운드 재생 실패: `$source`\n사유: $reason"
            }
        }
    }

    override suspend fun handleMessage(event: MessageCreateEvent, args: List<String>) {
        val source = args.joinToString(" ").trim()
        if (source.isBlank()) {
            event.message.channel.createMessage("사용법: !playsound <외부 오디오 URL>")
            return
        }

        val guild = event.getGuildOrNull() ?: run {
            event.message.channel.createMessage("길드에서만 사용할 수 있습니다.")
            return
        }
        val author = event.message.author?.asMemberOrNull(guild.id)
        val voiceChannelId = author?.getVoiceStateOrNull()?.channelId
        if (voiceChannelId == null) {
            event.message.channel.createMessage("음성 채널에 먼저 입장해 주세요.")
            return
        }

        val result = GameManager.playSound(source, guild, voiceChannelId)
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
