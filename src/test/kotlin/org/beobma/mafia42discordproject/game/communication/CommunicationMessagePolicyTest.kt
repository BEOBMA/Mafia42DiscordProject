package org.beobma.mafia42discordproject.game.communication

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommunicationMessagePolicyTest {
    @Test
    fun `messages are truncated at each communication limit`() {
        assertEquals("1234567891", "12345678910".truncateToCharacterLimit(MEGAPHONE_MAX_CHARACTERS))
        assertEquals("12345678901234567890", "123456789012345678901".truncateToCharacterLimit(SECRET_LETTER_MAX_CHARACTERS))
        assertEquals("가".repeat(WILL_MAX_CHARACTERS), "가".repeat(WILL_MAX_CHARACTERS + 1).truncateToCharacterLimit(WILL_MAX_CHARACTERS))
    }

    @Test
    fun `spaces and unicode code points count as characters`() {
        assertEquals("12 34", "12 345".truncateToCharacterLimit(5))
        assertEquals("😀😀", "😀😀😀".truncateToCharacterLimit(2))
    }

    @Test
    fun `night communication closes at exactly fifteen seconds remaining`() {
        val nightEndsAtMillis = 100_000L

        assertTrue(isNightCommunicationSubmissionOpen(84_999L, nightEndsAtMillis))
        assertFalse(isNightCommunicationSubmissionOpen(85_000L, nightEndsAtMillis))
        assertFalse(isNightCommunicationSubmissionOpen(85_001L, nightEndsAtMillis))
    }
}
