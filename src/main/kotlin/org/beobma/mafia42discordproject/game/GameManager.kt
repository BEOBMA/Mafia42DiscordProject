package org.beobma.mafia42discordproject.game

import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.channel.VoiceChannel
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import org.beobma.mafia42discordproject.game.player.PlayerData

object GameManager {
    private var currentGame: Game? = null

    suspend fun start(event: GuildChatInputCommandInteractionCreateEvent) {
        Game(mutableListOf()).start(event)
    }
    suspend fun Game.start(event: GuildChatInputCommandInteractionCreateEvent) {
        val interaction = event.interaction
        if (currentGame != null) {
            val response = interaction.deferEphemeralResponse()
            response.respond {
                content = "이미 게임이 진행 중입니다."
            }
            return
        }
        currentGame = this
        val guild = interaction.guild
        val commandSender = event.interaction.user
        val voiceChannelId = commandSender.getVoiceStateOrNull()?.channelId ?: run {
            val response = interaction.deferEphemeralResponse()
            response.respond {
                content = "현재 음성채널에 들어가 있지 않습니다."
            }
            return
        }
        val voiceChannel = guild.getChannelOfOrNull<VoiceChannel>(voiceChannelId) ?: run {
            val response = interaction.deferEphemeralResponse()
            response.respond {
                content = "음성채널 정보를 가져오지 못했습니다."
            }
            return
        }

        val membersInSameVoice = guild.members
            .filter { guildMember ->
                guildMember.getVoiceStateOrNull()?.channelId == voiceChannelId
            }
            .toList()

        interaction.respondPublic {
            content = buildString {
                appendLine("현재 음성채널: ${voiceChannel.mention}")
                appendLine("인원 수: ${membersInSameVoice.size}")
                appendLine()
                append(
                    membersInSameVoice.joinToString("\n") { "• ${it.mention}" }
                )
            }
        }

        this.playerDatas = membersInSameVoice.map { member -> PlayerData(member) }.toMutableList()
    }

    suspend fun stop(event: GuildChatInputCommandInteractionCreateEvent) {
        val interaction = event.interaction
        if (currentGame == null) {
            val response = interaction.deferEphemeralResponse()
            response.respond {
                content = "진행 중인 게임이 없습니다."
            }
            return
        }
        currentGame = null

        interaction.respondPublic {
            content = "${event.interaction.user.mention}이(가) 게임을 종료했습니다."
        }
    }
}
