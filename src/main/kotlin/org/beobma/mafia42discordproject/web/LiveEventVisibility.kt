package org.beobma.mafia42discordproject.web

import dev.kord.common.entity.Snowflake
import org.beobma.mafia42discordproject.game.replay.ReplayLogEntry
import org.beobma.mafia42discordproject.game.replay.ReplayLogType
import org.beobma.mafia42discordproject.game.replay.ReplayVisibility

internal object LiveEventVisibility {
    private val chatTypes = setOf(
        ReplayLogType.CHAT_PUBLIC,
        ReplayLogType.CHAT_MAFIA,
        ReplayLogType.CHAT_COUPLE,
        ReplayLogType.CHAT_DEAD
    )
    private val playerAuthoredTitles = setOf(
        "확성기", "밀서 작성", "밀서 배달", "유언", "유언 작성", "암구호"
    )

    fun canView(entry: ReplayLogEntry, viewerId: Snowflake): Boolean {
        if (entry.type in chatTypes) return false
        if (entry.title in playerAuthoredTitles || entry.title.startsWith("밀서")) return false

        return when (entry.visibility) {
            ReplayVisibility.PUBLIC -> true
            ReplayVisibility.DIRECT_MESSAGE,
            ReplayVisibility.EPHEMERAL,
            ReplayVisibility.MAFIA_CHANNEL,
            ReplayVisibility.COUPLE_CHANNEL,
            ReplayVisibility.DEAD_CHANNEL -> entry.recipients.any { it.id == viewerId }
            ReplayVisibility.SYSTEM_INTERNAL -> false
        }
    }
}
