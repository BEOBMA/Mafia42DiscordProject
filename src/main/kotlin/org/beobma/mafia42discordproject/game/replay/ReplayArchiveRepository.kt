package org.beobma.mafia42discordproject.game.replay

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

object ReplayArchiveRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val defaultReplayDirectory: Path = Path.of("data", "replay-render-data")
    private val uuidPattern = Regex("^[0-9a-f]{32}$")

    fun list(directory: Path = defaultReplayDirectory): JsonArray = buildJsonArray {
        load(directory)
            .sortedByDescending { it.generatedAtMillis }
            .distinctBy(ReplayRecord::uuid)
            .forEach { add(it.summary) }
    }

    fun find(uuid: String, directory: Path = defaultReplayDirectory): JsonObject? {
        val normalized = uuid.lowercase()
        if (!uuidPattern.matches(normalized)) return null
        return load(directory)
            .filter { it.uuid == normalized }
            .maxByOrNull { it.generatedAtMillis }
            ?.document
    }

    fun count(directory: Path = defaultReplayDirectory): Int = load(directory).distinctBy(ReplayRecord::uuid).size

    private fun load(directory: Path): List<ReplayRecord> {
        if (!Files.isDirectory(directory)) return emptyList()

        return Files.list(directory).use { paths ->
            paths.filter { it.isRegularFile() && it.extension.equals("json", ignoreCase = true) }
                .map { path -> parse(path) }
                .filter { it != null }
                .map { it!! }
                .toList()
        }
    }

    private fun parse(path: Path): ReplayRecord? = runCatching {
        val source = json.parseToJsonElement(path.readText()).jsonObject
        val guildId = source.string("guildId")
        val startedAt = source.long("replayStartedAtMillis")
        val generatedAt = source.long("generatedAtMillis").takeIf { it > 0 } ?: startedAt
        val storedUuid = source.string("replayUuid")?.lowercase()
        val uuid = storedUuid?.takeIf(uuidPattern::matches)
            ?: GameReplayRenderDataStore.replayUuid(guildId, startedAt)
        val document = JsonObject(source + ("replayUuid" to JsonPrimitive(uuid)))
        val logs = source["logs"]?.jsonArray ?: JsonArray(emptyList())
        val players = source["players"]?.jsonArray ?: JsonArray(emptyList())
        val lastLogAt = logs.maxOfOrNull { it.jsonObject.long("timestampMillis") } ?: generatedAt
        val durationMillis = (lastLogAt - startedAt).coerceAtLeast(0)

        val summary = buildJsonObject {
            put("replayUuid", uuid)
            put("generatedAtMillis", generatedAt)
            put("replayStartedAtMillis", startedAt)
            put("durationMillis", durationMillis)
            putNullable("guildId", guildId)
            putNullable("guildName", source.string("guildName"))
            putNullable("winningTeamName", source.string("winningTeamName"))
            put("endReason", source.string("endReason").orEmpty())
            put("dayCount", source.int("dayCount"))
            put("initialPlayerCount", source.int("initialPlayerCount").takeIf { it > 0 } ?: players.size)
            put("logCount", logs.size)
            put("players", buildJsonArray {
                players.forEach { playerElement ->
                    val player = playerElement.jsonObject
                    add(buildJsonObject {
                        put("name", player.string("name").orEmpty())
                        putNullable("jobName", player.string("jobName"))
                        put("isDead", player["isDead"]?.jsonPrimitive?.contentOrNull == "true")
                    })
                }
            })
        }
        ReplayRecord(uuid, generatedAt, document, summary)
    }.onFailure { error ->
        println("[ReplayArchiveRepository] 리플레이 파일 읽기 실패 ($path): ${error.message}")
    }.getOrNull()

    private fun JsonObject.string(key: String): String? {
        val value = this[key] ?: return null
        if (value is JsonNull) return null
        return value.jsonPrimitive.contentOrNull
    }

    private fun JsonObject.long(key: String): Long = this[key]?.jsonPrimitive?.longOrNull ?: 0L

    private fun JsonObject.int(key: String): Int = this[key]?.jsonPrimitive?.intOrNull ?: 0

    private fun kotlinx.serialization.json.JsonObjectBuilder.putNullable(key: String, value: String?) {
        put(key, value?.let(::JsonPrimitive) ?: JsonNull)
    }

    private data class ReplayRecord(
        val uuid: String,
        val generatedAtMillis: Long,
        val document: JsonObject,
        val summary: JsonObject
    )
}
