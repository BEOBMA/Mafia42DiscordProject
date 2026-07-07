package org.beobma.mafia42discordproject.game.player

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.beobma.mafia42discordproject.job.Job
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PreferenceStoreTest {
    @Test
    fun jobPreferenceStoreWritesSortedUserIdsAndJobArrays() {
        val path = Files.createTempDirectory("job-preferences-test").resolve("preferences.json")
        val alpha = TestJob("alpha")
        val beta = TestJob("beta")
        val store = JobPreferenceStore(path) { name -> mapOf(alpha.name to alpha, beta.name to beta)[name] }

        store.save(20u, listOf(beta))
        store.save(10u, listOf(alpha, beta))

        val raw = Files.readString(path)
        assertTrue(raw.indexOf("\"10\"") < raw.indexOf("\"20\""))

        val root = Json.parseToJsonElement(raw).jsonObject
        assertEquals(listOf("alpha", "beta"), root["10"]!!.jsonArray.map { it.jsonPrimitive.contentOrNull })
        assertEquals(listOf("beta"), root["20"]!!.jsonArray.map { it.jsonPrimitive.contentOrNull })
    }

    @Test
    fun jobPreferenceStoreLoadsKnownJobsAndSkipsUnknownOrEmptyEntries() {
        val path = Files.createTempDirectory("job-preferences-load-test").resolve("preferences.json")
        val alpha = TestJob("alpha")
        val beta = TestJob("beta")
        Files.writeString(
            path,
            """
            {
                "1": ["alpha", "missing", "beta"],
                "2": ["missing"],
                "bad": ["alpha"]
            }
            """.trimIndent()
        )

        val store = JobPreferenceStore(path) { name -> mapOf(alpha.name to alpha, beta.name to beta)[name] }
        store.load()

        assertEquals(listOf(alpha, beta), store.get(1u))
        assertNull(store.get(2u))
    }

    @Test
    fun bestJobPreferenceStoreWritesLoadsAndClearsValues() {
        val path = Files.createTempDirectory("best-job-preferences-test").resolve("best.json")
        val alpha = TestJob("alpha")
        val beta = TestJob("beta")
        val jobsByName = mapOf(alpha.name to alpha, beta.name to beta)

        val store = BestJobPreferenceStore(path) { name -> jobsByName[name] }
        store.save(20u, beta)
        store.save(10u, alpha)

        val raw = Files.readString(path)
        assertTrue(raw.indexOf("\"10\"") < raw.indexOf("\"20\""))

        val loaded = BestJobPreferenceStore(path) { name -> jobsByName[name] }
        loaded.load()
        assertEquals(alpha, loaded.get(10u))
        assertEquals(beta, loaded.get(20u))

        loaded.clear(10u)
        val clearedRaw = Files.readString(path)
        assertTrue("\"10\"" !in clearedRaw)
        assertEquals(beta, loaded.get(20u))
    }

    private data class TestJob(
        override val name: String,
        override val description: String = "test job"
    ) : Job()
}
