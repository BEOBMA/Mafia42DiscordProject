package org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia

import org.beobma.mafia42discordproject.game.system.AttackTier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MafiaAbilityTest {
    @Test
    fun `couple sacrifice target becomes effective execution target`() {
        var hackerRedirectWasEvaluated = false

        val effectiveTarget = MafiaAbility.resolveExecutionTarget(
            originalTarget = "A",
            coupleRedirectedTarget = "B",
            bypassHackerProxy = false
        ) {
            hackerRedirectWasEvaluated = true
            "C"
        }

        assertEquals("B", effectiveTarget)
        assertFalse(hackerRedirectWasEvaluated)
    }

    @Test
    fun `activated successor execution uses sniper attack tier`() {
        assertEquals(
            AttackTier.PIERCE,
            MafiaAbility.applySuccessorAttackTier(
                currentTier = AttackTier.NORMAL,
                isActivatedSuccessorExecution = true
            )
        )
    }

    @Test
    fun `sniper enhanced execution bypasses hacker proxy`() {
        assertTrue(
            MafiaAbility.shouldBypassHackerProxy(
                isThiefExecution = false,
                isSniperEnhanced = true
            )
        )
    }

    @Test
    fun `thief execution bypasses hacker proxy without sniper`() {
        assertTrue(
            MafiaAbility.shouldBypassHackerProxy(
                isThiefExecution = true,
                isSniperEnhanced = false
            )
        )
    }

    @Test
    fun `ordinary mafia execution still follows hacker proxy`() {
        assertFalse(
            MafiaAbility.shouldBypassHackerProxy(
                isThiefExecution = false,
                isSniperEnhanced = false
            )
        )
    }
}
