package org.beobma.mafia42discordproject.game.player

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.JobManager
import org.beobma.mafia42discordproject.job.definition.list.MentalPatient
import org.beobma.mafia42discordproject.util.AtomicTextFileWriter
import java.nio.file.Files
import java.nio.file.Path

object BestJobPreferenceManager {
    private val store = BestJobPreferenceStore(
        storagePath = Path.of("data", "best-job-preferences.json"),
        resolveJob = JobManager::findByName
    )

    private val fixedCandidateJobNames = setOf("의사")
    private val excludedCandidateJobNames = setOf(MentalPatient.JOB_NAME, "마피아")

    fun save(userId: ULong, job: Job) = store.save(userId, job)

    fun get(userId: ULong): Job? = store.get(userId)

    fun clear(userId: ULong) = store.clear(userId)

    fun load() = store.load()

    fun buildAllowedJobNames(userId: ULong): Set<String> {
        val preferredJobNames = JobPreferenceManager.get(userId)
            .orEmpty()
            .map(Job::name)
            .filter { it !in excludedCandidateJobNames }
            .toSet()
        return preferredJobNames + fixedCandidateJobNames
    }

    fun isAllowedJob(userId: ULong, jobName: String): Boolean {
        if (jobName in excludedCandidateJobNames) return false
        return jobName in buildAllowedJobNames(userId)
    }
}

internal class BestJobPreferenceStore(
    private val storagePath: Path,
    private val resolveJob: (String) -> Job?
) {
    private val bestJobByUserId: MutableMap<ULong, Job> = mutableMapOf()
    private val json = Json { prettyPrint = true }

    fun save(userId: ULong, job: Job) {
        bestJobByUserId[userId] = job
        persist()
    }

    fun get(userId: ULong): Job? = bestJobByUserId[userId]

    fun clear(userId: ULong) {
        if (bestJobByUserId.remove(userId) != null) {
            persist()
        }
    }

    fun load() {
        if (!Files.exists(storagePath)) {
            return
        }

        val raw = Files.readString(storagePath)
        val root = runCatching { json.parseToJsonElement(raw) }
            .getOrElse {
                println("[BestJobPreferenceManager] failed to parse preference file: ${it.message}")
                return
            }

        val loaded = mutableMapOf<ULong, Job>()
        val jsonObject = root as? JsonObject ?: return

        jsonObject.forEach { (userIdText, jobElement) ->
            val userId = userIdText.toULongOrNull()
            if (userId == null) {
                println("[BestJobPreferenceManager] skipping invalid userId key: $userIdText")
                return@forEach
            }

            val jobName = jobElement.jsonPrimitive.contentOrNull ?: return@forEach
            val matchedJob = resolveJob(jobName)
            if (matchedJob == null) {
                println("[BestJobPreferenceManager] skipping unknown job: $jobName")
                return@forEach
            }

            loaded[userId] = matchedJob
        }

        bestJobByUserId.clear()
        bestJobByUserId.putAll(loaded)
        println("[BestJobPreferenceManager] loaded ${loaded.size} best-job preference entries.")
    }

    private fun persist() {
        val snapshot = bestJobByUserId.toMap()

        val root = buildJsonObject {
            snapshot.entries
                .sortedBy { it.key }
                .forEach { (userId, job) ->
                    put(userId.toString(), JsonPrimitive(job.name))
                }
        }

        AtomicTextFileWriter.write(storagePath, json.encodeToString(JsonObject.serializer(), root))
    }
}
