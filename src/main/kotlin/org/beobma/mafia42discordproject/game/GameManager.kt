package org.beobma.mafia42discordproject.game

import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.channel.VoiceChannel
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import org.beobma.mafia42discordproject.discord.DiscordMessageManager
import org.beobma.mafia42discordproject.game.player.PlayerData

object GameManager {
    private var currentGame: Game? = null

    suspend fun start(event: GuildChatInputCommandInteractionCreateEvent) {
        Game(mutableListOf()).start(event)
    }

    private suspend fun Game.start(event: GuildChatInputCommandInteractionCreateEvent) {
        val interaction = event.interaction
        if (currentGame != null) {
            DiscordMessageManager.respondEphemeral(event, "이미 게임이 진행 중입니다.")
            return
        }

        val guild = interaction.guild
        val commandSender = interaction.user
        val voiceChannelId = commandSender.getVoiceStateOrNull()?.channelId ?: run {
            DiscordMessageManager.respondEphemeral(event, "현재 음성채널에 들어가 있지 않습니다.")
            return
        }
        val voiceChannel = guild.getChannelOfOrNull<VoiceChannel>(voiceChannelId) ?: run {
            DiscordMessageManager.respondEphemeral(event, "음성채널 정보를 가져오지 못했습니다.")
            return
        }

        val membersInSameVoice = guild.members
            .filter { guildMember ->
                guildMember.getVoiceStateOrNull()?.channelId == voiceChannelId
            }
            .toList()

        currentGame = this
        this.playerDatas = membersInSameVoice.map(::PlayerData).toMutableList()

        DiscordMessageManager.respondPublic(
            event,
            buildString {
                appendLine("현재 음성채널: ${voiceChannel.mention}")
                appendLine("인원 수: ${membersInSameVoice.size}")
                appendLine()
                append(DiscordMessageManager.mentions(membersInSameVoice))
            }
        )
    }

    suspend fun stop(event: GuildChatInputCommandInteractionCreateEvent) {
        if (currentGame == null) {
            DiscordMessageManager.respondEphemeral(event, "진행 중인 게임이 없습니다.")
            return
        }
        currentGame = null

        val mention = DiscordMessageManager.mention(event.interaction.user)
        DiscordMessageManager.respondPublic(event, "${mention}이(가) 게임을 종료했습니다.")
    }
}
