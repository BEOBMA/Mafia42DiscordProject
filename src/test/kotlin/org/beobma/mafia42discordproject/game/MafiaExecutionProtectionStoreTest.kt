package org.beobma.mafia42discordproject.game

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MafiaExecutionProtectionStoreTest {
    @Test
    fun recordsLoadsAndConsumesProtectedTargets() {
        val path = Files.createTempDirectory("mafia-execution-protection-test").resolve("targets.json")
        val store = MafiaExecutionProtectionStore(path)

        store.record(20u, 200u)
        store.record(10u, 100u)

        val raw = Files.readString(path)
        assertTrue(raw.indexOf("\"10\"") < raw.indexOf("\"20\""))

        val loaded = MafiaExecutionProtectionStore(path)
        loaded.load()
        assertEquals(100u, loaded.consume(10u))
        assertEquals(200u, loaded.consume(20u))
        assertNull(loaded.consume(10u))

        val afterConsumption = MafiaExecutionProtectionStore(path)
        afterConsumption.load()
        assertNull(afterConsumption.consume(10u))
        assertNull(afterConsumption.consume(20u))
    }

    @Test
    fun overwritesTargetWhenFirstNightSelectionChanges() {
        val path = Files.createTempDirectory("mafia-execution-protection-overwrite-test").resolve("targets.json")
        val store = MafiaExecutionProtectionStore(path)

        store.record(10u, 100u)
        store.record(10u, 101u)

        val loaded = MafiaExecutionProtectionStore(path)
        loaded.load()
        assertEquals(101u, loaded.consume(10u))
    }
}
