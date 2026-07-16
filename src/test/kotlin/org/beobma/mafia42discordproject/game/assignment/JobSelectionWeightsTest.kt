package org.beobma.mafia42discordproject.game.assignment

import org.beobma.mafia42discordproject.job.Job
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class JobSelectionWeightsTest {
    @Test
    fun bestJobMultipliesEligiblePreferenceWeight() {
        val alpha = TestJob("alpha")
        val beta = TestJob("beta")
        val players = listOf(
            AssignmentPlayer(name = "player1", preferences = listOf(alpha, beta), bestJob = alpha),
            AssignmentPlayer(name = "player2", preferences = listOf(beta))
        )

        val weights = buildJobSelectionWeightByName(players) { job ->
            job.name in setOf(alpha.name, beta.name)
        }

        assertEquals(3, weights[alpha.name])
        assertEquals(2, weights[beta.name])
    }

    @Test
    fun bestJobOutsideEligiblePoolDoesNotAffectWeights() {
        val alpha = TestJob("alpha")
        val beta = TestJob("beta")
        val outside = TestJob("outside")
        val players = listOf(
            AssignmentPlayer(name = "player1", preferences = listOf(alpha), bestJob = outside),
            AssignmentPlayer(name = "player2", preferences = listOf(beta))
        )

        val weights = buildJobSelectionWeightByName(players) { job ->
            job.name != outside.name
        }

        assertEquals(1, weights[alpha.name])
        assertEquals(1, weights[beta.name])
        assertFalse(outside.name in weights)
    }

    private data class TestJob(
        override val name: String,
        override val description: String = "test job"
    ) : Job()
}
