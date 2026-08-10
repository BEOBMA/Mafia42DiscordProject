package org.beobma.mafia42discordproject.game.communication

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class MegaphoneUseGate {
    private val mutex = Mutex()

    suspend fun <Result> exclusive(action: suspend () -> Result): Result {
        return mutex.withLock { action() }
    }
}
