package org.beobma.mafia42discordproject.game.system

import org.beobma.mafia42discordproject.job.definition.list.Citizen
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import org.beobma.mafia42discordproject.job.evil.list.Spy
import org.beobma.mafia42discordproject.job.evil.list.Villain
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PoliceSearchPolicyTest {
    @Test
    fun `only mafia is reported as mafia`() {
        assertTrue(PoliceSearchPolicy.isMafia(Mafia()))
        assertFalse(PoliceSearchPolicy.isMafia(Spy()))
        assertFalse(PoliceSearchPolicy.isMafia(Villain()))
        assertFalse(PoliceSearchPolicy.isMafia(Citizen()))
        assertFalse(PoliceSearchPolicy.isMafia(null))
    }

    @Test
    fun `frog investigation overrides are preserved`() {
        assertFalse(PoliceSearchPolicy.isMafia(Mafia(), isFrogCursed = true))
        assertTrue(
            PoliceSearchPolicy.isMafia(
                Citizen(),
                isFrogCursed = true,
                hallucinatedAsMafia = true
            )
        )
    }
}
