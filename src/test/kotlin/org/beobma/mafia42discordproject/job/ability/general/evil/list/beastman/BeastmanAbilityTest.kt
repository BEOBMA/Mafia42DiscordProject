package org.beobma.mafia42discordproject.job.ability.general.evil.list.beastman

import org.beobma.mafia42discordproject.game.system.AttackTier
import org.beobma.mafia42discordproject.game.system.DefenseTier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BeastmanAbilityTest {
    @Test
    fun `tamed execution uses ordinary mafia attack tier`() {
        assertEquals(AttackTier.NORMAL, BeastmanAbility.TAMED_EXECUTION_ATTACK_TIER)
    }

    @Test
    fun `tamed execution is blocked by healing and bulletproof defense`() {
        val attackTier = BeastmanAbility.TAMED_EXECUTION_ATTACK_TIER

        assertTrue(DefenseTier.NORMAL.level >= attackTier.level)
        assertTrue(DefenseTier.ABSOLUTE.level >= attackTier.level)
    }
}
