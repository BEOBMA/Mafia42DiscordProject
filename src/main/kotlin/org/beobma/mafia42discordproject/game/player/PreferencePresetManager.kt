package org.beobma.mafia42discordproject.game.player

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.JobManager
import org.beobma.mafia42discordproject.util.AtomicTextFileWriter
import java.nio.file.Files
import java.nio.file.Path

data class PreferencePreset(
    val name: String,
    val jobs: List<Job>,
    val bestJob: Job
)

enum class PresetSaveResult {
    CREATED,
    UPDATED,
    INVALID_NAME,
    PREFERENCES_NOT_SET,
    BEST_JOB_NOT_SET,
    BEST_JOB_INVALID,
    LIMIT_REACHED
}

enum class PresetLoadResult {
    LOADED,
    INVALID_NAME,
    NOT_FOUND,
    INVALID_PRESET
}

enum class PresetRenameResult {
    RENAMED,
    INVALID_CURRENT_NAME,
    INVALID_NEW_NAME,
    NOT_FOUND,
    NAME_ALREADY_EXISTS
}

enum class PresetDeleteResult {
    DELETED,
    INVALID_NAME,
    NOT_FOUND
}

object PreferencePresetManager {
    const val MAX_PRESETS = 7
    const val MAX_NAME_LENGTH = 32
    private const val REQUIRED_PREFERENCE_COUNT = 7

    private val store = PreferencePresetStore(
        storagePath = Path.of("data", "preference-presets.json"),
        resolveJob = JobManager::findByName,
        maxPresets = MAX_PRESETS,
        requiredJobCount = REQUIRED_PREFERENCE_COUNT
    )

    fun saveCurrent(userId: ULong, rawName: String?): PresetSaveResult {
        val name = normalizeName(rawName) ?: return PresetSaveResult.INVALID_NAME
        val jobs = JobPreferenceManager.get(userId)
            ?.takeIf { it.size == REQUIRED_PREFERENCE_COUNT }
            ?: return PresetSaveResult.PREFERENCES_NOT_SET
        val bestJob = BestJobPreferenceManager.get(userId)
            ?: return PresetSaveResult.BEST_JOB_NOT_SET

        if (!BestJobPreferenceManager.isAllowedJob(userId, bestJob.name)) {
            return PresetSaveResult.BEST_JOB_INVALID
        }

        return when (store.save(userId, PreferencePreset(name, jobs, bestJob))) {
            PreferencePresetStore.SaveResult.CREATED -> PresetSaveResult.CREATED
            PreferencePresetStore.SaveResult.UPDATED -> PresetSaveResult.UPDATED
            PreferencePresetStore.SaveResult.LIMIT_REACHED -> PresetSaveResult.LIMIT_REACHED
        }
    }

    fun loadPreset(userId: ULong, rawName: String?): PresetLoadResult {
        val name = normalizeName(rawName) ?: return PresetLoadResult.INVALID_NAME
        val preset = store.get(userId, name) ?: return PresetLoadResult.NOT_FOUND
        if (preset.jobs.size != REQUIRED_PREFERENCE_COUNT || !isBestJobValidForPreset(preset)) {
            return PresetLoadResult.INVALID_PRESET
        }

        JobPreferenceManager.save(userId, preset.jobs)
        BestJobPreferenceManager.save(userId, preset.bestJob)
        return PresetLoadResult.LOADED
    }

    fun rename(userId: ULong, rawCurrentName: String?, rawNewName: String?): PresetRenameResult {
        val currentName = normalizeName(rawCurrentName)
            ?: return PresetRenameResult.INVALID_CURRENT_NAME
        val newName = normalizeName(rawNewName)
            ?: return PresetRenameResult.INVALID_NEW_NAME

        return when (store.rename(userId, currentName, newName)) {
            PreferencePresetStore.RenameResult.RENAMED -> PresetRenameResult.RENAMED
            PreferencePresetStore.RenameResult.NOT_FOUND -> PresetRenameResult.NOT_FOUND
            PreferencePresetStore.RenameResult.NAME_ALREADY_EXISTS -> PresetRenameResult.NAME_ALREADY_EXISTS
        }
    }

    fun delete(userId: ULong, rawName: String?): PresetDeleteResult {
        val name = normalizeName(rawName) ?: return PresetDeleteResult.INVALID_NAME
        return if (store.delete(userId, name)) {
            PresetDeleteResult.DELETED
        } else {
            PresetDeleteResult.NOT_FOUND
        }
    }

    fun get(userId: ULong, rawName: String?): PreferencePreset? {
        val name = normalizeName(rawName) ?: return null
        return store.get(userId, name)
    }

    fun getAll(userId: ULong): List<PreferencePreset> = store.getAll(userId)

    fun load() = store.load()

    fun normalizeName(rawName: String?): String? {
        val name = rawName?.trim() ?: return null
        return name.takeIf {
            it.isNotEmpty() &&
                it.length <= MAX_NAME_LENGTH &&
                it.none(Char::isISOControl)
        }
    }

    private fun isBestJobValidForPreset(preset: PreferencePreset): Boolean {
        return BestJobPreferenceManager.isAllowedJob(preset.jobs, preset.bestJob.name)
    }
}

internal class PreferencePresetStore(
    private val storagePath: Path,
    private val resolveJob: (String) -> Job?,
    private val maxPresets: Int = PreferencePresetManager.MAX_PRESETS,
    private val requiredJobCount: Int = 7
) {
    enum class SaveResult {
        CREATED,
        UPDATED,
        LIMIT_REACHED
    }

    enum class RenameResult {
        RENAMED,
        NOT_FOUND,
        NAME_ALREADY_EXISTS
    }

    private val presetsByUserId: MutableMap<ULong, List<PreferencePreset>> = mutableMapOf()
    private val json = Json { prettyPrint = true }

    fun save(userId: ULong, preset: PreferencePreset): SaveResult {
        require(preset.jobs.size == requiredJobCount) {
            "A preference preset must contain exactly $requiredJobCount jobs."
        }
        require(preset.jobs.distinctBy(Job::name).size == requiredJobCount) {
            "A preference preset cannot contain duplicate jobs."
        }

        val current = presetsByUserId[userId].orEmpty()
        val existingIndex = current.indexOfName(preset.name)
        val result: SaveResult
        val updated = if (existingIndex >= 0) {
            result = SaveResult.UPDATED
            current.toMutableList().apply { this[existingIndex] = preset }.toList()
        } else {
            if (current.size >= maxPresets) return SaveResult.LIMIT_REACHED
            result = SaveResult.CREATED
            current + preset
        }

        presetsByUserId[userId] = updated
        persist()
        return result
    }

    fun get(userId: ULong, name: String): PreferencePreset? =
        presetsByUserId[userId].orEmpty().firstOrNull { it.name.equals(name, ignoreCase = true) }

    fun getAll(userId: ULong): List<PreferencePreset> = presetsByUserId[userId].orEmpty().toList()

    fun rename(userId: ULong, currentName: String, newName: String): RenameResult {
        val current = presetsByUserId[userId].orEmpty()
        val currentIndex = current.indexOfName(currentName)
        if (currentIndex < 0) return RenameResult.NOT_FOUND

        val duplicateIndex = current.indexOfName(newName)
        if (duplicateIndex >= 0 && duplicateIndex != currentIndex) {
            return RenameResult.NAME_ALREADY_EXISTS
        }

        val updated = current.toMutableList()
        updated[currentIndex] = updated[currentIndex].copy(name = newName)
        presetsByUserId[userId] = updated
        persist()
        return RenameResult.RENAMED
    }

    fun delete(userId: ULong, name: String): Boolean {
        val current = presetsByUserId[userId].orEmpty()
        val index = current.indexOfName(name)
        if (index < 0) return false

        val updated = current.toMutableList().apply { removeAt(index) }
        if (updated.isEmpty()) {
            presetsByUserId.remove(userId)
        } else {
            presetsByUserId[userId] = updated
        }
        persist()
        return true
    }

    fun load() {
        if (!Files.exists(storagePath)) return

        val raw = Files.readString(storagePath)
        val root = runCatching { json.parseToJsonElement(raw) }
            .getOrElse {
                println("[PreferencePresetManager] failed to parse preset file: ${it.message}")
                return
            } as? JsonObject ?: return

        val loaded = mutableMapOf<ULong, List<PreferencePreset>>()
        root.forEach { (userIdText, presetsElement) ->
            val userId = userIdText.toULongOrNull()
            if (userId == null) {
                println("[PreferencePresetManager] skipping invalid userId key: $userIdText")
                return@forEach
            }

            val presetsArray = presetsElement as? JsonArray ?: return@forEach
            val names = mutableSetOf<String>()
            val presets = presetsArray.mapNotNull { element ->
                parsePreset(element as? JsonObject ?: return@mapNotNull null)
            }.filter { preset ->
                val normalizedName = preset.name.lowercase()
                names.add(normalizedName)
            }.take(maxPresets)

            if (presets.isNotEmpty()) loaded[userId] = presets
        }

        presetsByUserId.clear()
        presetsByUserId.putAll(loaded)
        println("[PreferencePresetManager] loaded ${loaded.values.sumOf(List<PreferencePreset>::size)} presets for ${loaded.size} users.")
    }

    private fun parsePreset(jsonObject: JsonObject): PreferencePreset? {
        val name = (jsonObject["name"] as? JsonPrimitive)
            ?.contentOrNull
            ?.let(PreferencePresetManager::normalizeName)
            ?: return null
        val jobsArray = jsonObject["jobs"] as? JsonArray ?: return null
        if (jobsArray.size != requiredJobCount) return null

        val jobs = jobsArray.mapNotNull { element ->
            val jobName = (element as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
            resolveJob(jobName).also { job ->
                if (job == null) println("[PreferencePresetManager] skipping unknown job: $jobName")
            }
        }
        if (jobs.size != requiredJobCount || jobs.distinctBy(Job::name).size != requiredJobCount) return null

        val bestJobName = (jsonObject["bestJob"] as? JsonPrimitive)?.contentOrNull ?: return null
        val bestJob = resolveJob(bestJobName)
        if (bestJob == null) {
            println("[PreferencePresetManager] skipping unknown best job: $bestJobName")
            return null
        }
        return PreferencePreset(name, jobs, bestJob)
    }

    private fun persist() {
        val root = buildJsonObject {
            presetsByUserId.entries
                .sortedBy { it.key }
                .forEach { (userId, presets) ->
                    put(userId.toString(), buildJsonArray {
                        presets.forEach { preset ->
                            add(buildJsonObject {
                                put("name", JsonPrimitive(preset.name))
                                put("jobs", buildJsonArray {
                                    preset.jobs.forEach { job -> add(JsonPrimitive(job.name)) }
                                })
                                put("bestJob", JsonPrimitive(preset.bestJob.name))
                            })
                        }
                    })
                }
        }
        AtomicTextFileWriter.write(storagePath, json.encodeToString(JsonObject.serializer(), root))
    }

    private fun List<PreferencePreset>.indexOfName(name: String): Int =
        indexOfFirst { it.name.equals(name, ignoreCase = true) }
}
