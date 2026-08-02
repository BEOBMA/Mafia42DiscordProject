package org.beobma.mafia42discordproject.game.system

import org.beobma.mafia42discordproject.job.definition.list.Citizen
import org.beobma.mafia42discordproject.job.definition.list.Prophet
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import org.beobma.mafia42discordproject.job.evil.list.Witch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProphetApostlePolicyTest {
    @Test
    fun `frog cursed citizen is excluded from apostle citizen survivors`() {
        assertFalse(
            ProphetApostlePolicy.isCitizenTeamSurvivor(
                job = Citizen(),
                isDead = false,
                isFrogCursed = true
            )
        )
    }

    @Test
    fun `prophet is the only citizen survivor when another citizen is frog cursed`() {
        val citizenSurvivorCount = listOf(
            Survivor(Prophet(), isFrogCursed = false),
            Survivor(Citizen(), isFrogCursed = true),
            Survivor(Mafia(), isFrogCursed = false),
            Survivor(Witch(), isFrogCursed = false)
        ).count { survivor ->
            ProphetApostlePolicy.isCitizenTeamSurvivor(
                job = survivor.job,
                isDead = false,
                isFrogCursed = survivor.isFrogCursed
            )
        }

        assertEquals(1, citizenSurvivorCount)
    }

    @Test
    fun `uncursed living citizen remains an apostle citizen survivor`() {
        assertTrue(
            ProphetApostlePolicy.isCitizenTeamSurvivor(
                job = Citizen(),
                isDead = false,
                isFrogCursed = false
            )
        )
    }

    @Test
    fun `dead citizen is excluded from apostle citizen survivors`() {
        assertFalse(
            ProphetApostlePolicy.isCitizenTeamSurvivor(
                job = Citizen(),
                isDead = true,
                isFrogCursed = false
            )
        )
    }

    private data class Survivor(
        val job: org.beobma.mafia42discordproject.job.Job,
        val isFrogCursed: Boolean
    )
}
