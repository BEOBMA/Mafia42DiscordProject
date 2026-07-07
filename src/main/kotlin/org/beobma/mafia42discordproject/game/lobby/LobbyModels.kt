package org.beobma.mafia42discordproject.game.lobby

import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Member

internal enum class LobbyParticipation {
    READY,
    SPECTATOR
}

internal data class LobbyKey(
    val guildId: Snowflake,
    val voiceChannelId: Snowflake
)

internal data class VoiceLobbyMembers(
    val allVoiceMembers: List<Member>,
    val readyMembers: List<Member>,
    val spectatorMembers: List<Member>,
    val undecidedMembers: List<Member>
)

internal data class LobbyRefreshResult(
    val members: VoiceLobbyMembers,
    val removedSelectionCount: Int
)

data class LobbyParticipationResult(
    val isSuccess: Boolean,
    val message: String
)
