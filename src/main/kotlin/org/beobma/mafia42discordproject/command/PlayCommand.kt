package org.beobma.mafia42discordproject.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.string

import org.beobma.mafia42discordproject.discord.DiscordMessageManager
import org.beobma.mafia42discordproject.lavalink.LavalinkManager

object PlayCommand : DiscordCommand {
    override val name: String = "play"
    override val description: String = "로컬 오디오 파일 경로로 음악을 재생합니다."
    override val koreanName: String = "재생"

    private const val pathOption = "path"

    override suspend fun registerGlobal(kord: Kord) {
        kord.createGlobalChatInputCommand(name, description) {
            applyKoreanLocalization(this)
            string(pathOption, "재생할 로컬 파일 경로") {
                required = true
            }
        }
    }

    override suspend fun registerGuild(kord: Kord, guildId: Snowflake) {
        kord.createGuildChatInputCommand(guildId, name, description) {
            applyKoreanLocalization(this)
            string(pathOption, "재생할 로컬 파일 경로") {
                required = true
            }
        }
    }

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        val guild = event.interaction.getGuild()
        val member = event.interaction.user.asMember(guild.id)
        val voiceState = member.getVoiceStateOrNull()
        val voiceChannel = voiceState?.getChannelOrNull()

        if (voiceChannel == null) {
            DiscordMessageManager.respondEphemeral(event, "먼저 음성 채널에 접속한 뒤 /play 명령어를 사용해 주세요.")
            return
        }

        val filePath = event.interaction.command.strings[pathOption]?.trim().orEmpty()
        if (filePath.isBlank()) {
            DiscordMessageManager.respondEphemeral(event, "재생할 로컬 파일 경로를 입력해 주세요.")
            return
        }

        val deferredResponse = event.interaction.deferPublicResponse()
        val result = LavalinkManager.play(
            kord = event.kord,
            guildId = guild.id,
            voiceChannelId = voiceChannel.id,
            source = filePath
        )

        deferredResponse.respond {
            content = result.message
        }
    }
}
