package org.beobma.mafia42discordproject.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.string
import org.beobma.mafia42discordproject.discord.DiscordMessageManager
import org.beobma.mafia42discordproject.lavalink.LavalinkManager

object PlayCommand : DiscordCommand {
    override val name: String = "play"
    override val description: String = "URL 또는 키워드로 음악을 재생합니다."
    override val koreanName: String = "재생"

    private const val queryOption = "query"

    override suspend fun registerGlobal(kord: Kord) {
        kord.createGlobalChatInputCommand(name, description) {
            applyKoreanLocalization(this)
            string(queryOption, "재생할 URL 또는 키워드") {
                required = true
            }
        }
    }

    override suspend fun registerGuild(kord: Kord, guildId: Snowflake) {
        kord.createGuildChatInputCommand(guildId, name, description) {
            applyKoreanLocalization(this)
            string(queryOption, "재생할 URL 또는 키워드") {
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

        val query = event.interaction.command.strings[queryOption]?.trim().orEmpty()
        if (query.isBlank()) {
            DiscordMessageManager.respondEphemeral(event, "재생할 URL 또는 검색어를 입력해 주세요.")
            return
        }

        val result = LavalinkManager.play(guild, voiceChannel, query)
        if (result.success) {
            DiscordMessageManager.respondPublic(event, result.message)
            return
        }

        DiscordMessageManager.respondEphemeral(event, result.message)
    }
}
