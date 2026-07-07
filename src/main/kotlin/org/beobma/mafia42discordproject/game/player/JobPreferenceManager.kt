package org.beobma.mafia42discordproject.game.player

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.JobManager
import org.beobma.mafia42discordproject.job.definition.list.MentalPatient
import org.beobma.mafia42discordproject.util.AtomicTextFileWriter
import java.nio.file.Files
import java.nio.file.Path

object JobPreferenceManager {
    private val store = JobPreferenceStore(
        storagePath = Path.of("data", "job-preferences.json"),
        resolveJob = JobManager::findByName
    )

    fun save(userId: ULong, jobs: List<Job>) = store.save(userId, jobs)

    fun get(userId: ULong): List<Job>? = store.get(userId)

    fun load() = store.load()
}

internal class JobPreferenceStore(
    private val storagePath: Path,
    private val resolveJob: (String) -> Job?
) {
    private val preferencesByUserId: MutableMap<ULong, List<Job>> = mutableMapOf()
    private val json = Json { prettyPrint = true }

    fun save(userId: ULong, jobs: List<Job>) {
        preferencesByUserId[userId] = jobs.toList()
        persist()
    }

    fun get(userId: ULong): List<Job>? = preferencesByUserId[userId]

    fun load() {
        if (!Files.exists(storagePath)) {
            return
        }

        val raw = Files.readString(storagePath)
        val root = runCatching { json.parseToJsonElement(raw) }
            .getOrElse {
                println("[JobPreferenceManager] failed to parse preference file: ${it.message}")
                return
            }

        val loaded = mutableMapOf<ULong, List<Job>>()
        val jsonObject = root as? JsonObject ?: return

        jsonObject.forEach { (userIdText, jobsElement) ->
            val userId = userIdText.toULongOrNull()
            if (userId == null) {
                println("[JobPreferenceManager] skipping invalid userId key: $userIdText")
                return@forEach
            }

            val jobsArray = jobsElement as? JsonArray ?: return@forEach
            val jobs = jobsArray.mapNotNull { element ->
                val name = element.jsonPrimitive.contentOrNull ?: return@mapNotNull null
                resolveJob(name).also { job ->
                    if (job == null) {
                        println("[JobPreferenceManager] skipping unknown job: $name")
                    }
                }
            }.filter { job -> job.name != MentalPatient.JOB_NAME }

            if (jobs.isNotEmpty()) {
                loaded[userId] = jobs
            }
        }

        preferencesByUserId.clear()
        preferencesByUserId.putAll(loaded)
        println("[JobPreferenceManager] loaded ${loaded.size} preference entries.")
    }

    private fun persist() {
        val snapshot = preferencesByUserId.toMap()

        val root = buildJsonObject {
            snapshot.entries
                .sortedBy { it.key }
                .forEach { (userId, jobs) ->
                    put(userId.toString(), buildJsonArray {
                        jobs.forEach { job ->
                            add(JsonPrimitive(job.name))
                        }
                    })
                }
        }

        AtomicTextFileWriter.write(storagePath, json.encodeToString(JsonObject.serializer(), root))
    }
}
