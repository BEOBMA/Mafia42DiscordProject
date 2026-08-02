package org.beobma.mafia42discordproject.game

import kotlin.test.Test
import kotlin.test.assertTrue

class CommunicationRestrictionTest {
    @Test
    fun `living player cannot speak after redirected soul release`() {
        assertTrue(
            GameLoopManager.shouldRestrictCommunication(
                isDead = false,
                isShamaned = true,
                isSilenced = false,
                isMadScientistDistortionHidden = false
            )
        )
    }
}
