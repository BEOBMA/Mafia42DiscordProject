package org.beobma.mafia42discordproject.job.ability.general.evil.list.witch

import org.beobma.mafia42discordproject.game.loop.NIGHT_DURATION_MS
import kotlin.test.Test
import kotlin.test.assertEquals

class WitchAbilityTest {
    @Test
    fun `normal curse waits until ten seconds remain`() {
        val nightStartedAt = 1_000L
        val now = nightStartedAt + 15_000L

        assertEquals(
            NIGHT_DURATION_MS - 25_000L,
            WitchAbility.delayUntilNormalCurse(nightStartedAt, now)
        )
    }

    @Test
    fun `normal curse applies immediately when less than ten seconds remain`() {
        val nightStartedAt = 1_000L
        val now = nightStartedAt + NIGHT_DURATION_MS - 9_000L

        assertEquals(0L, WitchAbility.delayUntilNormalCurse(nightStartedAt, now))
    }
}
