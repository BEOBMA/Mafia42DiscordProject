package org.beobma.mafia42discordproject.discord

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.createSoundboardSound
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.channel.VoiceChannel
import dev.kord.rest.Sound
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.beobma.mafia42discordproject.game.Game
import kotlin.random.Random

object DiscordVoiceManager {
    private val httpClient = HttpClient(OkHttp)

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

        return runCatching {
            val voiceChannel = game.guild.getChannelOfOrNull<VoiceChannel>(voiceChannelId)
                ?: error("음성 채널을 찾을 수 없습니다.")
            val sound = Sound.Companion.fromUrl(httpClient, source)
            val tempSoundName = "ext-${Random.nextInt(100000, 999999)}"
            val createdSound = game.guild.createSoundboardSound(tempSoundName, sound)
            try {
                createdSound.send(voiceChannel)
            } finally {
                runCatching { createdSound.delete("외부 오디오 재생 후 임시 사운드 정리") }
            }
        }
    }
}
