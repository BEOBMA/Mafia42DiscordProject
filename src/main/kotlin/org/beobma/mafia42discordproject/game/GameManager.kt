package org.beobma.mafia42discordproject.game

import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.channel.VoiceChannel
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import org.beobma.mafia42discordproject.game.player.PlayerData

object GameManager {

    suspend fun Game.start(event: GuildChatInputCommandInteractionCreateEvent) {
        val interaction = event.interaction
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

        val response = interaction.deferEphemeralResponse()
        response.respond {
            content = buildString {
                appendLine("현재 음성채널: ${voiceChannel.mention}")
                appendLine("인원 수: ${membersInSameVoice.size}")
                appendLine()
                append(
                    membersInSameVoice.joinToString("\n") { "• ${it.nickname}" }
                )
            }
        }

        this.playerDatas = membersInSameVoice.map { member -> PlayerData(member) }.toMutableList()
    }
}