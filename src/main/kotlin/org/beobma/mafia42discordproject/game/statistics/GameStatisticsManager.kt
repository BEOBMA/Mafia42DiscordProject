package org.beobma.mafia42discordproject.game.statistics

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import kotlin.math.round

object GameStatisticsManager {
    private const val schemaVersion = 1
    private val json = Json { prettyPrint = true }
    private val archiveDir: Path = Path.of("data", "game-archives")
    private val outputDir: Path = Path.of("data", "statistics")
    private val outputPath: Path = outputDir.resolve("game-statistics.json")

    fun generate(): GenerationResult {
        val previouslyProcessedArchiveIds = loadProcessedArchiveIds()
        val archiveFiles = archiveFiles()
        val state = StatisticsState()
        var unreadableArchiveCount = 0
        var duplicateArchiveFileCount = 0

        archiveFiles.forEach { archivePath ->
            val archive = runCatching { readArchive(archivePath) }.getOrNull()
            if (archive == null) {
                unreadableArchiveCount++
                return@forEach
            }

            if (state.processedArchives.containsKey(archive.archiveId)) {
                duplicateArchiveFileCount++
                return@forEach
            }

            state.record(archive)
        }

        val newArchiveCount = state.processedArchives.keys.count { it !in previouslyProcessedArchiveIds }
        val output = toJson(
            state = state,
            sourceFileCount = archiveFiles.size,
            newArchiveCount = newArchiveCount,
            duplicateArchiveFileCount = duplicateArchiveFileCount,
            unreadableArchiveCount = unreadableArchiveCount,
        )

        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir)
        }
        Files.writeString(outputPath, json.encodeToString(JsonObject.serializer(), output))

        return GenerationResult(
            outputPath = outputPath,
            totalArchiveFileCount = archiveFiles.size,
            processedArchiveCount = state.processedArchives.size,
            newArchiveCount = newArchiveCount,
            duplicateArchiveFileCount = duplicateArchiveFileCount,
            unreadableArchiveCount = unreadableArchiveCount,
        )
    }

    private fun archiveFiles(): List<Path> {
        if (!Files.exists(archiveDir)) {
            return emptyList()
        }

        Files.list(archiveDir).use { stream ->
            return stream
                .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".json", ignoreCase = true) }
                .sorted()
                .toList()
        }
    }

    private fun loadProcessedArchiveIds(): Set<String> {
        if (!Files.exists(outputPath)) {
            return emptySet()
        }

        return runCatching {
            val root = json.parseToJsonElement(Files.readString(outputPath)) as? JsonObject
                ?: return@runCatching emptySet()
            val processedArchives = root.obj("processedArchives") ?: return@runCatching emptySet()
            processedArchives.keys
        }.getOrDefault(emptySet())
    }

    private fun readArchive(path: Path): ArchiveGame? {
        val root = json.parseToJsonElement(Files.readString(path)) as? JsonObject ?: return null
        val players = root.array("players")
            .orEmpty()
            .mapNotNull { parsePlayer(it) }
        val replayLogs = root.array("replayLogs").orEmpty()
        val abilityUses = replayLogs.mapNotNull { parseAbilityUse(it) }
        val replayEventTypes = replayLogs
            .mapNotNull { (it as? JsonObject)?.string("type") }
            .groupingBy { it.ifBlank { unknownValue } }
            .eachCount()

        val archivedAt = root.string("archivedAt")
        val guildId = root.string("guildId")
        val endReason = root.string("endReason") ?: unknownValue
        val winningTeam = root.string("winningTeam")
        val dayCount = root.int("dayCount") ?: 0
        val initialPlayerCount = root.int("initialPlayerCount") ?: players.size

        val replaySignature = replayLogs
            .mapNotNull { it as? JsonObject }
            .joinToString(";") { log ->
                listOf(
                    log.string("sequence").orEmpty(),
                    log.string("timestampMillis").orEmpty(),
                    log.string("type").orEmpty(),
                    log.string("actorId").orEmpty(),
                    log.string("title").orEmpty(),
                ).joinToString(":")
            }
        val archiveFingerprint = listOf(
            guildId.orEmpty(),
            endReason,
            winningTeam.orEmpty(),
            initialPlayerCount.toString(),
            dayCount.toString(),
            players.joinToString(",") { "${it.id}:${it.job}:${it.team}" },
            replaySignature,
        ).joinToString("|")
        val archiveId = sha256(archiveFingerprint)

        return ArchiveGame(
            archiveId = archiveId,
            fileName = path.fileName.toString(),
            archivedAt = archivedAt,
            endReason = endReason,
            winningTeam = winningTeam,
            guildId = guildId,
            guildName = root.string("guildName"),
            dayCount = dayCount,
            initialPlayerCount = initialPlayerCount,
            isCrazyMode = root.boolean("isCrazyMode") ?: false,
            players = players,
            replayLogCount = replayLogs.size,
            replayEventTypes = replayEventTypes,
            abilityUses = abilityUses,
        )
    }

    private fun parsePlayer(element: JsonElement): ArchivePlayer? {
        val obj = element as? JsonObject ?: return null
        val id = obj.string("id") ?: return null
        val name = obj.string("name") ?: id
        val state = obj.obj("state")
        return ArchivePlayer(
            id = id,
            name = name,
            job = obj.string("job") ?: unknownValue,
            displayedJob = obj.string("displayedJob"),
            team = obj.string("team") ?: unknownValue,
            abilities = obj.array("abilities")
                .orEmpty()
                .mapNotNull { it.jsonPrimitive.contentOrNull }
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct(),
            isDead = state?.boolean("isDead") ?: false,
        )
    }

    private fun parseAbilityUse(element: JsonElement): ArchiveAbilityUse? {
        val obj = element as? JsonObject ?: return null
        if (obj.string("type") != "ABILITY_USED") {
            return null
        }

        val body = obj.string("body").orEmpty()
        val abilityName = extractBodyValue(body, "능력") ?: extractBodyValue(body, "Ability") ?: return null
        val result = extractBodyValue(body, "결과") ?: extractBodyValue(body, "Result")

        return ArchiveAbilityUse(
            actorId = obj.string("actorId"),
            actorName = obj.string("actorName") ?: unknownValue,
            actorJobName = obj.string("actorJobName") ?: unknownValue,
            abilityName = abilityName,
            result = result,
        )
    }

    private fun extractBodyValue(body: String, key: String): String? {
        return body
            .lineSequence()
            .firstOrNull { line -> line.trimStart().startsWith("$key:") }
            ?.substringAfter(':')
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    private fun StatisticsState.record(archive: ArchiveGame) {
        processedArchives[archive.archiveId] = ArchiveRecord(
            fileName = archive.fileName,
            archivedAt = archive.archivedAt,
            guildId = archive.guildId,
            guildName = archive.guildName,
            endReason = archive.endReason,
            winningTeam = archive.winningTeam,
            playerCount = archive.players.size,
            initialPlayerCount = archive.initialPlayerCount,
            dayCount = archive.dayCount,
            isCrazyMode = archive.isCrazyMode,
            replayLogCount = archive.replayLogCount,
        )

        overview.record(archive)
        archive.replayEventTypes.forEach { (type, count) -> overview.replayEventTypes.increment(type, count) }

        archive.players.forEach { player ->
            val outcome = player.outcome(archive.winningTeam)
            val survived = !player.isDead
            val job = player.job.ifBlank { unknownValue }
            val team = player.team.ifBlank { unknownValue }

            byTeam.bucket(team).record(outcome, survived, archive)
            byJob.bucket(job).record(outcome, survived, archive)
            player.displayedJob?.takeIf { it.isNotBlank() }?.let { displayedJob ->
                byDisplayedJob.bucket(displayedJob).record(outcome, survived, archive)
            }

            val playerAggregate = byPlayer.getOrPut(player.id) { PlayerAggregate(player.id, player.name) }
            playerAggregate.name = player.name
            playerAggregate.bucket.record(outcome, survived, archive)
            playerAggregate.jobs.bucket(job).record(outcome, survived, archive)
            playerAggregate.teams.bucket(team).record(outcome, survived, archive)

            byPlayerJob.combo(player.id, player.name, job).bucket.record(outcome, survived, archive)

            player.abilities.forEach { ability ->
                byAbility.bucket(ability).record(outcome, survived, archive)
                playerAggregate.abilities.bucket(ability).record(outcome, survived, archive)
                byJobAbility.combo(job, ability).bucket.record(outcome, survived, archive)
                byPlayerAbility.combo(player.id, player.name, ability).bucket.record(outcome, survived, archive)
            }
        }

        archive.abilityUses.forEach { usage ->
            byUsedAbility.usage(usage.abilityName).record(usage.result)
            byUsedAbilityByJob.usage(usage.abilityName, usage.actorJobName).bucket.record(usage.result)
            usage.actorId?.let { actorId ->
                byUsedAbilityByPlayer.usage(usage.abilityName, actorId, usage.actorName).bucket.record(usage.result)
            }
        }
    }

    private fun ArchivePlayer.outcome(winningTeam: String?): Outcome {
        if (winningTeam.isNullOrBlank()) {
            return Outcome.NO_CONTEST
        }
        return if (team == winningTeam) Outcome.WIN else Outcome.LOSS
    }

    private fun toJson(
        state: StatisticsState,
        sourceFileCount: Int,
        newArchiveCount: Int,
        duplicateArchiveFileCount: Int,
        unreadableArchiveCount: Int,
    ): JsonObject {
        return buildJsonObject {
            put("schemaVersion", schemaVersion)
            put("generatedAt", Instant.now().toString())
            put("sourceArchiveDirectory", archiveDir.toString())
            put("outputFile", outputPath.toString())
            put("sourceArchiveFileCount", sourceFileCount)
            put("processedArchiveCount", state.processedArchives.size)
            put("newArchiveCount", newArchiveCount)
            put("duplicateArchiveFileCount", duplicateArchiveFileCount)
            put("unreadableArchiveCount", unreadableArchiveCount)
            put("overview", overviewJson(state.overview))
            put("processedArchives", processedArchivesJson(state.processedArchives))
            put("byTeam", bucketMapJson(state.byTeam))
            put("byJob", bucketMapJson(state.byJob))
            put("byDisplayedJob", bucketMapJson(state.byDisplayedJob))
            put("byAbility", bucketMapJson(state.byAbility))
            put("byPlayer", playerMapJson(state.byPlayer))
            put("byJobAbility", jobAbilityJson(state.byJobAbility))
            put("byPlayerJob", playerJobJson(state.byPlayerJob))
            put("byPlayerAbility", playerAbilityJson(state.byPlayerAbility))
            put("abilityUsage", buildJsonObject {
                put("byAbility", usageMapJson(state.byUsedAbility))
                put("byAbilityAndJob", usageByJobJson(state.byUsedAbilityByJob))
                put("byAbilityAndPlayer", usageByPlayerJson(state.byUsedAbilityByPlayer))
            })
        }
    }

    private fun overviewJson(overview: OverviewStats): JsonObject {
        return buildJsonObject {
            put("totalGames", overview.totalGames)
            put("totalPlayerEntries", overview.totalPlayerEntries)
            put("averageDayCount", rate(overview.totalDayCount, overview.totalGames))
            put("averageInitialPlayerCount", rate(overview.totalInitialPlayerCount, overview.totalGames))
            put("winningTeams", counterJson(overview.winningTeams))
            put("endReasons", counterJson(overview.endReasons))
            put("modes", counterJson(overview.modes))
            put("initialPlayerCounts", counterJson(overview.initialPlayerCounts))
            put("dayCounts", counterJson(overview.dayCounts))
            put("replayEventTypes", counterJson(overview.replayEventTypes))
        }
    }

    private fun processedArchivesJson(records: Map<String, ArchiveRecord>): JsonObject {
        return buildJsonObject {
            records.entries
                .sortedBy { it.value.archivedAt ?: it.value.fileName }
                .forEach { (archiveId, record) ->
                    put(archiveId, buildJsonObject {
                        put("fileName", record.fileName)
                        putNullable("archivedAt", record.archivedAt)
                        putNullable("guildId", record.guildId)
                        putNullable("guildName", record.guildName)
                        put("endReason", record.endReason)
                        putNullable("winningTeam", record.winningTeam)
                        put("playerCount", record.playerCount)
                        put("initialPlayerCount", record.initialPlayerCount)
                        put("dayCount", record.dayCount)
                        put("isCrazyMode", record.isCrazyMode)
                        put("replayLogCount", record.replayLogCount)
                    })
                }
        }
    }

    private fun playerMapJson(players: Map<String, PlayerAggregate>): JsonObject {
        return buildJsonObject {
            players.values
                .sortedWith(compareByDescending<PlayerAggregate> { it.bucket.appearances }.thenBy { it.name })
                .forEach { player ->
                    put(player.id, buildJsonObject {
                        put("name", player.name)
                        putBucket(player.bucket)
                        put("jobs", bucketMapJson(player.jobs))
                        put("abilities", bucketMapJson(player.abilities))
                        put("teams", bucketMapJson(player.teams))
                    })
                }
        }
    }

    private fun jobAbilityJson(combos: Map<String, JobAbilityAggregate>): JsonObject {
        return buildJsonObject {
            combos.values
                .sortedWith(compareByDescending<JobAbilityAggregate> { it.bucket.appearances }.thenBy { it.job }.thenBy { it.ability })
                .forEach { combo ->
                    put("${combo.job} + ${combo.ability}", buildJsonObject {
                        put("job", combo.job)
                        put("ability", combo.ability)
                        putBucket(combo.bucket)
                    })
                }
        }
    }

    private fun playerJobJson(combos: Map<String, PlayerJobAggregate>): JsonObject {
        return buildJsonObject {
            combos.values
                .sortedWith(compareByDescending<PlayerJobAggregate> { it.bucket.appearances }.thenBy { it.playerName }.thenBy { it.job })
                .forEach { combo ->
                    put("${combo.playerName} (${combo.playerId}) + ${combo.job}", buildJsonObject {
                        put("playerId", combo.playerId)
                        put("playerName", combo.playerName)
                        put("job", combo.job)
                        putBucket(combo.bucket)
                    })
                }
        }
    }

    private fun playerAbilityJson(combos: Map<String, PlayerAbilityAggregate>): JsonObject {
        return buildJsonObject {
            combos.values
                .sortedWith(compareByDescending<PlayerAbilityAggregate> { it.bucket.appearances }.thenBy { it.playerName }.thenBy { it.ability })
                .forEach { combo ->
                    put("${combo.playerName} (${combo.playerId}) + ${combo.ability}", buildJsonObject {
                        put("playerId", combo.playerId)
                        put("playerName", combo.playerName)
                        put("ability", combo.ability)
                        putBucket(combo.bucket)
                    })
                }
        }
    }

    private fun bucketMapJson(map: Map<String, StatBucket>): JsonObject {
        return buildJsonObject {
            map.entries
                .sortedWith(compareByDescending<Map.Entry<String, StatBucket>> { it.value.appearances }.thenBy { it.key })
                .forEach { (key, bucket) ->
                    put(key, bucketJson(bucket))
                }
        }
    }

    private fun bucketJson(bucket: StatBucket): JsonObject {
        return buildJsonObject {
            putBucket(bucket)
        }
    }

    private fun JsonObjectBuilder.putBucket(bucket: StatBucket) {
        put("games", bucket.appearances)
        put("wins", bucket.wins)
        put("losses", bucket.losses)
        put("noContest", bucket.noContest)
        put("winRate", percent(bucket.wins, bucket.wins + bucket.losses))
        put("survivals", bucket.survivals)
        put("deaths", bucket.deaths)
        put("survivalRate", percent(bucket.survivals, bucket.appearances))
        put("averageDayCount", rate(bucket.totalDayCount, bucket.appearances))
        put("averageInitialPlayerCount", rate(bucket.totalInitialPlayerCount, bucket.appearances))
    }

    private fun usageMapJson(map: Map<String, UsageBucket>): JsonObject {
        return buildJsonObject {
            map.entries
                .sortedWith(compareByDescending<Map.Entry<String, UsageBucket>> { it.value.uses }.thenBy { it.key })
                .forEach { (ability, bucket) ->
                    put(ability, usageJson(bucket))
                }
        }
    }

    private fun usageByJobJson(map: Map<String, AbilityJobUsageAggregate>): JsonObject {
        return buildJsonObject {
            map.values
                .sortedWith(compareByDescending<AbilityJobUsageAggregate> { it.bucket.uses }.thenBy { it.ability }.thenBy { it.job })
                .forEach { aggregate ->
                    put("${aggregate.ability} + ${aggregate.job}", buildJsonObject {
                        put("ability", aggregate.ability)
                        put("job", aggregate.job)
                        putUsage(aggregate.bucket)
                    })
                }
        }
    }

    private fun usageByPlayerJson(map: Map<String, AbilityPlayerUsageAggregate>): JsonObject {
        return buildJsonObject {
            map.values
                .sortedWith(compareByDescending<AbilityPlayerUsageAggregate> { it.bucket.uses }.thenBy { it.playerName }.thenBy { it.ability })
                .forEach { aggregate ->
                    put("${aggregate.playerName} (${aggregate.playerId}) + ${aggregate.ability}", buildJsonObject {
                        put("ability", aggregate.ability)
                        put("playerId", aggregate.playerId)
                        put("playerName", aggregate.playerName)
                        putUsage(aggregate.bucket)
                    })
                }
        }
    }

    private fun usageJson(bucket: UsageBucket): JsonObject {
        return buildJsonObject {
            putUsage(bucket)
        }
    }

    private fun JsonObjectBuilder.putUsage(bucket: UsageBucket) {
        put("uses", bucket.uses)
        put("successes", bucket.successes)
        put("failures", bucket.failures)
        put("unknownResults", bucket.unknownResults)
        put("successRate", percent(bucket.successes, bucket.successes + bucket.failures))
        put("results", counterJson(bucket.results))
    }

    private fun counterJson(counter: Map<String, Int>): JsonObject {
        return buildJsonObject {
            counter.entries
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .forEach { (key, value) ->
                    put(key, value)
                }
        }
    }

    private fun JsonObjectBuilder.putNullable(key: String, value: String?) {
        if (value == null) {
            put(key, JsonNull)
        } else {
            put(key, value)
        }
    }

    private fun MutableMap<String, StatBucket>.bucket(key: String): StatBucket {
        return getOrPut(key.ifBlank { unknownValue }) { StatBucket() }
    }

    private fun MutableMap<String, Int>.increment(key: String, amount: Int = 1) {
        val normalized = key.ifBlank { unknownValue }
        this[normalized] = (this[normalized] ?: 0) + amount
    }

    private fun MutableMap<String, JobAbilityAggregate>.combo(job: String, ability: String): JobAbilityAggregate {
        val key = "$job\u0000$ability"
        return getOrPut(key) { JobAbilityAggregate(job, ability) }
    }

    private fun MutableMap<String, PlayerJobAggregate>.combo(
        playerId: String,
        playerName: String,
        job: String,
    ): PlayerJobAggregate {
        val key = "$playerId\u0000$job"
        val aggregate = getOrPut(key) { PlayerJobAggregate(playerId, playerName, job) }
        aggregate.playerName = playerName
        return aggregate
    }

    private fun MutableMap<String, PlayerAbilityAggregate>.combo(
        playerId: String,
        playerName: String,
        ability: String,
    ): PlayerAbilityAggregate {
        val key = "$playerId\u0000$ability"
        val aggregate = getOrPut(key) { PlayerAbilityAggregate(playerId, playerName, ability) }
        aggregate.playerName = playerName
        return aggregate
    }

    private fun MutableMap<String, UsageBucket>.usage(ability: String): UsageBucket {
        return getOrPut(ability.ifBlank { unknownValue }) { UsageBucket() }
    }

    private fun MutableMap<String, AbilityJobUsageAggregate>.usage(
        ability: String,
        job: String,
    ): AbilityJobUsageAggregate {
        val key = "$ability\u0000$job"
        return getOrPut(key) { AbilityJobUsageAggregate(ability, job) }
    }

    private fun MutableMap<String, AbilityPlayerUsageAggregate>.usage(
        ability: String,
        playerId: String,
        playerName: String,
    ): AbilityPlayerUsageAggregate {
        val key = "$ability\u0000$playerId"
        val aggregate = getOrPut(key) { AbilityPlayerUsageAggregate(ability, playerId, playerName) }
        aggregate.playerName = playerName
        return aggregate
    }

    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

    private fun JsonObject.array(key: String): JsonArray? = this[key] as? JsonArray

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.int(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull

    private fun JsonObject.boolean(key: String): Boolean? = this[key]?.jsonPrimitive?.booleanOrNull

    private fun percent(numerator: Int, denominator: Int): Double {
        if (denominator <= 0) return 0.0
        return round(numerator * 10000.0 / denominator) / 100.0
    }

    private fun rate(total: Int, count: Int): Double {
        if (count <= 0) return 0.0
        return round(total * 100.0 / count) / 100.0
    }

    data class GenerationResult(
        val outputPath: Path,
        val totalArchiveFileCount: Int,
        val processedArchiveCount: Int,
        val newArchiveCount: Int,
        val duplicateArchiveFileCount: Int,
        val unreadableArchiveCount: Int,
    )

    private data class StatisticsState(
        val processedArchives: LinkedHashMap<String, ArchiveRecord> = linkedMapOf(),
        val overview: OverviewStats = OverviewStats(),
        val byTeam: MutableMap<String, StatBucket> = linkedMapOf(),
        val byJob: MutableMap<String, StatBucket> = linkedMapOf(),
        val byDisplayedJob: MutableMap<String, StatBucket> = linkedMapOf(),
        val byAbility: MutableMap<String, StatBucket> = linkedMapOf(),
        val byPlayer: MutableMap<String, PlayerAggregate> = linkedMapOf(),
        val byJobAbility: MutableMap<String, JobAbilityAggregate> = linkedMapOf(),
        val byPlayerJob: MutableMap<String, PlayerJobAggregate> = linkedMapOf(),
        val byPlayerAbility: MutableMap<String, PlayerAbilityAggregate> = linkedMapOf(),
        val byUsedAbility: MutableMap<String, UsageBucket> = linkedMapOf(),
        val byUsedAbilityByJob: MutableMap<String, AbilityJobUsageAggregate> = linkedMapOf(),
        val byUsedAbilityByPlayer: MutableMap<String, AbilityPlayerUsageAggregate> = linkedMapOf(),
    )

    private data class ArchiveGame(
        val archiveId: String,
        val fileName: String,
        val archivedAt: String?,
        val endReason: String,
        val winningTeam: String?,
        val guildId: String?,
        val guildName: String?,
        val dayCount: Int,
        val initialPlayerCount: Int,
        val isCrazyMode: Boolean,
        val players: List<ArchivePlayer>,
        val replayLogCount: Int,
        val replayEventTypes: Map<String, Int>,
        val abilityUses: List<ArchiveAbilityUse>,
    )

    private data class ArchivePlayer(
        val id: String,
        val name: String,
        val job: String,
        val displayedJob: String?,
        val team: String,
        val abilities: List<String>,
        val isDead: Boolean,
    )

    private data class ArchiveAbilityUse(
        val actorId: String?,
        val actorName: String,
        val actorJobName: String,
        val abilityName: String,
        val result: String?,
    )

    private data class ArchiveRecord(
        val fileName: String,
        val archivedAt: String?,
        val guildId: String?,
        val guildName: String?,
        val endReason: String,
        val winningTeam: String?,
        val playerCount: Int,
        val initialPlayerCount: Int,
        val dayCount: Int,
        val isCrazyMode: Boolean,
        val replayLogCount: Int,
    )

    private data class OverviewStats(
        var totalGames: Int = 0,
        var totalPlayerEntries: Int = 0,
        var totalDayCount: Int = 0,
        var totalInitialPlayerCount: Int = 0,
        val winningTeams: MutableMap<String, Int> = linkedMapOf(),
        val endReasons: MutableMap<String, Int> = linkedMapOf(),
        val modes: MutableMap<String, Int> = linkedMapOf(),
        val initialPlayerCounts: MutableMap<String, Int> = linkedMapOf(),
        val dayCounts: MutableMap<String, Int> = linkedMapOf(),
        val replayEventTypes: MutableMap<String, Int> = linkedMapOf(),
    ) {
        fun record(archive: ArchiveGame) {
            totalGames++
            totalPlayerEntries += archive.players.size
            totalDayCount += archive.dayCount
            totalInitialPlayerCount += archive.initialPlayerCount
            winningTeams.increment(archive.winningTeam ?: "NO_WINNER")
            endReasons.increment(archive.endReason)
            modes.increment(if (archive.isCrazyMode) "CRAZY" else "NORMAL")
            initialPlayerCounts.increment(archive.initialPlayerCount.toString())
            dayCounts.increment(archive.dayCount.toString())
        }
    }

    private data class StatBucket(
        var appearances: Int = 0,
        var wins: Int = 0,
        var losses: Int = 0,
        var noContest: Int = 0,
        var survivals: Int = 0,
        var deaths: Int = 0,
        var totalDayCount: Int = 0,
        var totalInitialPlayerCount: Int = 0,
    ) {
        fun record(outcome: Outcome, survived: Boolean, archive: ArchiveGame) {
            appearances++
            when (outcome) {
                Outcome.WIN -> wins++
                Outcome.LOSS -> losses++
                Outcome.NO_CONTEST -> noContest++
            }
            if (survived) {
                survivals++
            } else {
                deaths++
            }
            totalDayCount += archive.dayCount
            totalInitialPlayerCount += archive.initialPlayerCount
        }
    }

    private data class PlayerAggregate(
        val id: String,
        var name: String,
        val bucket: StatBucket = StatBucket(),
        val jobs: MutableMap<String, StatBucket> = linkedMapOf(),
        val abilities: MutableMap<String, StatBucket> = linkedMapOf(),
        val teams: MutableMap<String, StatBucket> = linkedMapOf(),
    )

    private data class JobAbilityAggregate(
        val job: String,
        val ability: String,
        val bucket: StatBucket = StatBucket(),
    )

    private data class PlayerJobAggregate(
        val playerId: String,
        var playerName: String,
        val job: String,
        val bucket: StatBucket = StatBucket(),
    )

    private data class PlayerAbilityAggregate(
        val playerId: String,
        var playerName: String,
        val ability: String,
        val bucket: StatBucket = StatBucket(),
    )

    private data class UsageBucket(
        var uses: Int = 0,
        var successes: Int = 0,
        var failures: Int = 0,
        var unknownResults: Int = 0,
        val results: MutableMap<String, Int> = linkedMapOf(),
    ) {
        fun record(result: String?) {
            uses++
            val normalizedResult = result?.takeIf { it.isNotBlank() } ?: unknownValue
            results.increment(normalizedResult)
            when {
                normalizedResult.contains("성공", ignoreCase = true) -> successes++
                normalizedResult.contains("실패", ignoreCase = true) -> failures++
                else -> unknownResults++
            }
        }
    }

    private data class AbilityJobUsageAggregate(
        val ability: String,
        val job: String,
        val bucket: UsageBucket = UsageBucket(),
    )

    private data class AbilityPlayerUsageAggregate(
        val ability: String,
        val playerId: String,
        var playerName: String,
        val bucket: UsageBucket = UsageBucket(),
    )

    private enum class Outcome {
        WIN,
        LOSS,
        NO_CONTEST,
    }

    private const val unknownValue = "UNKNOWN"
}
