package org.beobma.mafia42discordproject.game.communication

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals

class MegaphoneUseGateTest {
    @Test
    fun `only one simultaneous megaphone use can claim the night`() = runBlocking {
        val gate = MegaphoneUseGate()
        val firstEntered = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        var usedTonight = false

        suspend fun tryUse(waitBeforeClaim: Boolean): Boolean = gate.exclusive {
            if (usedTonight) return@exclusive false
            if (waitBeforeClaim) {
                firstEntered.complete(Unit)
                releaseFirst.await()
            }
            usedTonight = true
            true
        }

        val first = async { tryUse(waitBeforeClaim = true) }
        firstEntered.await()
        val second = async { tryUse(waitBeforeClaim = false) }
        yield()
        releaseFirst.complete(Unit)

        assertEquals(listOf(true, false), listOf(first.await(), second.await()))
    }
}
