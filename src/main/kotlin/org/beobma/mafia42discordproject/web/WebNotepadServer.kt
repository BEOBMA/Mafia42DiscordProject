package org.beobma.mafia42discordproject.web

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Member
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GameManager
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.replay.GameReplayRenderDataStore
import org.beobma.mafia42discordproject.game.replay.ReplayArchiveRepository
import org.beobma.mafia42discordproject.game.replay.ReplayRenderData
import org.beobma.mafia42discordproject.game.system.FrogCurseManager
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.JobManager
import org.beobma.mafia42discordproject.job.definition.list.MentalPatient
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

    private val host = System.getenv("WEB_HOST")?.trim()?.takeIf(String::isNotEmpty) ?: DEFAULT_HOST
    private val port = System.getenv("WEB_PORT")?.toIntOrNull()?.takeIf { it in 1..65535 } ?: DEFAULT_PORT
    private val localBaseUrl = "http://$host:$port"
    private val publicBaseUrl = System.getenv("REPLAY_PUBLIC_BASE_URL")
        ?.trim()
        ?.trimEnd('/')
        ?.takeIf(String::isNotEmpty)
        ?: localBaseUrl

    private val json = Json { ignoreUnknownKeys = true }
    private val secureRandom = SecureRandom()
    private val sessions = ConcurrentHashMap<String, WebSession>()
    private val notes = ConcurrentHashMap<NoteKey, PlayerNote>()

    @Volatile
    private var server: HttpServer? = null
    private var executor: ExecutorService? = null

    @Synchronized
    fun start() {
        if (server != null) return

        val created = try {
            HttpServer.create(InetSocketAddress(host, port), 0)
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
    }

    fun issueAccessUrl(userId: Snowflake): NotepadAccessResult {
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

        return NotepadAccessResult.Success("$publicBaseUrl/session?token=$rawToken")
    }

    fun replayUrl(data: ReplayRenderData): String {
        val uuid = data.replayUuid.ifBlank {
            GameReplayRenderDataStore.replayUuid(data.guildId, data.replayStartedAtMillis)
        }
        return "$publicBaseUrl/history/$uuid"
    }

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
            path == "/session" -> exchangeSessionToken(exchange)
            path == "/api/state" -> serveState(exchange)
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
        val session = rawToken?.let { sessions[tokenHash(it)] }
        val game = session?.let(::currentGameFor)
        if (rawToken.isNullOrBlank() || session == null || game == null) {
            sendText(exchange, 401, "유효하지 않거나 만료된 메모장 링크입니다.")
            return
        }

        exchange.responseHeaders.add(
            "Set-Cookie",
            "$SESSION_COOKIE=$rawToken; Path=/; HttpOnly; SameSite=Strict; Max-Age=${SESSION_LIFETIME_MILLIS / 1000}"
        )
        exchange.responseHeaders.add("Location", "/notepad")
        exchange.sendResponseHeaders(303, -1)
        exchange.close()
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
                putNullableJob("job", viewerDisplayJob)
                put("abilities", buildJsonArray {
                    viewerAbilities.forEach { ability ->
                        add(buildJsonObject {
                            put("name", ability.name)
                            put("description", ability.description)
                            put("image", ability.image)
                        })
                    }
                })
            })
            put("players", buildJsonArray {
                players.forEach { player ->
                    val isSelf = player.member.id == viewer.member.id
                    val isPublic = player.state.isJobPubliclyRevealed
                    val visibleJob = when {
                        isSelf -> viewerDisplayJob
                        isPublic -> FrogCurseManager.displayedJob(player)
                        else -> null
                    }
                    val note = notes[NoteKey(gameKey, viewer.member.id.value, player.member.id.value)]
                    add(buildJsonObject {
                        put("id", player.member.id.value.toString())
                        put("name", player.member.effectiveName)
                        put("avatarUrl", player.member.avatarUrl())
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
                JobManager.getAll()
                    .distinctBy(Job::name)
                    .sortedBy(Job::name)
                    .forEach { job ->
                        add(buildJsonObject {
                            put("name", job.name)
                            putNullableString("image", job.jobImage)
                        })
                    }
            })
        }
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
