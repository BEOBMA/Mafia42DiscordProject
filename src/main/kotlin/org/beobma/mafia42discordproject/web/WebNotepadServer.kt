package org.beobma.mafia42discordproject.web

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Member
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.coroutines.runBlocking
import org.beobma.mafia42discordproject.command.AbilityUseCommand
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GameManager
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.replay.GameReplayRenderDataStore
import org.beobma.mafia42discordproject.game.replay.ReplayArchiveRepository
import org.beobma.mafia42discordproject.game.replay.ReplayRenderData
import org.beobma.mafia42discordproject.game.replay.ReplayLogEntry
import org.beobma.mafia42discordproject.game.system.FrogCurseManager
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.JobManager
import org.beobma.mafia42discordproject.job.definition.list.MentalPatient
import org.beobma.mafia42discordproject.job.evil.Evil
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

sealed interface NotepadAccessResult {
    data class Success(val url: String) : NotepadAccessResult
    data class Failure(val message: String) : NotepadAccessResult
}

object WebNotepadServer {
    private const val DEFAULT_HOST = "127.0.0.1"
    private const val DEFAULT_PORT = 8080
    private const val SESSION_COOKIE = "mafia_notepad_session"
    private const val SESSION_LIFETIME_MILLIS = 8 * 60 * 60 * 1000L
    private const val MAX_REQUEST_BODY_BYTES = 8 * 1024
    private const val MAX_NOTE_LENGTH = 1_000
    private const val JOB_ICON_BASE_URL =
        "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia"

    private val primaryMemoJobNames = listOf("마피아", "경찰", "형사", "요원", "자경단원", "의사")

    private val host = System.getenv("WEB_HOST")
        ?.trim()
        ?.removePrefix("http://")
        ?.removePrefix("https://")
        ?.trimEnd('/')
        ?.takeIf(String::isNotEmpty)
        ?: DEFAULT_HOST
    private val port = System.getenv("WEB_PORT")?.toIntOrNull()?.takeIf { it in 1..65535 } ?: DEFAULT_PORT
    private val localBaseUrl = "http://$host:$port"
    private val publicBaseUrl = System.getenv("WEB_PUBLIC_URL")
        ?.trim()
        ?.trimEnd('/')
        ?.takeIf(String::isNotEmpty)
        ?: localBaseUrl

    private val json = Json { ignoreUnknownKeys = true }
    private val secureRandom = SecureRandom()
    private val sessions = ConcurrentHashMap<String, WebSession>()
    private val notes = ConcurrentHashMap<NoteKey, PlayerNote>()
    private val laboratoryGames = LaboratoryGameService(LaboratoryJobCatalog::definitions)
    @Volatile
    private var server: HttpServer? = null
    private var executor: ExecutorService? = null

    @Synchronized
    fun start() {
        startOnPort(port)
    }

    @Synchronized
    internal fun startForTests(): Int {
        startOnPort(0)
        return requireNotNull(server) { "웹 메모장 테스트 서버를 시작하지 못했습니다." }.address.port
    }

    private fun startOnPort(bindPort: Int) {
        if (server != null) return

        val created = try {
            HttpServer.create(InetSocketAddress(host, bindPort), 0)
        } catch (error: Exception) {
            println("[WebNotepad] 로컬 서버 시작 실패: ${error.message}")
            return
        }

        created.createContext("/") { exchange ->
            runCatching { route(exchange) }
                .onFailure { error ->
                    println("[WebNotepad] 요청 처리 실패: ${error.message}")
                    if (!exchange.responseHeaders.containsKey("Content-Type")) {
                        sendText(exchange, 500, "서버에서 요청을 처리하지 못했습니다.")
                    }
                }
        }
        val createdExecutor = Executors.newFixedThreadPool(4) { task ->
            Thread(task, "web-notepad").apply { isDaemon = true }
        }
        created.executor = createdExecutor
        created.start()
        executor = createdExecutor
        server = created
        println("[WebServer] 리플레이 아카이브: $publicBaseUrl")
        println("[WebServer] 게임 메모장: $localBaseUrl/notepad")
    }

    @Synchronized
    fun stop() {
        server?.stop(1)
        server = null
        executor?.shutdownNow()
        executor = null
        sessions.clear()
        notes.clear()
        laboratoryGames.clear()
    }

    fun issueAccessUrl(userId: Snowflake): NotepadAccessResult {
        if (server == null) {
            start()
        }
        if (server == null) {
            return NotepadAccessResult.Failure("로컬 메모장 서버가 실행 중이지 않습니다.")
        }

        val game = GameManager.getCurrentGameFor(userId)
            ?: return NotepadAccessResult.Failure("현재 게임에 참여 중인 플레이어만 메모장을 열 수 있습니다.")

        val rawToken = ByteArray(32).also(secureRandom::nextBytes)
            .let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }
        val now = System.currentTimeMillis()
        sessions[tokenHash(rawToken)] = WebSession(
            userId = userId.value,
            gameKey = game.key(),
            expiresAtMillis = now + SESSION_LIFETIME_MILLIS
        )
        pruneExpiredSessions(now)

        return NotepadAccessResult.Success(publicUrl("/session?token=$rawToken"))
    }

    fun replayUrl(data: ReplayRenderData): String {
        val uuid = data.replayUuid.ifBlank {
            GameReplayRenderDataStore.replayUuid(data.guildId, data.replayStartedAtMillis)
        }
        return publicUrl("/history/$uuid")
    }

    private fun publicUrl(path: String): String = "$publicBaseUrl/${path.trimStart('/')}"

    fun invalidateGame(game: Game) {
        val gameKey = game.key()
        sessions.entries.removeIf { it.value.gameKey == gameKey }
        notes.keys.removeIf { it.gameKey == gameKey }
    }

    private fun route(exchange: HttpExchange) {
        setSecurityHeaders(exchange)
        val path = exchange.requestURI.path
        when {
            path == "/" -> serveResource(exchange, "/web/replay/index.html", "text/html; charset=utf-8")
            path == "/replay.css" -> serveResource(exchange, "/web/replay/replay.css", "text/css; charset=utf-8")
            path == "/replay.js" -> serveResource(exchange, "/web/replay/replay.js", "text/javascript; charset=utf-8")
            path == "/history" || path.startsWith("/history/") ->
                serveResource(exchange, "/web/replay/index.html", "text/html; charset=utf-8")
            path == "/api/replays" -> serveReplayList(exchange)
            path.startsWith("/api/replays/") -> serveReplay(exchange, path.removePrefix("/api/replays/"))
            path == "/notepad" -> serveResource(exchange, "/web/notepad/index.html", "text/html; charset=utf-8")
            path == "/app.css" -> serveResource(exchange, "/web/notepad/app.css", "text/css; charset=utf-8")
            path == "/app.js" -> serveResource(exchange, "/web/notepad/app.js", "text/javascript; charset=utf-8")
            path == "/lab" -> serveResource(exchange, "/web/lab/index.html", "text/html; charset=utf-8")
            path == "/lab.css" -> serveResource(exchange, "/web/lab/lab.css", "text/css; charset=utf-8")
            path == "/lab.js" -> serveResource(exchange, "/web/lab/lab.js", "text/javascript; charset=utf-8")
            path == "/session" -> exchangeSessionToken(exchange)
            path == "/api/lab/session" -> createLaboratorySession(exchange)
            path == "/api/lab/state" -> serveLaboratoryState(exchange)
            path == "/api/lab/setup" -> updateLaboratorySetup(exchange)
            path == "/api/lab/start" -> startLaboratoryGame(exchange)
            path == "/api/lab/action" -> submitLaboratoryAction(exchange)
            path == "/api/lab/vote" -> castLaboratoryVote(exchange)
            path == "/api/lab/pros-cons" -> castLaboratoryProsConsVote(exchange)
            path == "/api/lab/player-state" -> setLaboratoryPlayerState(exchange)
            path == "/api/lab/advance" -> advanceLaboratoryGame(exchange)
            path == "/api/lab/reset" -> resetLaboratoryGame(exchange)
            path == "/api/state" -> serveState(exchange)
            path == "/api/events" -> serveEvents(exchange)
            path == "/api/ability" -> useAbility(exchange)
            path == "/api/note" -> saveNote(exchange)
            path == "/health" -> sendJson(exchange, 200, buildJsonObject { put("status", "ok") })
            else -> sendText(exchange, 404, "페이지를 찾을 수 없습니다.")
        }
    }

    private fun serveReplayList(exchange: HttpExchange) {
        if (exchange.requestMethod != "GET") {
            sendMethodNotAllowed(exchange, "GET")
            return
        }
        val replays = ReplayArchiveRepository.list()
        sendJson(exchange, 200, buildJsonObject {
            put("count", replays.size)
            put("replays", replays)
        })
    }

    private fun serveReplay(exchange: HttpExchange, uuid: String) {
        if (exchange.requestMethod != "GET") {
            sendMethodNotAllowed(exchange, "GET")
            return
        }
        val replay = ReplayArchiveRepository.find(uuid)
        if (replay == null) {
            sendJsonError(exchange, 404, "해당 UUID의 리플레이를 찾을 수 없습니다.")
            return
        }
        sendJson(exchange, 200, replay)
    }

    private fun exchangeSessionToken(exchange: HttpExchange) {
        if (exchange.requestMethod != "GET") {
            sendMethodNotAllowed(exchange, "GET")
            return
        }

        val rawToken = queryParameters(exchange)["token"]
        val session = rawToken?.let { sessions.remove(tokenHash(it)) }
        val game = session?.let(::currentGameFor)
        if (rawToken.isNullOrBlank() || session == null || game == null) {
            sendText(exchange, 401, "유효하지 않거나 만료된 메모장 링크입니다.")
            return
        }

        val sessionToken = ByteArray(32).also(secureRandom::nextBytes)
            .let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }
        sessions[tokenHash(sessionToken)] = session
        val secureAttribute = if (publicBaseUrl.startsWith("https://", ignoreCase = true)) "; Secure" else ""
        exchange.responseHeaders.add(
            "Set-Cookie",
            "$SESSION_COOKIE=$sessionToken; Path=/; HttpOnly; SameSite=Strict$secureAttribute; Max-Age=${SESSION_LIFETIME_MILLIS / 1000}"
        )
        exchange.responseHeaders.add("Location", "/notepad")
        exchange.sendResponseHeaders(303, -1)
        exchange.close()
    }

    private fun createLaboratorySession(exchange: HttpExchange) {
        if (exchange.requestMethod != "POST") {
            sendMethodNotAllowed(exchange, "POST")
            return
        }
        if (!isSameOrigin(exchange)) {
            sendJsonError(exchange, 403, "허용되지 않은 요청 출처입니다.")
            return
        }
        val created = laboratoryGames.createSession()
        sendJson(exchange, 201, buildJsonObject {
            put("token", created.token)
            put("state", laboratorySnapshotJson(created.snapshot))
        })
    }

    private fun serveLaboratoryState(exchange: HttpExchange) {
        if (exchange.requestMethod != "GET") {
            sendMethodNotAllowed(exchange, "GET")
            return
        }
        val snapshot = laboratoryGames.getState(laboratoryToken(exchange))
        if (snapshot == null) {
            sendJsonError(exchange, 401, "실험실 세션이 없거나 만료되었습니다.")
            return
        }
        sendJson(exchange, 200, laboratorySnapshotJson(snapshot))
    }

    private fun updateLaboratorySetup(exchange: HttpExchange) {
        val payload = laboratoryMutationPayload(exchange, "PUT") ?: return
        val players = payload["players"] as? kotlinx.serialization.json.JsonArray
        if (players == null) {
            sendJsonError(exchange, 400, "참가자 설정이 필요합니다.")
            return
        }
        val setup = players.mapNotNull { element ->
            val player = element as? JsonObject ?: return@mapNotNull null
            val name = player["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val jobName = player["jobName"]?.jsonPrimitive?.contentOrNull
            LaboratoryPlayerSetup(name, jobName)
        }
        if (setup.size != players.size) {
            sendJsonError(exchange, 400, "올바른 참가자 설정이 아닙니다.")
            return
        }
        sendLaboratoryResult(exchange, laboratoryGames.updateSetup(laboratoryToken(exchange), setup))
    }

    private fun startLaboratoryGame(exchange: HttpExchange) {
        if (!validateLaboratoryMutation(exchange, "POST")) return
        sendLaboratoryResult(exchange, laboratoryGames.start(laboratoryToken(exchange)))
    }

    private fun submitLaboratoryAction(exchange: HttpExchange) {
        val payload = laboratoryMutationPayload(exchange, "POST") ?: return
        val actorId = payload["actorId"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val abilityName = payload["abilityName"]?.jsonPrimitive?.contentOrNull.orEmpty()
        if (actorId.isBlank() || abilityName.isBlank()) {
            sendJsonError(exchange, 400, "플레이어와 능력을 선택해 주세요.")
            return
        }
        sendLaboratoryResult(
            exchange,
            laboratoryGames.submitAction(
                token = laboratoryToken(exchange),
                actorId = actorId,
                abilityName = abilityName,
                targetId = payload["targetId"]?.jsonPrimitive?.contentOrNull,
                selectedJobName = payload["selectedJobName"]?.jsonPrimitive?.contentOrNull
            )
        )
    }

    private fun castLaboratoryVote(exchange: HttpExchange) {
        val payload = laboratoryMutationPayload(exchange, "POST") ?: return
        val voterId = payload["voterId"]?.jsonPrimitive?.contentOrNull.orEmpty()
        if (voterId.isBlank()) {
            sendJsonError(exchange, 400, "투표자를 선택해 주세요.")
            return
        }
        sendLaboratoryResult(
            exchange,
            laboratoryGames.castMainVote(
                laboratoryToken(exchange),
                voterId,
                payload["targetId"]?.jsonPrimitive?.contentOrNull
            )
        )
    }

    private fun castLaboratoryProsConsVote(exchange: HttpExchange) {
        val payload = laboratoryMutationPayload(exchange, "POST") ?: return
        val voterId = payload["voterId"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val isPros = payload["isPros"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()
        if (voterId.isBlank() || isPros == null) {
            sendJsonError(exchange, 400, "찬반 투표 정보를 확인해 주세요.")
            return
        }
        sendLaboratoryResult(
            exchange,
            laboratoryGames.castProsConsVote(laboratoryToken(exchange), voterId, isPros)
        )
    }

    private fun setLaboratoryPlayerState(exchange: HttpExchange) {
        val payload = laboratoryMutationPayload(exchange, "PUT") ?: return
        val playerId = payload["playerId"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val isAlive = payload["isAlive"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()
        if (playerId.isBlank() || isAlive == null) {
            sendJsonError(exchange, 400, "플레이어 상태 정보를 확인해 주세요.")
            return
        }
        sendLaboratoryResult(
            exchange,
            laboratoryGames.setPlayerAlive(laboratoryToken(exchange), playerId, isAlive)
        )
    }

    private fun advanceLaboratoryGame(exchange: HttpExchange) {
        if (!validateLaboratoryMutation(exchange, "POST")) return
        sendLaboratoryResult(exchange, laboratoryGames.advance(laboratoryToken(exchange)))
    }

    private fun resetLaboratoryGame(exchange: HttpExchange) {
        if (!validateLaboratoryMutation(exchange, "POST")) return
        sendLaboratoryResult(exchange, laboratoryGames.reset(laboratoryToken(exchange)))
    }

    private fun serveState(exchange: HttpExchange) {
        if (exchange.requestMethod != "GET") {
            sendMethodNotAllowed(exchange, "GET")
            return
        }

        val authenticated = authenticate(exchange) ?: return
        val viewer = authenticated.game.getPlayer(Snowflake(authenticated.session.userId))
        if (viewer == null) {
            clearSessionCookie(exchange)
            sendJsonError(exchange, 401, "게임 참가자 정보를 찾을 수 없습니다.")
            return
        }

        sendJson(exchange, 200, buildState(authenticated.game, viewer))
    }

    private fun serveEvents(exchange: HttpExchange) {
        if (exchange.requestMethod != "GET") {
            sendMethodNotAllowed(exchange, "GET")
            return
        }
        val authenticated = authenticate(exchange) ?: return
        val viewer = authenticated.game.getPlayer(Snowflake(authenticated.session.userId))
        if (viewer == null) {
            sendJsonError(exchange, 401, "게임 참가자 정보를 찾을 수 없습니다.")
            return
        }
        val after = queryParameters(exchange)["after"]?.toLongOrNull()?.coerceAtLeast(0) ?: 0L
        sendJson(exchange, 200, buildVisibleEvents(authenticated.game, viewer, after))
    }

    private fun useAbility(exchange: HttpExchange) {
        if (exchange.requestMethod != "POST") {
            sendMethodNotAllowed(exchange, "POST")
            return
        }
        if (!isSameOrigin(exchange)) {
            sendJsonError(exchange, 403, "허용되지 않은 요청 출처입니다.")
            return
        }
        val authenticated = authenticate(exchange) ?: return
        val caster = authenticated.game.getPlayer(Snowflake(authenticated.session.userId))
        if (caster == null) {
            sendJsonError(exchange, 401, "게임 참가자 정보를 찾을 수 없습니다.")
            return
        }
        val requestBody = readRequestBody(exchange) ?: return
        val payload = runCatching { json.parseToJsonElement(requestBody).jsonObject }.getOrNull()
        if (payload == null) {
            sendJsonError(exchange, 400, "올바른 JSON 요청이 아닙니다.")
            return
        }
        val abilityName = payload["abilityName"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        if (abilityName.isBlank()) {
            sendJsonError(exchange, 400, "사용할 능력을 선택해 주세요.")
            return
        }
        val targetIdText = payload["targetId"]?.jsonPrimitive?.contentOrNull
        val target = targetIdText?.let { id ->
            runCatching { Snowflake(id) }.getOrNull()?.let(authenticated.game::getPlayer)
        }
        if (targetIdText != null && target == null) {
            sendJsonError(exchange, 400, "능력 대상을 찾을 수 없습니다.")
            return
        }
        val selectedJobName = payload["selectedJobName"]?.jsonPrimitive?.contentOrNull
            ?.trim()
            ?.takeIf(String::isNotEmpty)
        val result = runBlocking {
            AbilityUseCommand.executeAbility(
                game = authenticated.game,
                caster = caster,
                abilityName = abilityName,
                target = target,
                selectedJobName = selectedJobName
            )
        }
        sendJson(exchange, if (result.isSuccess) 200 else 409, buildJsonObject {
            put("success", result.isSuccess)
            put("message", result.message)
        })
    }

    private fun saveNote(exchange: HttpExchange) {
        if (exchange.requestMethod != "PUT") {
            sendMethodNotAllowed(exchange, "PUT")
            return
        }
        if (!isSameOrigin(exchange)) {
            sendJsonError(exchange, 403, "허용되지 않은 요청 출처입니다.")
            return
        }

        val authenticated = authenticate(exchange) ?: return
        val requestBody = readRequestBody(exchange) ?: return
        val payload = runCatching { json.parseToJsonElement(requestBody).jsonObject }.getOrNull()
        if (payload == null) {
            sendJsonError(exchange, 400, "올바른 JSON 요청이 아닙니다.")
            return
        }

        val targetId = payload["targetId"]?.jsonPrimitive?.contentOrNull
            ?.let { runCatching { Snowflake(it) }.getOrNull() }
        val target = targetId?.let(authenticated.game::getPlayer)
        if (target == null || target.member.id.value == authenticated.session.userId) {
            sendJsonError(exchange, 400, "메모 대상을 찾을 수 없습니다.")
            return
        }

        val guessedJobName = payload["guessedJobName"]?.jsonPrimitive?.contentOrNull
            ?.trim()
            ?.takeIf(String::isNotEmpty)
        if (guessedJobName != null && JobManager.findByName(guessedJobName) == null) {
            sendJsonError(exchange, 400, "존재하지 않는 직업입니다.")
            return
        }

        val content = payload["content"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        if (content.length > MAX_NOTE_LENGTH) {
            sendJsonError(exchange, 400, "메모는 ${MAX_NOTE_LENGTH}자 이하로 작성해 주세요.")
            return
        }

        val key = NoteKey(
            gameKey = authenticated.session.gameKey,
            ownerId = authenticated.session.userId,
            targetId = target.member.id.value
        )
        if (guessedJobName == null && content.isBlank()) {
            notes.remove(key)
        } else {
            notes[key] = PlayerNote(guessedJobName, content, System.currentTimeMillis())
        }

        sendJson(exchange, 200, buildJsonObject { put("saved", true) })
    }

    private fun buildState(game: Game, viewer: PlayerData): JsonObject {
        val gameKey = game.key()
        val players = game.playerDatas.toList()
        val viewerDisplayJob = (viewer.job as? MentalPatient)?.displayedJob ?: viewer.job
        val viewerMemoJob = game.privateDisplayedJobNamesByObserver[viewer.member.id]
            ?.get(viewer.member.id)
            ?.let(JobManager::findByName)
            ?: viewerDisplayJob
        val viewerAbilities = when (val actualJob = viewer.job) {
            is MentalPatient -> actualJob.activeAbilitySourceAbilities()
            else -> viewer.allAbilities
        }.distinctBy { it.name }

        return buildJsonObject {
            put("game", buildJsonObject {
                put("guildName", game.guild.name)
                put("dayCount", game.dayCount)
                put("phase", game.currentPhase.name)
                put("phaseLabel", game.currentPhase.displayName())
                put("mode", game.mode.displayName)
                put("isRunning", game.isRunning)
                put("aliveCount", players.count { !it.state.isDead })
                put("playerCount", players.size)
            })
            put("me", buildJsonObject {
                put("id", viewer.member.id.value.toString())
                put("name", viewer.member.effectiveName)
                put("avatarUrl", viewer.member.avatarUrl())
                putNullableJob("job", viewerMemoJob)
                put("abilities", buildJsonArray {
                    viewerAbilities.forEach { ability ->
                        add(buildJsonObject {
                            put("name", ability.name)
                            put("description", ability.description)
                            put("image", ability.image)
                        })
                    }
                })
                put("actionAbilities", buildJsonArray {
                    AbilityUseCommand.getAbilityActionOptions(game, viewer).forEach { ability ->
                        add(buildJsonObject {
                            put("name", ability.name)
                            put("description", ability.description)
                            put("image", ability.image)
                            put("requiresTarget", ability.requiresTarget)
                            put("requiresJobSelection", ability.requiresJobSelection)
                            put("selectableJobNames", buildJsonArray {
                                ability.selectableJobNames.forEach { add(JsonPrimitive(it)) }
                            })
                        })
                    }
                })
            })
            put("players", buildJsonArray {
                players.forEach { player ->
                    val isSelf = player.member.id == viewer.member.id
                    val isPublic = player.state.isJobPubliclyRevealed
                    val privateMemoJob = game.privateDisplayedJobNamesByObserver[viewer.member.id]
                        ?.get(player.member.id)
                        ?.let(JobManager::findByName)
                    val visibleJob = when {
                        isSelf -> viewerMemoJob
                        isPublic -> FrogCurseManager.displayedJob(player)
                        privateMemoJob != null -> privateMemoJob
                        else -> null
                    }
                    val note = notes[NoteKey(gameKey, viewer.member.id.value, player.member.id.value)]
                    add(buildJsonObject {
                        put("id", player.member.id.value.toString())
                        put("name", player.member.effectiveName)
                        put("isSelf", isSelf)
                        put("isDead", player.state.isDead)
                        put("isJobPublic", isPublic)
                        putNullableJob("job", visibleJob)
                        put("note", buildJsonObject {
                            putNullableString("guessedJobName", note?.guessedJobName)
                            put("content", note?.content.orEmpty())
                        })
                    })
                }
            })
            put("jobs", buildJsonArray {
                memoJobsByRow().forEach { (rowNumber, jobs) ->
                    jobs.forEach { job ->
                        add(buildJsonObject {
                            put("name", job.name)
                            put("image", "$JOB_ICON_BASE_URL/${job.javaClass.simpleName.lowercase()}_icon.webp")
                            put("memoRow", rowNumber)
                        })
                    }
                }
            })
        }
    }

    /** 메모장의 의미별 행을 고정한다. 각 행은 UI 규칙상 최대 여섯 직업만 포함한다. */
    private fun memoJobsByRow(): List<Pair<Int, List<Job>>> {
        val allJobs = JobManager.getAll().distinctBy(Job::name)
        val jobsByName = allJobs.associateBy(Job::name)
        val primary = primaryMemoJobNames.mapNotNull(jobsByName::get)
        val villain = jobsByName["악인"]
        val assistants = allJobs.filter { it is Evil && it.name !in setOf("마피아", "악인") }
        val specialJobs = allJobs.filter { job ->
            job !is Evil && job.name !in primaryMemoJobNames && job.name != "시민"
        } + listOfNotNull(jobsByName["시민"])

        return buildList {
            add(1 to primary.take(6))
            add(2 to assistants.take(6))
            add(3 to (assistants.drop(6).take(5) + listOfNotNull(villain)))
            specialJobs.chunked(6).forEachIndexed { index, jobs -> add(index + 4 to jobs) }
        }.filter { (_, jobs) -> jobs.isNotEmpty() }
    }

    private fun buildVisibleEvents(game: Game, viewer: PlayerData, after: Long): JsonObject {
        val snapshot = synchronized(game) { game.replayLogs.toList() }
        val lastSequence = snapshot.maxOfOrNull(ReplayLogEntry::sequence) ?: 0L
        return buildJsonObject {
            put("lastSequence", lastSequence)
            put("events", buildJsonArray {
                snapshot.asSequence()
                    .filter { it.sequence > after }
                    .filter { LiveEventVisibility.canView(it, viewer.member.id) }
                    .forEach { entry ->
                        add(buildJsonObject {
                            put("sequence", entry.sequence)
                            put("timestampMillis", entry.timestampMillis)
                            put("dayCount", entry.dayCount)
                            put("phase", entry.phase.name)
                            put("type", entry.type.name)
                            putNullableString("actorName", entry.actorName)
                            put("title", entry.title)
                            put("body", entry.body)
                            put("imageUrls", buildJsonArray {
                                entry.imageUrls.forEach { add(JsonPrimitive(it)) }
                            })
                        })
                    }
            })
        }
    }

    private fun laboratorySnapshotJson(snapshot: LaboratorySnapshot): JsonObject = buildJsonObject {
        put("phase", snapshot.phase.name)
        put("phaseLabel", snapshot.phase.label)
        put("dayCount", snapshot.dayCount)
        putNullableString("defenseTargetId", snapshot.defenseTargetId)
        put("players", buildJsonArray {
            snapshot.players.forEach { player ->
                add(buildJsonObject {
                    put("id", player.id)
                    put("name", player.name)
                    put("isHuman", player.isHuman)
                    put("isAlive", player.isAlive)
                    putNullableString("jobName", player.jobName)
                    putNullableString("jobImage", player.jobImage)
                    put("abilities", buildJsonArray {
                        player.abilities.forEach { ability -> add(laboratoryAbilityJson(ability)) }
                    })
                })
            }
        })
        put("jobs", buildJsonArray {
            snapshot.jobs.forEach { job ->
                add(buildJsonObject {
                    put("name", job.name)
                    put("description", job.description)
                    putNullableString("image", job.image)
                    put("isEvil", job.isEvil)
                    put("abilities", buildJsonArray {
                        job.abilities.forEach { ability -> add(laboratoryAbilityJson(ability)) }
                    })
                })
            }
        })
        put("actions", buildJsonArray {
            snapshot.actions.forEach { action ->
                add(buildJsonObject {
                    put("actorId", action.actorId)
                    put("actorName", action.actorName)
                    put("abilityName", action.abilityName)
                    putNullableString("targetId", action.targetId)
                    putNullableString("targetName", action.targetName)
                    putNullableString("selectedJobName", action.selectedJobName)
                })
            }
        })
        put("votes", buildJsonObject {
            snapshot.votes.forEach { (voterId, targetId) -> putNullableString(voterId, targetId) }
        })
        put("prosConsVotes", buildJsonObject {
            snapshot.prosConsVotes.forEach { (voterId, isPros) -> put(voterId, isPros) }
        })
        put("events", buildJsonArray {
            snapshot.events.forEach { event ->
                add(buildJsonObject {
                    put("sequence", event.sequence)
                    put("dayCount", event.dayCount)
                    put("phase", event.phase.name)
                    put("phaseLabel", event.phase.label)
                    put("title", event.title)
                    put("body", event.body)
                })
            }
        })
    }

    private fun laboratoryAbilityJson(ability: LaboratoryAbilityDefinition): JsonObject = buildJsonObject {
        put("name", ability.name)
        put("description", ability.description)
        put("image", ability.image)
        put("phase", ability.phase.name)
        put("requiresTarget", ability.requiresTarget)
        put("requiresJobSelection", ability.requiresJobSelection)
    }

    private fun laboratoryMutationPayload(exchange: HttpExchange, method: String): JsonObject? {
        if (!validateLaboratoryMutation(exchange, method)) return null
        val requestBody = readRequestBody(exchange) ?: return null
        return runCatching { json.parseToJsonElement(requestBody).jsonObject }.getOrElse {
            sendJsonError(exchange, 400, "올바른 JSON 요청이 아닙니다.")
            return null
        }
    }

    private fun validateLaboratoryMutation(exchange: HttpExchange, method: String): Boolean {
        if (exchange.requestMethod != method) {
            sendMethodNotAllowed(exchange, method)
            return false
        }
        if (!isSameOrigin(exchange)) {
            sendJsonError(exchange, 403, "허용되지 않은 요청 출처입니다.")
            return false
        }
        if (laboratoryToken(exchange).isNullOrBlank()) {
            sendJsonError(exchange, 401, "실험실 세션이 필요합니다.")
            return false
        }
        return true
    }

    private fun laboratoryToken(exchange: HttpExchange): String? =
        exchange.requestHeaders.getFirst("X-Lab-Session")?.trim()?.takeIf(String::isNotEmpty)

    private fun sendLaboratoryResult(exchange: HttpExchange, result: LaboratoryOperationResult) {
        val status = if (result.success) 200 else if (result.message.contains("세션")) 401 else 409
        sendJson(exchange, status, buildJsonObject {
            put("success", result.success)
            put("message", result.message)
            result.snapshot?.let { put("state", laboratorySnapshotJson(it)) }
        })
    }

    private fun authenticate(exchange: HttpExchange): AuthenticatedSession? {
        val rawToken = cookieValue(exchange, SESSION_COOKIE)
        val session = rawToken?.let { sessions[tokenHash(it)] }
        val game = session?.let(::currentGameFor)
        if (session == null || game == null) {
            rawToken?.let { sessions.remove(tokenHash(it)) }
            clearSessionCookie(exchange)
            sendJsonError(exchange, 401, "Discord에서 /메모장 명령으로 새 링크를 받아 주세요.")
            return null
        }
        return AuthenticatedSession(session, game)
    }

    private fun currentGameFor(session: WebSession): Game? {
        if (session.expiresAtMillis <= System.currentTimeMillis()) return null
        val game = GameManager.getCurrentGameFor(Snowflake(session.userId)) ?: return null
        return game.takeIf { it.key() == session.gameKey }
    }

    private fun serveResource(exchange: HttpExchange, resourcePath: String, contentType: String) {
        if (exchange.requestMethod != "GET") {
            sendMethodNotAllowed(exchange, "GET")
            return
        }
        val bytes = WebNotepadServer::class.java.getResourceAsStream(resourcePath)?.use { it.readBytes() }
        if (bytes == null) {
            sendText(exchange, 500, "웹 리소스를 불러올 수 없습니다.")
            return
        }
        sendBytes(exchange, 200, contentType, bytes)
    }

    private fun readRequestBody(exchange: HttpExchange): String? {
        val bytes = exchange.requestBody.use { it.readNBytes(MAX_REQUEST_BODY_BYTES + 1) }
        if (bytes.size > MAX_REQUEST_BODY_BYTES) {
            sendJsonError(exchange, 413, "요청 내용이 너무 큽니다.")
            return null
        }
        return bytes.toString(StandardCharsets.UTF_8)
    }

    private fun isSameOrigin(exchange: HttpExchange): Boolean {
        val origin = exchange.requestHeaders.getFirst("Origin") ?: return true
        return origin.trimEnd('/') == localBaseUrl || origin.trimEnd('/') == publicBaseUrl
    }

    private fun queryParameters(exchange: HttpExchange): Map<String, String> {
        val rawQuery = exchange.requestURI.rawQuery ?: return emptyMap()
        return rawQuery.split('&').mapNotNull { part ->
            val pieces = part.split('=', limit = 2)
            val key = URLDecoder.decode(pieces[0], StandardCharsets.UTF_8)
            val value = URLDecoder.decode(pieces.getOrElse(1) { "" }, StandardCharsets.UTF_8)
            key to value
        }.toMap()
    }

    private fun cookieValue(exchange: HttpExchange, name: String): String? =
        exchange.requestHeaders.getFirst("Cookie")
            ?.split(';')
            ?.map(String::trim)
            ?.firstNotNullOfOrNull { cookie ->
                val separator = cookie.indexOf('=')
                if (separator <= 0 || cookie.substring(0, separator) != name) null
                else cookie.substring(separator + 1)
            }

    private fun setSecurityHeaders(exchange: HttpExchange) {
        exchange.responseHeaders.set("Cache-Control", "no-store")
        exchange.responseHeaders.set("Referrer-Policy", "no-referrer")
        exchange.responseHeaders.set("X-Content-Type-Options", "nosniff")
        exchange.responseHeaders.set("X-Frame-Options", "DENY")
        exchange.responseHeaders.set(
            "Content-Security-Policy",
            "default-src 'self'; img-src 'self' data: https:; style-src 'self'; script-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'"
        )
    }

    private fun clearSessionCookie(exchange: HttpExchange) {
        exchange.responseHeaders.add(
            "Set-Cookie",
            "$SESSION_COOKIE=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0"
        )
    }

    private fun sendMethodNotAllowed(exchange: HttpExchange, allowed: String) {
        exchange.responseHeaders.set("Allow", allowed)
        sendText(exchange, 405, "허용되지 않은 요청 방식입니다.")
    }

    private fun sendJsonError(exchange: HttpExchange, status: Int, message: String) {
        sendJson(exchange, status, buildJsonObject { put("error", message) })
    }

    private fun sendJson(exchange: HttpExchange, status: Int, body: JsonElement) {
        sendBytes(
            exchange,
            status,
            "application/json; charset=utf-8",
            body.toString().toByteArray(StandardCharsets.UTF_8)
        )
    }

    private fun sendText(exchange: HttpExchange, status: Int, message: String) {
        sendBytes(
            exchange,
            status,
            "text/plain; charset=utf-8",
            message.toByteArray(StandardCharsets.UTF_8)
        )
    }

    private fun sendBytes(exchange: HttpExchange, status: Int, contentType: String, bytes: ByteArray) {
        exchange.responseHeaders.set("Content-Type", contentType)
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun tokenHash(token: String): String = MessageDigest.getInstance("SHA-256")
        .digest(token.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }

    private fun pruneExpiredSessions(now: Long) {
        sessions.entries.removeIf { it.value.expiresAtMillis <= now }
    }

    private fun Game.key(): GameKey = GameKey(guild.id.value, replayStartedAtMillis)

    private fun GamePhase.displayName(): String = when (this) {
        GamePhase.DAY -> "낮"
        GamePhase.NIGHT -> "밤"
        GamePhase.DAWN -> "새벽"
        GamePhase.VOTE -> "투표"
        GamePhase.END -> "종료"
    }

    private fun Member.avatarUrl(): String =
        (memberAvatar ?: avatar ?: defaultAvatar).cdnUrl.toUrl()

    private fun kotlinx.serialization.json.JsonObjectBuilder.putNullableString(key: String, value: String?) {
        if (value == null) put(key, JsonNull) else put(key, value)
    }

    private fun kotlinx.serialization.json.JsonObjectBuilder.putNullableJob(key: String, job: Job?) {
        if (job == null) {
            put(key, JsonNull)
            return
        }
        put(key, buildJsonObject {
            put("name", job.name)
            put("description", job.description)
            putNullableString("image", job.jobImage)
        })
    }

    private data class WebSession(
        val userId: ULong,
        val gameKey: GameKey,
        val expiresAtMillis: Long
    )

    private data class GameKey(val guildId: ULong, val startedAtMillis: Long)

    private data class NoteKey(val gameKey: GameKey, val ownerId: ULong, val targetId: ULong)

    private data class PlayerNote(
        val guessedJobName: String?,
        val content: String,
        val updatedAtMillis: Long
    )

    private data class AuthenticatedSession(val session: WebSession, val game: Game)
}
