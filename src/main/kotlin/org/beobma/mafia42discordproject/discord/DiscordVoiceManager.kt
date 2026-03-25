package org.beobma.mafia42discordproject.discord

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.channel.VoiceChannel
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import org.beobma.mafia42discordproject.game.Game

object DiscordVoiceManager {
    private val httpClient = HttpClient(OkHttp)
    private val externalPlayerBaseUrl = System.getenv("EXTERNAL_AUDIO_PLAYER_URL")?.trim()?.trimEnd('/')
    private val externalPlayerToken = System.getenv("EXTERNAL_AUDIO_PLAYER_TOKEN")?.trim()?.takeIf { it.isNotBlank() }
    private val externalPlayerPath = System.getenv("EXTERNAL_AUDIO_PLAYER_PATH")?.trim()?.ifBlank { null } ?: "/play"

    suspend fun moveBotToVoiceChannel(guild: GuildBehavior, voiceChannelId: Snowflake): Result<Unit> {
        return runCatching {
            val selfMember = guild.getMember(guild.kord.selfId)
            selfMember.edit {
                this.voiceChannelId = voiceChannelId
            }
        }
    }

    suspend fun playExternalSound(game: Game, source: String): Result<Unit> {
        val voiceChannelId = game.gameVoiceChannelId
            ?: return Result.failure(IllegalStateException("게임 음성 채널 정보가 없습니다."))
        return playExternalSound(game.guild, voiceChannelId, source)
    }

    suspend fun playExternalSound(guild: GuildBehavior, voiceChannelId: Snowflake, source: String): Result<Unit> {
        val externalApiUrl = externalPlayerBaseUrl
            ?: return Result.failure(
                IllegalStateException(
                    "외부 재생 API가 설정되지 않았습니다. EXTERNAL_AUDIO_PLAYER_URL 환경 변수를 설정해 주세요. " +
                        "(예: https://audio-player.example.com)"
                )
            )

        return runCatching {
            guild.getChannelOfOrNull<VoiceChannel>(voiceChannelId)
                ?: error("음성 채널을 찾을 수 없습니다.")

            val response = httpClient.post("$externalApiUrl$externalPlayerPath") {
                contentType(ContentType.Application.Json)
                externalPlayerToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                setBody(
                    ExternalAudioPlayRequest(
                        guildId = guild.id.toString(),
                        voiceChannelId = voiceChannelId.toString(),
                        source = source
                    )
                )
            }

            if (!response.status.isSuccess()) {
                val responseBody = response.bodyAsText().ifBlank { "응답 본문 없음" }
                error(
                    "외부 오디오 재생 API 호출 실패: url=$externalApiUrl$externalPlayerPath, " +
                        "status=${response.status.value}, body=$responseBody"
                )
            }
        }
    }

    @Serializable
    private data class ExternalAudioPlayRequest(
        val guildId: String,
        val voiceChannelId: String,
        val source: String
    )
}
