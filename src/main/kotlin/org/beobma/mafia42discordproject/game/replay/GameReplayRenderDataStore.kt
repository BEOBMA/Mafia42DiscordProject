package org.beobma.mafia42discordproject.game.replay

import kotlinx.serialization.json.*
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.util.AtomicTextFileWriter
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
            AtomicTextFileWriter.write(outputPath, json.encodeToString(JsonObject.serializer(), toJson(data)))
            println("[GameReplayRenderDataStore] 리플레이 렌더 데이터 저장 완료: $outputPath")
            outputPath
        }.onFailure { error ->
            println("[GameReplayRenderDataStore] 리플레이 렌더 데이터 저장 실패: ${error.message}")
        }.getOrNull()
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

    private fun JsonObjectBuilder.putNullable(key: String, value: String?) {
        if (value == null) {
            put(key, JsonNull)
            return
        }
        put(key, value)
    }
}
