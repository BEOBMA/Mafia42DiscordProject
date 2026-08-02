package org.beobma.mafia42discordproject.job.evil.list

import dev.kord.common.entity.Snowflake
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.MafiaAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.thief.ThiefAbility
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ThiefTest {
    @Test
    fun `theft is an independently selectable vote phase ability`() {
        val ability = ThiefAbility()

        assertIs<ActiveAbility>(ability)
        assertTrue(ability.usablePhase == GamePhase.VOTE)
    }

    @Test
    fun `borrowed job abilities are attached and removed together`() {
        val thief = Thief()
        val mafia = Mafia()

        thief.setStolenJob(mafia, Snowflake(42u), mafia.abilities)

        assertIs<Mafia>(thief.stolenJob)
        assertTrue(thief.abilities.any { it is MafiaAbility })

        thief.clearStolenAbility()

        assertFalse(thief.abilities.any { it is MafiaAbility })
        assertTrue(thief.stolenJob == null)
    }

    @Test
    fun `game long stolen ability flags survive a borrowed job change`() {
        val thief = Thief().apply {
            hasUsedStolenHackerAbility = true
            hasStolenPoliticianAbility = true
            hasActivatedSuccessorMafia = true
            hasUsedStolenMagicianTrick = true
            hasUsedTheftThisVote = true
        }

        thief.setStolenJob(Mafia(), Snowflake(7u), emptyList())
        thief.clearStolenAbility()

        assertTrue(thief.hasUsedStolenHackerAbility)
        assertTrue(thief.hasStolenPoliticianAbility)
        assertTrue(thief.hasActivatedSuccessorMafia)
        assertTrue(thief.hasUsedStolenMagicianTrick)
        assertTrue(thief.hasUsedTheftThisVote)
    }
}
