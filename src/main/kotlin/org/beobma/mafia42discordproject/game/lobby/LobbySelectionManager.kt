package org.beobma.mafia42discordproject.game.lobby

import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Member
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.toList
import org.beobma.mafia42discordproject.discord.DiscordMessageManager
import java.util.concurrent.ConcurrentHashMap

internal class LobbySelectionManager {
    private val selectionsByLobby: MutableMap<LobbyKey, MutableMap<Snowflake, LobbyParticipation>> = ConcurrentHashMap()

    suspend fun markParticipation(
        guild: Guild,
        member: Member,
        participation: LobbyParticipation
    ): LobbyParticipationResult {
        val voiceChannelId = member.getVoiceStateOrNull()?.channelId
            ?: return LobbyParticipationResult(false, "먼저 음성채널에 접속한 뒤 사용해 주세요.")

        clearSelectionForMember(guild.id, member.id)
        val key = LobbyKey(guild.id, voiceChannelId)
        val selections = selectionsByLobby.getOrPut(key) { ConcurrentHashMap() }
        selections[member.id] = participation

        val lobbyMembers = collectMembers(guild, voiceChannelId)
        val action = when (participation) {
            LobbyParticipation.READY -> "플레이어로 준비했습니다."
            LobbyParticipation.SPECTATOR -> "관전자로 등록했습니다."
        }

        return LobbyParticipationResult(
            true,
            buildString {
                appendLine(action)
                append("현재 선택: 플레이어 ${lobbyMembers.readyMembers.size}명, 관전자 ${lobbyMembers.spectatorMembers.size}명, 미선택 ${lobbyMembers.undecidedMembers.size}명")
                if (lobbyMembers.undecidedMembers.isNotEmpty()) {
                    appendLine()
                    appendLine("아직 선택하지 않은 인원:")
                    append(DiscordMessageManager.mentions(lobbyMembers.undecidedMembers))
                }
            }
        )
    }

    suspend fun collectMembers(guild: Guild, voiceChannelId: Snowflake): VoiceLobbyMembers {
        val membersInSameVoice = guild.members
            .filter { guildMember ->
                guildMember.getVoiceStateOrNull()?.channelId == voiceChannelId
            }
            .filterNot { it.isBot }
            .toList()
        val selections = selectionsByLobby[LobbyKey(guild.id, voiceChannelId)].orEmpty()

        return VoiceLobbyMembers(
            allVoiceMembers = membersInSameVoice,
            readyMembers = membersInSameVoice.filter { member -> selections[member.id] == LobbyParticipation.READY },
            spectatorMembers = membersInSameVoice.filter { member -> selections[member.id] == LobbyParticipation.SPECTATOR },
            undecidedMembers = membersInSameVoice.filter { member -> selections[member.id] == null }
        )
    }

    fun clearSelections(guildId: Snowflake, voiceChannelId: Snowflake) {
        selectionsByLobby.remove(LobbyKey(guildId, voiceChannelId))
    }

    private fun clearSelectionForMember(guildId: Snowflake, memberId: Snowflake) {
        selectionsByLobby
            .filterKeys { key -> key.guildId == guildId }
            .values
            .forEach { selections -> selections.remove(memberId) }
        removeEmptySelections()
    }

    private fun removeEmptySelections() {
        selectionsByLobby
            .filterValues { selections -> selections.isEmpty() }
            .keys
            .forEach { key -> selectionsByLobby.remove(key) }
    }
}
