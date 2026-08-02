package org.beobma.mafia42discordproject.game.system

import org.beobma.mafia42discordproject.job.definition.list.Citizen
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import org.beobma.mafia42discordproject.job.evil.list.Spy
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PoliceAutopsyPolicyTest {
    @Test
    fun `only the mafia job has a positive autopsy result`() {
        assertTrue(PoliceAutopsyPolicy.isMafia(Mafia()))
        assertFalse(PoliceAutopsyPolicy.isMafia(Spy()))
        assertFalse(PoliceAutopsyPolicy.isMafia(Citizen()))
        assertFalse(PoliceAutopsyPolicy.isMafia(null))
    }
}
