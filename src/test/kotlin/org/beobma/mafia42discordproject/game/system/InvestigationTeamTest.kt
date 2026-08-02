package org.beobma.mafia42discordproject.game.system

import org.beobma.mafia42discordproject.job.definition.list.Citizen
import org.beobma.mafia42discordproject.job.definition.list.Frog
import org.beobma.mafia42discordproject.job.definition.list.Hacker
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import kotlin.test.Test
import kotlin.test.assertEquals

class InvestigationTeamTest {
    @Test
    fun `frog curse overrides a citizen job`() {
        assertEquals(InvestigationTeam.FROG, InvestigationTeam.of(Hacker(), isFrogCursed = true))
    }

    @Test
    fun `frog curse overrides a mafia job`() {
        assertEquals(InvestigationTeam.FROG, InvestigationTeam.of(Mafia(), isFrogCursed = true))
    }

    @Test
    fun `hallucinated cursed citizen is investigated as mafia`() {
        assertEquals(
            InvestigationTeam.MAFIA,
            InvestigationTeam.of(
                job = Hacker(),
                isFrogCursed = true,
                hallucinatedAsMafia = true
            )
        )
    }

    @Test
    fun `displayed frog belongs to frog investigation team`() {
        assertEquals(InvestigationTeam.FROG, InvestigationTeam.of(Frog()))
    }

    @Test
    fun `uncursed jobs retain their normal investigation teams`() {
        assertEquals(InvestigationTeam.CITIZEN, InvestigationTeam.of(Citizen()))
        assertEquals(InvestigationTeam.MAFIA, InvestigationTeam.of(Mafia()))
    }
}
