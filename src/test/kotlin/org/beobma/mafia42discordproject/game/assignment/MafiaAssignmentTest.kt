package org.beobma.mafia42discordproject.game.assignment

import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MafiaAssignmentTest {
    @Test
    fun selectsRequestedNumberOfDistinctPlayers() {
        val selected = selectUniformMafiaPlayerIndices(
            playerCount = 8,
            mafiaCount = 2,
            random = Random(42)
        )

        assertEquals(2, selected.size)
        assertTrue(selected.all { it in 0 until 8 })
    }

    @Test
    fun givesEveryPlayerTheSameSelectionChance() {
        val selectionCounts = IntArray(8)
        val random = Random(42)
        val repeatCount = 20_000

        repeat(repeatCount) {
            selectUniformMafiaPlayerIndices(8, 2, random).forEach { index ->
                selectionCounts[index]++
            }
        }

        val expectedCount = repeatCount * 2 / 8
        selectionCounts.forEach { actualCount ->
            assertTrue(abs(actualCount - expectedCount) < 300)
        }
    }

    @Test
    fun rejectsMafiaCountLargerThanPlayerCount() {
        assertFailsWith<IllegalArgumentException> {
            selectUniformMafiaPlayerIndices(4, 5, Random(42))
        }
    }
}
