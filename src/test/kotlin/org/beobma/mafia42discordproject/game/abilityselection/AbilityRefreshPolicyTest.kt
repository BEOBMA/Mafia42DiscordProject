package org.beobma.mafia42discordproject.game.abilityselection

import dev.kord.common.entity.Snowflake
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AbilityRefreshPolicyTest {
    private val firstMafiaId = Snowflake(1uL)
    private val secondMafiaId = Snowflake(2uL)
    private val publicTargetId = Snowflake(3uL)

    @Test
    fun ninePlayersGrantsOneRefreshToOneRandomMafia() {
        val grants = selectAbilityRefreshGrants(
            playerCount = 9,
            mafiaPlayerIds = listOf(firstMafiaId, secondMafiaId),
            previousPublicTargetId = null,
            random = Random(42)
        )

        assertEquals(1, grants.size)
        assertEquals(1, grants.values.single().refreshCount)
        assertTrue(grants.keys.single() in setOf(firstMafiaId, secondMafiaId))
    }

    @Test
    fun tenPlayersGrantsOneRefreshToBothMafias() {
        val grants = selectAbilityRefreshGrants(
            playerCount = 10,
            mafiaPlayerIds = listOf(firstMafiaId, secondMafiaId),
            previousPublicTargetId = null
        )

        assertEquals(1, grants.getValue(firstMafiaId).refreshCount)
        assertEquals(1, grants.getValue(secondMafiaId).refreshCount)
    }

    @Test
    fun soloMafiaReceivesTwoRefreshes() {
        val grants = selectAbilityRefreshGrants(
            playerCount = 8,
            mafiaPlayerIds = listOf(firstMafiaId),
            previousPublicTargetId = null
        )

        assertEquals(2, grants.getValue(firstMafiaId).refreshCount)
        assertEquals(
            setOf(AbilityRefreshGrantReason.SOLO_MAFIA),
            grants.getValue(firstMafiaId).reasons
        )
    }

    @Test
    fun previousPublicTargetReceivesOneRefreshRegardlessOfJob() {
        val grants = selectAbilityRefreshGrants(
            playerCount = 8,
            mafiaPlayerIds = listOf(firstMafiaId, secondMafiaId),
            previousPublicTargetId = publicTargetId
        )

        assertEquals(1, grants.getValue(publicTargetId).refreshCount)
        assertEquals(
            setOf(AbilityRefreshGrantReason.PREVIOUS_PUBLIC_TARGET),
            grants.getValue(publicTargetId).reasons
        )
    }

    @Test
    fun publicTargetAndSoloMafiaBonusesStack() {
        val grants = selectAbilityRefreshGrants(
            playerCount = 8,
            mafiaPlayerIds = listOf(firstMafiaId),
            previousPublicTargetId = firstMafiaId
        )

        val grant = grants.getValue(firstMafiaId)
        assertEquals(3, grant.refreshCount)
        assertEquals(
            setOf(
                AbilityRefreshGrantReason.PREVIOUS_PUBLIC_TARGET,
                AbilityRefreshGrantReason.SOLO_MAFIA
            ),
            grant.reasons
        )
    }

    @Test
    fun publicTargetBonusStacksWithExtendedGameBonus() {
        val grants = selectAbilityRefreshGrants(
            playerCount = 10,
            mafiaPlayerIds = listOf(firstMafiaId, secondMafiaId),
            previousPublicTargetId = firstMafiaId
        )

        assertEquals(2, grants.getValue(firstMafiaId).refreshCount)
        assertEquals(1, grants.getValue(secondMafiaId).refreshCount)
    }

    @Test
    fun otherPlayerCountsDoNotGrantRefreshesToMultipleMafias() {
        val grants = selectAbilityRefreshGrants(
            playerCount = 11,
            mafiaPlayerIds = listOf(firstMafiaId, secondMafiaId),
            previousPublicTargetId = null
        )

        assertTrue(grants.isEmpty())
    }
}
