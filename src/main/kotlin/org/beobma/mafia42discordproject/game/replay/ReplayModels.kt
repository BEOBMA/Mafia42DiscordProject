package org.beobma.mafia42discordproject.game.replay

import dev.kord.common.entity.Snowflake
import org.beobma.mafia42discordproject.game.GamePhase

enum class ReplayLogType {
    GAME_START,
    GAME_END,
    PHASE_START,
    CHAT_PUBLIC,
    CHAT_MAFIA,
    CHAT_COUPLE,
    CHAT_DEAD,
    ABILITY_USED,
    VOTE_CAST,
    PROS_CONS_VOTE,
    DIRECT_MESSAGE,
    EPHEMERAL,
    SYSTEM_RESULT,
    DEATH,
}

enum class ReplayVisibility {
    PUBLIC,
    MAFIA_CHANNEL,
    COUPLE_CHANNEL,
    DEAD_CHANNEL,
    DIRECT_MESSAGE,
    EPHEMERAL,
    SYSTEM_INTERNAL
}

data class ReplayRecipient(
    val id: Snowflake?,
    val name: String,
    val scope: ReplayVisibility
)

data class ReplayLogEntry(
    val sequence: Long,
    val timestampMillis: Long,
    val dayCount: Int,
    val phase: GamePhase,
    val type: ReplayLogType,
    val actorId: Snowflake?,
    val actorName: String?,
    val actorJobName: String?,
    val recipients: List<ReplayRecipient>,
    val visibility: ReplayVisibility,
    val title: String,
    val body: String,
    val imageUrls: List<String> = emptyList(),
    val relatedEventId: String? = null
)
