package org.beobma.mafia42discordproject.game.statistics

import kotlinx.serialization.json.*
import org.beobma.mafia42discordproject.game.mode.GameStartMode
import org.beobma.mafia42discordproject.util.AtomicTextFileWriter
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import kotlin.math.round

object GameStatisticsManager {
    private const val SCHEMA_VERSION = 1
    private val json = Json { prettyPrint = true }
    private val archiveDir: Path = Path.of("data", "game-archives")
    private val outputDir: Path = Path.of("data", "statistics")
    private val outputPath: Path = outputDir.resolve("game-statistics.json")

    private object Key {
        const val SCHEMA_VERSION = "스키마 버전"
        const val GENERATED_AT = "생성 시각"
        const val SOURCE_ARCHIVE_DIRECTORY = "원본 아카이브 폴더"
        const val OUTPUT_FILE = "출력 파일"
        const val SOURCE_ARCHIVE_FILE_COUNT = "원본 아카이브 파일 수"
        const val PROCESSED_ARCHIVE_COUNT = "반영된 아카이브 수"
        const val NEW_ARCHIVE_COUNT = "새로 반영된 아카이브 수"
        const val DUPLICATE_ARCHIVE_FILE_COUNT = "중복 제외 파일 수"
        const val UNREADABLE_ARCHIVE_COUNT = "읽기 실패 파일 수"

        const val OVERVIEW = "개요"
        const val PROCESSED_ARCHIVES = "반영된 아카이브"
        const val BY_TEAM = "팀별 통계"
        const val BY_JOB = "직업별 통계"
        const val BY_DISPLAYED_JOB = "표시 직업별 통계"
        const val BY_ABILITY = "능력별 통계"
        const val BY_PLAYER = "참가자별 통계"
        const val BY_JOB_ABILITY = "직업 능력 조합"
        const val BY_PLAYER_JOB = "참가자 직업 통계"
        const val BY_PLAYER_ABILITY = "참가자 능력 통계"
        const val ABILITY_USAGE = "능력 사용 통계"
        const val BY_ABILITY_USAGE = "능력별"
        const val BY_ABILITY_AND_JOB_USAGE = "능력·직업별"
        const val BY_ABILITY_AND_PLAYER_USAGE = "능력·참가자별"

        const val TOTAL_GAMES = "전체 게임 수"
        const val TOTAL_PLAYER_ENTRIES = "전체 참가 기록 수"
        const val AVERAGE_DAY_COUNT = "평균 진행 일차"
        const val AVERAGE_INITIAL_PLAYER_COUNT = "평균 시작 인원"
        const val WINNING_TEAMS = "승리 팀 분포"
        const val END_REASONS = "종료 사유 분포"
        const val MODES = "모드 분포"
        const val INITIAL_PLAYER_COUNTS = "시작 인원 분포"
        const val DAY_COUNTS = "일차 분포"
        const val REPLAY_EVENT_TYPES = "리플레이 이벤트 분포"

        const val FILE_NAME = "파일명"
        const val ARCHIVED_AT = "보관 시각"
        const val GUILD_ID = "길드 ID"
        const val GUILD_NAME = "길드명"
        const val END_REASON = "종료 사유"
        const val WINNING_TEAM = "승리 팀"
        const val PLAYER_COUNT = "플레이어 수"
        const val INITIAL_PLAYER_COUNT = "시작 인원"
        const val DAY_COUNT = "진행 일차"
        const val MODE_TYPE = "모드 타입"
        const val REPLAY_LOG_COUNT = "리플레이 로그 수"

        const val NAME = "이름"
        const val JOB = "직업"
        const val ABILITY = "능력"
        const val JOBS = "직업 목록"
        const val ABILITIES = "능력 목록"
        const val TEAMS = "팀 목록"
        const val PLAYER_ID = "참가자 ID"
        const val PLAYER_NAME = "참가자명"

        const val GAMES = "게임 수"
        const val WINS = "승리 수"
        const val LOSSES = "패배 수"
        const val NO_CONTEST = "무효 수"
        const val WIN_RATE = "승률"
        const val SURVIVALS = "생존 수"
        const val DEATHS = "사망 수"
        const val SURVIVAL_RATE = "생존율"

        const val USES = "사용 수"
        const val SUCCESSES = "성공 수"
        const val FAILURES = "실패 수"
        const val UNKNOWN_RESULTS = "결과 미확인 수"
        const val SUCCESS_RATE = "성공률"
        const val RESULTS = "결과 분포"
    }

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
        AtomicTextFileWriter.write(outputPath, json.encodeToString(JsonObject.serializer(), output))

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
            val processedArchives = root.obj(Key.PROCESSED_ARCHIVES)
                ?: root.obj("processedArchives")
                ?: return@runCatching emptySet()
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
            .groupingBy { replayLogTypeLabel(it.ifBlank { UNKNOWN_VALUE }) }
            .eachCount()

        val archivedAt = root.string("archivedAt")
        val guildId = root.string("guildId")
        val endReason = root.string("endReason") ?: UNKNOWN_VALUE
        val winningTeam = root.string("winningTeam")
        val dayCount = root.int("dayCount") ?: 0
        val initialPlayerCount = root.int("initialPlayerCount") ?: players.size
        val modeType = readModeType(root)
        val modeDisplayName = readModeDisplayName(root, modeType)

        val replaySignature = replayLogs
            .filterIsInstance<JsonObject>()
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
            modeType,
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
            modeType = modeType,
            modeDisplayName = modeDisplayName,
            players = players,
            replayLogCount = replayLogs.size,
            replayEventTypes = replayEventTypes,
            abilityUses = abilityUses,
        )
    }

    private fun readModeType(root: JsonObject): String {
        val archivedModeType = root.string("modeType")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        if (archivedModeType != null) {
            return normalizeModeType(archivedModeType)
        }

        val legacyIsCrazyMode = root.boolean("isCrazyMode") ?: false
        return if (legacyIsCrazyMode) {
            GameStartMode.MADNESS.typeName
        } else {
            GameStartMode.NORMAL.typeName
        }
    }

    private fun readModeDisplayName(root: JsonObject, modeType: String): String {
        return root.string("modeDisplayName")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: GameStartMode.displayNameForType(modeType)
    }

    private fun normalizeModeType(raw: String): String {
        return GameStartMode.fromType(raw)?.typeName ?: raw
    }

    private fun parsePlayer(element: JsonElement): ArchivePlayer? {
        val obj = element as? JsonObject ?: return null
        val id = obj.string("id") ?: return null
        val name = obj.string("name") ?: id
        val state = obj.obj("state")
        return ArchivePlayer(
            id = id,
            name = name,
            job = obj.string("job") ?: UNKNOWN_VALUE,
            displayedJob = obj.string("displayedJob"),
            team = obj.string("team") ?: UNKNOWN_VALUE,
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
            actorName = obj.string("actorName") ?: UNKNOWN_VALUE,
            actorJobName = obj.string("actorJobName") ?: UNKNOWN_VALUE,
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
            endReason = endReasonLabel(archive.endReason),
            winningTeam = archive.winningTeam,
            playerCount = archive.players.size,
            initialPlayerCount = archive.initialPlayerCount,
            dayCount = archive.dayCount,
            modeType = archive.modeType,
            replayLogCount = archive.replayLogCount,
        )

        overview.record(archive)
        archive.replayEventTypes.forEach { (type, count) -> overview.replayEventTypes.increment(type, count) }

        archive.players.forEach { player ->
            val outcome = player.outcome(archive.winningTeam)
            val survived = !player.isDead
            val job = player.job.ifBlank { UNKNOWN_VALUE }
            val team = player.team.ifBlank { UNKNOWN_VALUE }

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
            put(Key.SCHEMA_VERSION, SCHEMA_VERSION)
            put(Key.GENERATED_AT, Instant.now().toString())
            put(Key.SOURCE_ARCHIVE_DIRECTORY, archiveDir.toString())
            put(Key.OUTPUT_FILE, outputPath.toString())
            put(Key.SOURCE_ARCHIVE_FILE_COUNT, sourceFileCount)
            put(Key.PROCESSED_ARCHIVE_COUNT, state.processedArchives.size)
            put(Key.NEW_ARCHIVE_COUNT, newArchiveCount)
            put(Key.DUPLICATE_ARCHIVE_FILE_COUNT, duplicateArchiveFileCount)
            put(Key.UNREADABLE_ARCHIVE_COUNT, unreadableArchiveCount)
            put(Key.OVERVIEW, overviewJson(state.overview))
            put(Key.PROCESSED_ARCHIVES, processedArchivesJson(state.processedArchives))
            put(Key.BY_TEAM, bucketMapJson(state.byTeam))
            put(Key.BY_JOB, bucketMapJson(state.byJob))
            put(Key.BY_DISPLAYED_JOB, bucketMapJson(state.byDisplayedJob))
            put(Key.BY_ABILITY, bucketMapJson(state.byAbility))
            put(Key.BY_PLAYER, playerMapJson(state.byPlayer))
            put(Key.BY_JOB_ABILITY, jobAbilityJson(state.byJobAbility))
            put(Key.BY_PLAYER_JOB, playerJobJson(state.byPlayerJob))
            put(Key.BY_PLAYER_ABILITY, playerAbilityJson(state.byPlayerAbility))
            put(Key.ABILITY_USAGE, buildJsonObject {
                put(Key.BY_ABILITY_USAGE, usageMapJson(state.byUsedAbility))
                put(Key.BY_ABILITY_AND_JOB_USAGE, usageByJobJson(state.byUsedAbilityByJob))
                put(Key.BY_ABILITY_AND_PLAYER_USAGE, usageByPlayerJson(state.byUsedAbilityByPlayer))
            })
        }
    }

    private fun overviewJson(overview: OverviewStats): JsonObject {
        return buildJsonObject {
            put(Key.TOTAL_GAMES, overview.totalGames)
            put(Key.TOTAL_PLAYER_ENTRIES, overview.totalPlayerEntries)
            put(Key.AVERAGE_DAY_COUNT, rate(overview.totalDayCount, overview.totalGames))
            put(Key.AVERAGE_INITIAL_PLAYER_COUNT, rate(overview.totalInitialPlayerCount, overview.totalGames))
            put(Key.WINNING_TEAMS, counterJson(overview.winningTeams))
            put(Key.END_REASONS, counterJson(overview.endReasons))
            put(Key.MODES, counterJson(overview.modes))
            put(Key.INITIAL_PLAYER_COUNTS, counterJson(overview.initialPlayerCounts))
            put(Key.DAY_COUNTS, counterJson(overview.dayCounts))
            put(Key.REPLAY_EVENT_TYPES, counterJson(overview.replayEventTypes))
        }
    }

    private fun processedArchivesJson(records: Map<String, ArchiveRecord>): JsonObject {
        return buildJsonObject {
            records.entries
                .sortedBy { it.value.archivedAt ?: it.value.fileName }
                .forEach { (archiveId, record) ->
                    put(archiveId, buildJsonObject {
                        put(Key.FILE_NAME, record.fileName)
                        putNullable(Key.ARCHIVED_AT, record.archivedAt)
                        putNullable(Key.GUILD_ID, record.guildId)
                        putNullable(Key.GUILD_NAME, record.guildName)
                        put(Key.END_REASON, record.endReason)
                        putNullable(Key.WINNING_TEAM, record.winningTeam)
                        put(Key.PLAYER_COUNT, record.playerCount)
                        put(Key.INITIAL_PLAYER_COUNT, record.initialPlayerCount)
                        put(Key.DAY_COUNT, record.dayCount)
                        put(Key.MODE_TYPE, record.modeType)
                        put(Key.REPLAY_LOG_COUNT, record.replayLogCount)
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
                        put(Key.NAME, player.name)
                        putBucket(player.bucket)
                        put(Key.JOBS, bucketMapJson(player.jobs))
                        put(Key.ABILITIES, bucketMapJson(player.abilities))
                        put(Key.TEAMS, bucketMapJson(player.teams))
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
                        put(Key.JOB, combo.job)
                        put(Key.ABILITY, combo.ability)
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
                        put(Key.PLAYER_ID, combo.playerId)
                        put(Key.PLAYER_NAME, combo.playerName)
                        put(Key.JOB, combo.job)
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
                        put(Key.PLAYER_ID, combo.playerId)
                        put(Key.PLAYER_NAME, combo.playerName)
                        put(Key.ABILITY, combo.ability)
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
        put(Key.GAMES, bucket.appearances)
        put(Key.WINS, bucket.wins)
        put(Key.LOSSES, bucket.losses)
        put(Key.NO_CONTEST, bucket.noContest)
        put(Key.WIN_RATE, percent(bucket.wins, bucket.wins + bucket.losses))
        put(Key.SURVIVALS, bucket.survivals)
        put(Key.DEATHS, bucket.deaths)
        put(Key.SURVIVAL_RATE, percent(bucket.survivals, bucket.appearances))
        put(Key.AVERAGE_DAY_COUNT, rate(bucket.totalDayCount, bucket.appearances))
        put(Key.AVERAGE_INITIAL_PLAYER_COUNT, rate(bucket.totalInitialPlayerCount, bucket.appearances))
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
                        put(Key.ABILITY, aggregate.ability)
                        put(Key.JOB, aggregate.job)
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
                        put(Key.ABILITY, aggregate.ability)
                        put(Key.PLAYER_ID, aggregate.playerId)
                        put(Key.PLAYER_NAME, aggregate.playerName)
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
        put(Key.USES, bucket.uses)
        put(Key.SUCCESSES, bucket.successes)
        put(Key.FAILURES, bucket.failures)
        put(Key.UNKNOWN_RESULTS, bucket.unknownResults)
        put(Key.SUCCESS_RATE, percent(bucket.successes, bucket.successes + bucket.failures))
        put(Key.RESULTS, counterJson(bucket.results))
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

    private fun replayLogTypeLabel(type: String): String = when (type) {
        "GAME_START" -> "게임 시작"
        "GAME_END" -> "게임 종료"
        "PHASE_START" -> "페이즈 시작"
        "CHAT_PUBLIC" -> "공개 채팅"
        "CHAT_MAFIA" -> "마피아 채팅"
        "CHAT_COUPLE" -> "연인 채팅"
        "CHAT_DEAD" -> "사망자 채팅"
        "ABILITY_USED" -> "능력 사용"
        "VOTE_CAST" -> "투표"
        "PROS_CONS_VOTE" -> "찬반 투표"
        "DIRECT_MESSAGE" -> "개인 DM"
        "EPHEMERAL" -> "개인 응답"
        "SYSTEM_RESULT" -> "시스템 결과"
        "DEATH" -> "사망"
        "REVIVE" -> "부활"
        else -> type.ifBlank { UNKNOWN_VALUE }
    }

    private fun endReasonLabel(reason: String): String = when (reason) {
        "WIN_CONDITION_MET" -> "승리 조건 충족"
        "FORCED_STOP" -> "강제 종료"
        else -> reason.ifBlank { UNKNOWN_VALUE }
    }

    private fun MutableMap<String, StatBucket>.bucket(key: String): StatBucket {
        return getOrPut(key.ifBlank { UNKNOWN_VALUE }) { StatBucket() }
    }

    private fun MutableMap<String, Int>.increment(key: String, amount: Int = 1) {
        val normalized = key.ifBlank { UNKNOWN_VALUE }
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
        return getOrPut(ability.ifBlank { UNKNOWN_VALUE }) { UsageBucket() }
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
        val modeType: String,
        val modeDisplayName: String,
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
        val modeType: String,
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
            winningTeams.increment(archive.winningTeam ?: "승리팀 없음")
            endReasons.increment(endReasonLabel(archive.endReason))
            modes.increment(archive.modeDisplayName)
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
            val normalizedResult = result?.takeIf { it.isNotBlank() } ?: UNKNOWN_VALUE
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

    private const val UNKNOWN_VALUE = "알 수 없음"
}
