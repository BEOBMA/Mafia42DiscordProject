package org.beobma.mafia42discordproject.game.replay

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class ReplayRenderData(
    val schemaVersion: Int = GameReplayRenderDataStore.SCHEMA_VERSION,
    val generatedAtMillis: Long,
    val endReason: String,
    val winningTeamName: String?,
    val guildId: String?,
    val guildName: String?,
    val replayStartedAtMillis: Long,
    val dayCount: Int,
    val initialPlayerCount: Int,
    val players: List<ReplayRenderPlayer>,
    val logs: List<ReplayRenderLogEntry>
)

data class ReplayRenderPlayer(
    val id: String?,
    val name: String,
    val jobName: String?,
    val isDead: Boolean
)

data class ReplayRenderRecipient(
    val id: String?,
    val name: String,
    val scope: ReplayVisibility
)

data class ReplayRenderLogEntry(
    val sequence: Long,
    val timestampMillis: Long,
    val dayCount: Int,
    val phase: GamePhase,
    val type: ReplayLogType,
    val actorId: String?,
    val actorName: String?,
    val actorJobName: String?,
    val recipients: List<ReplayRenderRecipient>,
    val visibility: ReplayVisibility,
    val title: String,
    val body: String,
    val imageUrls: List<String> = emptyList(),
    val relatedEventId: String? = null
)

object GameReplayRenderDataStore {
    const val SCHEMA_VERSION = 1

    private val json = Json { prettyPrint = true }
    private val renderDataDir: Path = Path.of("data", "replay-render-data")
    private val fileNameFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
        .withZone(ZoneOffset.UTC)

    fun snapshot(game: Game, endReason: String, winningTeamName: String?): ReplayRenderData {
        val players = synchronized(game) {
            game.playerDatas.map { player ->
                val snapshotJob = game.probationOriginalJobsByPlayer[player.member.id] ?: player.job
                ReplayRenderPlayer(
                    id = player.member.id.value.toString(),
                    name = player.member.effectiveName,
                    jobName = snapshotJob?.name,
                    isDead = player.state.isDead
                )
            }
        }
        val logs = synchronized(game) {
            game.replayLogs
                .sortedBy { it.sequence }
                .map { it.toRenderLogEntry() }
        }

        return ReplayRenderData(
            generatedAtMillis = System.currentTimeMillis(),
            endReason = endReason,
            winningTeamName = winningTeamName,
            guildId = game.guild.id.value.toString(),
            guildName = game.guild.name,
            replayStartedAtMillis = game.replayStartedAtMillis,
            dayCount = game.dayCount,
            initialPlayerCount = game.initialPlayerCount,
            players = players,
            logs = logs
        )
    }

    fun save(data: ReplayRenderData): Path? {
        return runCatching {
            if (!Files.exists(renderDataDir)) {
                Files.createDirectories(renderDataDir)
            }

            val guildPart = data.guildId?.takeIf { it.isNotBlank() } ?: "unknown-guild"
            val fileName = "replay-${fileNameFormatter.format(Instant.ofEpochMilli(data.generatedAtMillis))}-$guildPart.json"
            val outputPath = renderDataDir.resolve(fileName)
            Files.writeString(outputPath, json.encodeToString(JsonObject.serializer(), toJson(data)))
            println("[GameReplayRenderDataStore] 리플레이 렌더 데이터 저장 완료: $outputPath")
            outputPath
        }.onFailure { error ->
            println("[GameReplayRenderDataStore] 리플레이 렌더 데이터 저장 실패: ${error.message}")
        }.getOrNull()
    }

    fun load(path: Path): ReplayRenderData {
        val root = json.parseToJsonElement(Files.readString(path)).jsonObject
        return fromJson(root)
    }

    private fun ReplayLogEntry.toRenderLogEntry(): ReplayRenderLogEntry {
        return ReplayRenderLogEntry(
            sequence = sequence,
            timestampMillis = timestampMillis,
            dayCount = dayCount,
            phase = phase,
            type = type,
            actorId = actorId?.value?.toString(),
            actorName = actorName,
            actorJobName = actorJobName,
            recipients = recipients.map { it.toRenderRecipient() },
            visibility = visibility,
            title = title,
            body = body,
            imageUrls = imageUrls,
            relatedEventId = relatedEventId
        )
    }

    private fun ReplayRecipient.toRenderRecipient(): ReplayRenderRecipient {
        return ReplayRenderRecipient(
            id = id?.value?.toString(),
            name = name,
            scope = scope
        )
    }

    private fun toJson(data: ReplayRenderData): JsonObject {
        return buildJsonObject {
            put("schemaVersion", data.schemaVersion)
            put("generatedAtMillis", data.generatedAtMillis)
            put("endReason", data.endReason)
            putNullable("winningTeamName", data.winningTeamName)
            putNullable("guildId", data.guildId)
            putNullable("guildName", data.guildName)
            put("replayStartedAtMillis", data.replayStartedAtMillis)
            put("dayCount", data.dayCount)
            put("initialPlayerCount", data.initialPlayerCount)
            putPlayers(data.players)
            putLogs("logs", data.logs)
        }
    }

    private fun fromJson(root: JsonObject): ReplayRenderData {
        val logs = parseLogs(root["logs"]?.jsonArray ?: root["replayLogs"]?.jsonArray)
        val archivedAtMillis = root.string("archivedAt")?.let { raw ->
            runCatching { Instant.parse(raw).toEpochMilli() }.getOrNull()
        }
        val generatedAtMillis = root.long("generatedAtMillis")
            ?: archivedAtMillis
            ?: System.currentTimeMillis()
        val replayStartedAtMillis = root.long("replayStartedAtMillis")
            ?: logs.firstOrNull()?.timestampMillis
            ?: generatedAtMillis

        return ReplayRenderData(
            schemaVersion = root.int("schemaVersion") ?: 0,
            generatedAtMillis = generatedAtMillis,
            endReason = root.string("endReason") ?: "UNKNOWN",
            winningTeamName = root.string("winningTeamName") ?: root.string("winningTeam"),
            guildId = root.string("guildId"),
            guildName = root.string("guildName"),
            replayStartedAtMillis = replayStartedAtMillis,
            dayCount = root.int("dayCount") ?: logs.maxOfOrNull { it.dayCount } ?: 0,
            initialPlayerCount = root.int("initialPlayerCount") ?: 0,
            players = parsePlayers(root["players"]?.jsonArray),
            logs = logs
        )
    }

    private fun JsonObjectBuilder.putPlayers(players: List<ReplayRenderPlayer>) {
        put("players", buildJsonArray {
            players.forEach { player ->
                add(buildJsonObject {
                    putNullable("id", player.id)
                    put("name", player.name)
                    putNullable("jobName", player.jobName)
                    put("isDead", player.isDead)
                })
            }
        })
    }

    private fun JsonObjectBuilder.putLogs(key: String, logs: List<ReplayRenderLogEntry>) {
        put(key, buildJsonArray {
            logs.sortedBy { it.sequence }.forEach { entry ->
                add(buildJsonObject {
                    put("sequence", entry.sequence)
                    put("timestampMillis", entry.timestampMillis)
                    put("dayCount", entry.dayCount)
                    put("phase", entry.phase.name)
                    put("type", entry.type.name)
                    putNullable("actorId", entry.actorId)
                    putNullable("actorName", entry.actorName)
                    putNullable("actorJobName", entry.actorJobName)
                    put("visibility", entry.visibility.name)
                    put("title", entry.title)
                    put("body", entry.body)
                    put("imageUrls", buildJsonArray {
                        entry.imageUrls.forEach { add(it) }
                    })
                    put("recipients", buildJsonArray {
                        entry.recipients.forEach { recipient ->
                            add(buildJsonObject {
                                putNullable("id", recipient.id)
                                put("name", recipient.name)
                                put("scope", recipient.scope.name)
                            })
                        }
                    })
                    putNullable("relatedEventId", entry.relatedEventId)
                })
            }
        })
    }

    private fun parsePlayers(playersJson: kotlinx.serialization.json.JsonArray?): List<ReplayRenderPlayer> {
        return playersJson?.mapNotNull { element ->
            val player = element.jsonObject
            ReplayRenderPlayer(
                id = player.string("id"),
                name = player.string("name") ?: return@mapNotNull null,
                jobName = player.string("jobName") ?: player.string("job"),
                isDead = player.boolean("isDead")
                    ?: player["state"]?.jsonObject?.boolean("isDead")
                    ?: false
            )
        } ?: emptyList()
    }

    private fun parseLogs(logsJson: kotlinx.serialization.json.JsonArray?): List<ReplayRenderLogEntry> {
        return logsJson?.mapNotNull { element ->
            val entry = element.jsonObject
            ReplayRenderLogEntry(
                sequence = entry.long("sequence") ?: return@mapNotNull null,
                timestampMillis = entry.long("timestampMillis") ?: 0L,
                dayCount = entry.int("dayCount") ?: 0,
                phase = entry.enumValue("phase", GamePhase.DAY),
                type = entry.enumValue("type", ReplayLogType.SYSTEM_RESULT),
                actorId = entry.string("actorId"),
                actorName = entry.string("actorName"),
                actorJobName = entry.string("actorJobName"),
                recipients = parseRecipients(entry["recipients"]?.jsonArray),
                visibility = entry.enumValue("visibility", ReplayVisibility.PUBLIC),
                title = entry.string("title").orEmpty(),
                body = entry.string("body").orEmpty(),
                imageUrls = entry["imageUrls"]?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                    ?: emptyList(),
                relatedEventId = entry.string("relatedEventId")
            )
        }?.sortedBy { it.sequence } ?: emptyList()
    }

    private fun parseRecipients(recipientsJson: kotlinx.serialization.json.JsonArray?): List<ReplayRenderRecipient> {
        return recipientsJson?.mapNotNull { element ->
            val recipient = element.jsonObject
            ReplayRenderRecipient(
                id = recipient.string("id"),
                name = recipient.string("name") ?: return@mapNotNull null,
                scope = recipient.enumValue("scope", ReplayVisibility.PUBLIC)
            )
        } ?: emptyList()
    }

    private fun JsonObjectBuilder.putNullable(key: String, value: String?) {
        if (value == null) {
            put(key, JsonNull)
            return
        }
        put(key, value)
    }

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.long(key: String): Long? = this[key]?.jsonPrimitive?.longOrNull

    private fun JsonObject.int(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull

    private fun JsonObject.boolean(key: String): Boolean? = this[key]?.jsonPrimitive?.booleanOrNull

    private inline fun <reified T : Enum<T>> JsonObject.enumValue(key: String, default: T): T {
        return string(key)?.let { raw ->
            runCatching { enumValueOf<T>(raw) }.getOrNull()
        } ?: default
    }
}
