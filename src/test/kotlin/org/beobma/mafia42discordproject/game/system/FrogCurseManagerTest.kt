package org.beobma.mafia42discordproject.game.system

import org.beobma.mafia42discordproject.job.definition.list.Judge
import org.beobma.mafia42discordproject.job.definition.list.Politician
import org.beobma.mafia42discordproject.job.definition.list.Reporter
import org.beobma.mafia42discordproject.job.evil.list.Witch
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FrogCurseManagerTest {
    @Test
    fun `cursed mafia keeps abilities`() {
        assertFalse(FrogCurseManager.shouldSuppressAbilities(Mafia(), isFrogCursed = true))
    }

    @Test
    fun `cursed politician and judge lose abilities`() {
        assertTrue(FrogCurseManager.shouldSuppressAbilities(Politician(), isFrogCursed = true))
        assertTrue(FrogCurseManager.shouldSuppressAbilities(Judge(), isFrogCursed = true))
    }

    @Test
    fun `cursed citizen and mafia support jobs lose abilities`() {
        assertTrue(FrogCurseManager.shouldSuppressAbilities(Reporter(), isFrogCursed = true))
        assertTrue(FrogCurseManager.shouldSuppressAbilities(Witch(), isFrogCursed = true))
    }

    @Test
    fun `grave robber that acquired mafia still loses abilities while cursed`() {
        assertTrue(
            FrogCurseManager.shouldSuppressAbilities(
                Mafia(),
                isFrogCursed = true,
                hasCompletedGraveRobbing = true
            )
        )
    }

    @Test
    fun `uncursed jobs keep abilities`() {
        assertFalse(FrogCurseManager.shouldSuppressAbilities(Politician(), isFrogCursed = false))
        assertFalse(FrogCurseManager.shouldSuppressAbilities(Judge(), isFrogCursed = false))
    }
}
