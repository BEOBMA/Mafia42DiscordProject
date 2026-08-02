package org.beobma.mafia42discordproject.game.abilityselection

import dev.kord.common.entity.Snowflake
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AbilityRefreshPolicyTest {
    private val firstMafiaId = Snowflake(1uL)
    private val secondMafiaId = Snowflake(2uL)

    @Test
    fun ninePlayersGrantsOneRefreshToOneRandomMafia() {
        val limits = selectMafiaAbilityRefreshLimits(
            playerCount = 9,
            mafiaPlayerIds = listOf(firstMafiaId, secondMafiaId),
            random = Random(42)
        )

        assertEquals(1, limits.size)
        assertEquals(1, limits.values.single())
        assertTrue(limits.keys.single() in setOf(firstMafiaId, secondMafiaId))
    }

    @Test
    fun tenPlayersGrantsOneRefreshToBothMafias() {
        val limits = selectMafiaAbilityRefreshLimits(
            playerCount = 10,
            mafiaPlayerIds = listOf(firstMafiaId, secondMafiaId)
        )

        assertEquals(mapOf(firstMafiaId to 1, secondMafiaId to 1), limits)
    }

    @Test
    fun soloMafiaKeepsTwoRefreshes() {
        val limits = selectMafiaAbilityRefreshLimits(
            playerCount = 8,
            mafiaPlayerIds = listOf(firstMafiaId)
        )

        assertEquals(mapOf(firstMafiaId to 2), limits)
    }

    @Test
    fun otherPlayerCountsDoNotGrantRefreshesToMultipleMafias() {
        val limits = selectMafiaAbilityRefreshLimits(
            playerCount = 11,
            mafiaPlayerIds = listOf(firstMafiaId, secondMafiaId)
        )

        assertTrue(limits.isEmpty())
    }
}
