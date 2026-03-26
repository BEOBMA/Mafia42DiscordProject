package org.beobma.mafia42discordproject.game.player

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.JobManager
import java.nio.file.Files
import java.nio.file.Path

object BestJobPreferenceManager {
    private val bestJobByUserId: MutableMap<ULong, Job> = mutableMapOf()
    private val storagePath: Path = Path.of("data", "best-job-preferences.json")
    private val json = Json { prettyPrint = true }

    private val fixedCandidateJobNames = setOf("의사", "마피아", "경찰", "요원", "자경단원")

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
                println("[BestJobPreferenceManager] 저장 파일 파싱 실패: ${it.message}")
                return
            }

        val loaded = mutableMapOf<ULong, Job>()
        val jsonObject = root as? JsonObject ?: return

        jsonObject.forEach { (userIdText, jobElement) ->
            val userId = userIdText.toULongOrNull()
            if (userId == null) {
                println("[BestJobPreferenceManager] 잘못된 userId 키를 건너뜁니다: $userIdText")
                return@forEach
            }

            val jobName = jobElement.jsonPrimitive.contentOrNull ?: return@forEach
            val matchedJob = JobManager.findByName(jobName)
            if (matchedJob == null) {
                println("[BestJobPreferenceManager] 알 수 없는 직업을 건너뜁니다: $jobName")
                return@forEach
            }

            loaded[userId] = matchedJob
        }

        bestJobByUserId.clear()
        bestJobByUserId.putAll(loaded)
        println("[BestJobPreferenceManager] 최선호 직업 ${loaded.size}건을 불러왔습니다.")
    }

    fun buildAllowedJobNames(userId: ULong): Set<String> {
        val preferredJobNames = JobPreferenceManager.get(userId)
            .orEmpty()
            .map(Job::name)
            .toSet()
        return preferredJobNames + fixedCandidateJobNames
    }

    fun isAllowedJob(userId: ULong, jobName: String): Boolean {
        return jobName in buildAllowedJobNames(userId)
    }

    private fun persist() {
        val parent = storagePath.parent
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent)
        }

        val root = buildJsonObject {
            bestJobByUserId.entries
                .sortedBy { it.key }
                .forEach { (userId, job) ->
                    put(userId.toString(), JsonPrimitive(job.name))
                }
        }

        Files.writeString(storagePath, json.encodeToString(JsonObject.serializer(), root))
    }
}
