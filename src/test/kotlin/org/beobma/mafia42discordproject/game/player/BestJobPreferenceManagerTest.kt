package org.beobma.mafia42discordproject.game.player

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BestJobPreferenceManagerTest {
    @Test
    fun mafiaIsExcludedFromFixedBestJobCandidates() {
        val userIdWithoutPreferences = ULong.MAX_VALUE

        assertFalse(BestJobPreferenceManager.isAllowedJob(userIdWithoutPreferences, "마피아"))
        assertTrue(BestJobPreferenceManager.isAllowedJob(userIdWithoutPreferences, "의사"))
    }
}
