package org.beobma.mafia42discordproject.game.player

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.beobma.mafia42discordproject.job.Job
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PreferencePresetStoreTest {
    private val alpha = TestJob("alpha")
    private val beta = TestJob("beta")
    private val gamma = TestJob("gamma")
    private val jobsByName = listOf(alpha, beta, gamma).associateBy(Job::name)

    @Test
    fun savesUpToLimitAndOverwritesNamesCaseInsensitively() {
        val path = Files.createTempDirectory("preference-presets-limit-test").resolve("presets.json")
        val store = store(path, maxPresets = 2)

        assertEquals(
            PreferencePresetStore.SaveResult.CREATED,
            store.save(1u, PreferencePreset("첫 설정", listOf(alpha, beta), alpha))
        )
        assertEquals(
            PreferencePresetStore.SaveResult.UPDATED,
            store.save(1u, PreferencePreset("첫 설정", listOf(beta, gamma), beta))
        )
        assertEquals(
            PreferencePresetStore.SaveResult.CREATED,
            store.save(1u, PreferencePreset("Second", listOf(alpha, gamma), gamma))
        )
        assertEquals(
            PreferencePresetStore.SaveResult.LIMIT_REACHED,
            store.save(1u, PreferencePreset("third", listOf(alpha, beta), alpha))
        )

        assertEquals(listOf(beta, gamma), store.get(1u, "첫 설정")?.jobs)
        assertEquals("Second", store.get(1u, "second")?.name)
        assertEquals(2, store.getAll(1u).size)
    }

    @Test
    fun renamesAndDeletesPresetsWithoutChangingTheirSettings() {
        val path = Files.createTempDirectory("preference-presets-rename-test").resolve("presets.json")
        val store = store(path)
        store.save(1u, PreferencePreset("one", listOf(alpha, beta), alpha))
        store.save(1u, PreferencePreset("two", listOf(beta, gamma), beta))

        assertEquals(
            PreferencePresetStore.RenameResult.NAME_ALREADY_EXISTS,
            store.rename(1u, "one", "TWO")
        )
        assertEquals(
            PreferencePresetStore.RenameResult.RENAMED,
            store.rename(1u, "ONE", "renamed")
        )
        assertEquals(listOf(alpha, beta), store.get(1u, "renamed")?.jobs)
        assertNull(store.get(1u, "one"))

        assertTrue(store.delete(1u, "RENAMED"))
        assertFalse(store.delete(1u, "missing"))
        assertEquals(listOf("two"), store.getAll(1u).map(PreferencePreset::name))
    }

    @Test
    fun persistsAndLoadsCompleteKnownPresets() {
        val path = Files.createTempDirectory("preference-presets-load-test").resolve("presets.json")
        val store = store(path)
        store.save(20u, PreferencePreset("later", listOf(beta, gamma), gamma))
        store.save(10u, PreferencePreset("main", listOf(alpha, beta), alpha))

        val raw = Files.readString(path)
        assertTrue(raw.indexOf("\"10\"") < raw.indexOf("\"20\""))
        val firstPreset = Json.parseToJsonElement(raw).jsonObject["10"]!!.jsonArray.first().jsonObject
        assertEquals("main", firstPreset["name"]!!.jsonPrimitive.content)
        assertEquals(listOf("alpha", "beta"), firstPreset["jobs"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertEquals("alpha", firstPreset["bestJob"]!!.jsonPrimitive.content)

        val loaded = store(path)
        loaded.load()
        assertEquals("main", loaded.get(10u, "MAIN")?.name)
        assertEquals(gamma, loaded.get(20u, "later")?.bestJob)
    }

    @Test
    fun skipsMalformedUnknownAndDuplicatePresetsWhenLoading() {
        val path = Files.createTempDirectory("preference-presets-invalid-test").resolve("presets.json")
        Files.writeString(
            path,
            """
            {
              "1": [
                {"name":"valid","jobs":["alpha","beta"],"bestJob":"alpha"},
                {"name":"unknown","jobs":["alpha","missing"],"bestJob":"alpha"},
                {"name":"duplicate jobs","jobs":["alpha","alpha"],"bestJob":"alpha"},
                {"name":"VALID","jobs":["beta","gamma"],"bestJob":"beta"}
              ]
            }
            """.trimIndent()
        )

        val store = store(path)
        store.load()

        assertEquals(listOf("valid"), store.getAll(1u).map(PreferencePreset::name))
    }

    @Test
    fun validatesPresetNameLengthAndControlCharacters() {
        assertEquals("내 프리셋", PreferencePresetManager.normalizeName("  내 프리셋  "))
        assertNull(PreferencePresetManager.normalizeName(""))
        assertNull(PreferencePresetManager.normalizeName("a".repeat(PreferencePresetManager.MAX_NAME_LENGTH + 1)))
        assertNull(PreferencePresetManager.normalizeName("bad\nname"))
    }

    private fun store(path: java.nio.file.Path, maxPresets: Int = 7) = PreferencePresetStore(
        storagePath = path,
        resolveJob = jobsByName::get,
        maxPresets = maxPresets,
        requiredJobCount = 2
    )

    private data class TestJob(
        override val name: String,
        override val description: String = "test job"
    ) : Job()
}
