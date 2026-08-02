package org.beobma.mafia42discordproject.web

import dev.kord.common.entity.Snowflake
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.replay.ReplayLogEntry
import org.beobma.mafia42discordproject.game.replay.ReplayLogType
import org.beobma.mafia42discordproject.game.replay.ReplayRecipient
import org.beobma.mafia42discordproject.game.replay.ReplayVisibility

class LiveEventVisibilityTest {
    private val viewerId = Snowflake(42uL)

    @Test
    fun publicSystemEventsAreVisibleButPlayerChatIsNot() {
        assertTrue(LiveEventVisibility.canView(entry(), viewerId))
        assertFalse(
            LiveEventVisibility.canView(
                entry(type = ReplayLogType.CHAT_PUBLIC, title = "일반 채팅"),
                viewerId
            )
        )
    }

    @Test
    fun privateAndFactionEventsRequireViewerRecipient() {
        val recipient = ReplayRecipient(viewerId, "플레이어", ReplayVisibility.DIRECT_MESSAGE)
        assertTrue(
            LiveEventVisibility.canView(
                entry(visibility = ReplayVisibility.DIRECT_MESSAGE, recipients = listOf(recipient)),
                viewerId
            )
        )
        assertFalse(
            LiveEventVisibility.canView(
                entry(visibility = ReplayVisibility.DIRECT_MESSAGE),
                viewerId
            )
        )
        assertTrue(
            LiveEventVisibility.canView(
                entry(
                    visibility = ReplayVisibility.MAFIA_CHANNEL,
                    recipients = listOf(recipient.copy(scope = ReplayVisibility.MAFIA_CHANNEL))
                ),
                viewerId
            )
        )
    }

    @Test
    fun internalAndPlayerAuthoredMessagesStayHidden() {
        assertFalse(
            LiveEventVisibility.canView(
                entry(visibility = ReplayVisibility.SYSTEM_INTERNAL),
                viewerId
            )
        )
        assertFalse(LiveEventVisibility.canView(entry(title = "확성기"), viewerId))
        assertFalse(LiveEventVisibility.canView(entry(title = "밀서 배달"), viewerId))
    }

    private fun entry(
        type: ReplayLogType = ReplayLogType.SYSTEM_RESULT,
        visibility: ReplayVisibility = ReplayVisibility.PUBLIC,
        title: String = "시스템 안내",
        recipients: List<ReplayRecipient> = emptyList()
    ) = ReplayLogEntry(
        sequence = 1,
        timestampMillis = 1,
        dayCount = 1,
        phase = GamePhase.NIGHT,
        type = type,
        actorId = null,
        actorName = null,
        actorJobName = null,
        recipients = recipients,
        visibility = visibility,
        title = title,
        body = "내용"
    )
}
