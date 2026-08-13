package org.beobma.mafia42discordproject.game.system

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeathAbilitySuppressionPolicyTest {
    @Test
    fun probationSuppressesBelongingsButPreservesAStoredWill() {
        assertTrue(DeathAbilitySuppressionPolicy.shouldSuppressBelongings(wasConvertedByProbation = true))
        assertTrue(
            DeathAbilitySuppressionPolicy.shouldAnnounceWill(
                hasStoredWill = true,
                hasCurrentWillAbility = false,
                wasConvertedByProbation = true
            )
        )
    }

    @Test
    fun exorcismSuppressesWillWithoutSuppressingEarthboundTargets() {
        assertFalse(DeathAbilitySuppressionPolicy.shouldSuppressBelongings(wasConvertedByProbation = false))
        assertTrue(
            DeathAbilitySuppressionPolicy.shouldSuppressWillByExorcism(
                attackerHasExorcism = true,
                targetIsEvil = false,
                targetHasEarthboundSpirit = false
            )
        )
        assertFalse(
            DeathAbilitySuppressionPolicy.shouldSuppressWillByExorcism(
                attackerHasExorcism = true,
                targetIsEvil = false,
                targetHasEarthboundSpirit = true
            )
        )
    }

    @Test
    fun aStoredWillStillRequiresTheAbilityOrProbationConversion() {
        assertFalse(
            DeathAbilitySuppressionPolicy.shouldAnnounceWill(
                hasStoredWill = true,
                hasCurrentWillAbility = false,
                wasConvertedByProbation = false
            )
        )
    }

    @Test
    fun aCurrentWillAbilityAnnouncesAnEmptyWillUnlessSuppressed() {
        assertTrue(
            DeathAbilitySuppressionPolicy.shouldAnnounceWill(
                hasStoredWill = false,
                hasCurrentWillAbility = true,
                wasConvertedByProbation = false
            )
        )
        assertFalse(
            DeathAbilitySuppressionPolicy.shouldAnnounceWill(
                hasStoredWill = false,
                hasCurrentWillAbility = true,
                wasConvertedByProbation = false,
                isWillSuppressed = true
            )
        )
        assertEquals("''", DeathAbilitySuppressionPolicy.displayedWillContent(null))
        assertEquals("작성한 유언", DeathAbilitySuppressionPolicy.displayedWillContent("작성한 유언"))
    }
}
