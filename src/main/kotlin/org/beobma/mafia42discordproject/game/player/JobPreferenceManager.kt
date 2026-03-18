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
import java.nio.file.Files
import java.nio.file.Path

object JobPreferenceManager {
    private val preferencesByUserId: MutableMap<ULong, List<Job>> = mutableMapOf()
    private val storagePath: Path = Path.of("data", "job-preferences.json")
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
                println("[JobPreferenceManager] 저장 파일 파싱 실패: ${it.message}")
                return
            }

        val loaded = mutableMapOf<ULong, List<Job>>()
        val jsonObject = root as? JsonObject ?: return

        jsonObject.forEach { (userIdText, jobsElement) ->
            val userId = userIdText.toULongOrNull()
            if (userId == null) {
                println("[JobPreferenceManager] 잘못된 userId 키를 건너뜁니다: $userIdText")
                return@forEach
            }

            val jobsArray = jobsElement as? JsonArray ?: return@forEach
            val jobs = jobsArray.mapNotNull { element ->
                val name = element.jsonPrimitive.contentOrNull ?: return@mapNotNull null
                JobManager.findByName(name).also {
                    if (it == null) {
                        println("[JobPreferenceManager] 알 수 없는 직업을 건너뜁니다: $name")
                    }
                }
            }

            if (jobs.isNotEmpty()) {
                loaded[userId] = jobs
            }
        }

        preferencesByUserId.clear()
        preferencesByUserId.putAll(loaded)
        println("[JobPreferenceManager] 선호 직업 ${loaded.size}건을 불러왔습니다.")
    }

    private fun persist() {
        val parent = storagePath.parent
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent)
        }

        val root = buildJsonObject {
            preferencesByUserId.entries
                .sortedBy { it.key }
                .forEach { (userId, jobs) ->
                    put(userId.toString(), buildJsonArray {
                        jobs.forEach { job ->
                            add(JsonPrimitive(job.name))
                        }
                    })
                }
        }

        Files.writeString(storagePath, json.encodeToString(JsonObject.serializer(), root))
    }
}
