package org.beobma.mafia42discordproject.lavalink

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.guild.VoiceServerUpdateEvent
import dev.kord.core.event.user.VoiceStateUpdateEvent
import dev.kord.gateway.UpdateVoiceStatus
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.WebSocket
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletionStage
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

object LavalinkManager {
    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient: HttpClient = HttpClient.newHttpClient()

    private var ws: WebSocket? = null
    private var sessionId: String? = null
    private var initialized = false

    private var host: String = ""
    private var port: Int = 0
    private var password: String = ""
    private var secure: Boolean = false
    private var userId: String = ""

    private val voiceServerUpdates = ConcurrentHashMap<String, VoiceServerPayload>()
    private val voiceStates = ConcurrentHashMap<String, VoiceStatePayload>()

    fun isReady(): Boolean = initialized

    fun initialize(kord: Kord) {
        host = System.getenv("LAVALINK_HOST") ?: error("LAVALINK_HOST 환경 변수가 설정되지 않았습니다.")
        port = System.getenv("LAVALINK_PORT")?.toIntOrNull()
            ?: error("LAVALINK_PORT 환경 변수가 올바르지 않습니다.")
        password = System.getenv("LAVALINK_PASSWORD") ?: error("LAVALINK_PASSWORD 환경 변수가 설정되지 않았습니다.")
        secure = System.getenv("LAVALINK_SECURE")?.toBooleanStrictOrNull() ?: false
        userId = kord.selfId.toString()

        connectWebsocket()
        initialized = true
        println("✅ Lavalink(v4) 연결 초기화 완료: host=$host, port=$port, secure=$secure")
    }

    suspend fun handleVoiceStateUpdate(event: VoiceStateUpdateEvent, kord: Kord) {
        if (event.state.userId != kord.selfId) return

        val guildId = event.state.guildId.toString()
        val channelId = event.state.channelId?.toString()

        if (channelId == null) {
            voiceStates.remove(guildId)
            println("ℹ️ 봇이 음성 채널에서 나갔습니다. guildId=$guildId")
            return
        }

        voiceStates[guildId] = VoiceStatePayload(
            sessionId = event.state.sessionId,
            channelId = channelId
        )

        println("✅ VoiceStateUpdate 수신: guildId=$guildId, channelId=$channelId, sessionId=${event.state.sessionId}")
        trySendVoiceUpdate(guildId)
    }

    fun handleVoiceServerUpdate(event: VoiceServerUpdateEvent) {
        val guildId = event.guildId.toString()
        val endpoint = event.endpoint ?: return

        voiceServerUpdates[guildId] = VoiceServerPayload(
            token = event.token,
            endpoint = endpoint,
            guildId = guildId
        )

        println("✅ VoiceServerUpdate 수신: guildId=$guildId, endpoint=$endpoint")
        trySendVoiceUpdate(guildId)
    }

    suspend fun play(kord: Kord, guildId: Snowflake, voiceChannelId: Snowflake, query: String): PlayResult {
        ensureInitialized()

        val guildIdString = guildId.toString()
        val existingState = voiceStates[guildIdString]
        val existingServer = voiceServerUpdates[guildIdString]

        val needsNewConnection =
            existingState == null ||
                    existingServer == null ||
                    existingState.channelId != voiceChannelId.toString()

        if (needsNewConnection) {
            connectBotToVoiceChannel(kord, guildId, voiceChannelId)

            if (!waitForVoiceHandshake(guildIdString)) {
                return PlayResult(false, "음성 채널 연결 정보를 가져오지 못했습니다. 잠시 후 다시 시도해 주세요.")
            }
        }

        val resolvedQuery = if (query.startsWith("http://") || query.startsWith("https://")) {
            query
        } else {
            "ytsearch:$query"
        }

        val encodedQuery = URLEncoder.encode(resolvedQuery, StandardCharsets.UTF_8)
        val loadResponse = sendGet("${baseHttpUrl()}/v4/loadtracks?identifier=$encodedQuery")

        if (loadResponse.statusCode() !in 200..299) {
            return PlayResult(false, "트랙 로드 요청 실패(status=${loadResponse.statusCode()})")
        }

        val loaded = json.parseToJsonElement(loadResponse.body()).jsonObject
        val loadType = loaded.stringOrNull("loadType").orEmpty()

        val selectedTrack = extractTrack(loaded)
            ?: return when (loadType) {
                "empty" -> PlayResult(false, "재생할 트랙을 찾지 못했습니다.")
                "error" -> PlayResult(
                    false,
                    "Lavalink 로드 실패: ${loaded["data"]?.jsonObject?.stringOrNull("message") ?: "원인을 확인하세요."}"
                )
                else -> PlayResult(false, "트랙 정보를 해석하지 못했습니다. loadType=$loadType")
            }

        val currentSessionId = sessionId
            ?: return PlayResult(false, "Lavalink 세션이 아직 준비되지 않았습니다. 잠시 후 다시 시도해 주세요.")

        val playerPayload = buildJsonObject {
            put("encodedTrack", selectedTrack.encoded)
        }

        val playResponse = sendPatch(
            url = "${baseHttpUrl()}/v4/sessions/$currentSessionId/players/${guildId.value}?noReplace=false",
            body = json.encodeToString(JsonObject.serializer(), playerPayload)
        )

        if (playResponse.statusCode() !in 200..299) {
            return PlayResult(false, "재생 요청 실패(status=${playResponse.statusCode()}): ${playResponse.body()}")
        }

        return PlayResult(true, "재생 성공")
    }

    private fun ensureInitialized() {
        check(initialized) { "LavalinkManager가 초기화되지 않았습니다." }

        if (ws == null) {
            connectWebsocket()
        }
    }

    private suspend fun connectBotToVoiceChannel(kord: Kord, guildId: Snowflake, voiceChannelId: Snowflake) {
        kord.gateway.sendAll(
            UpdateVoiceStatus(
                guildId = guildId,
                channelId = voiceChannelId,
                selfMute = false,
                selfDeaf = true
            )
        )
    }

    private suspend fun waitForVoiceHandshake(guildId: String): Boolean {
        return withTimeoutOrNull(5_000) {
            while (voiceStates[guildId] == null || voiceServerUpdates[guildId] == null) {
                delay(50)
            }
            true
        } ?: false
    }

    private fun resetVoiceHandshakeState(guildId: String) {
        voiceStates.remove(guildId)
        voiceServerUpdates.remove(guildId)
    }

    private fun connectWebsocket() {
        val scheme = if (secure) "wss" else "ws"
        val wsUrl = "$scheme://$host:$port/v4/websocket"

        ws = httpClient.newWebSocketBuilder()
            .header("Authorization", password)
            .header("User-Id", userId)
            .header("Client-Name", "mafia42discordproject/1.0")
            .buildAsync(URI.create(wsUrl), LavalinkWebSocketListener())
            .join()
    }

    private fun trySendVoiceUpdate(guildId: String) {
        val state = voiceStates[guildId] ?: return
        val server = voiceServerUpdates[guildId] ?: return
        val currentSessionId = sessionId ?: return

        val normalizedEndpoint = server.endpoint
            .removePrefix("wss://")
            .removePrefix("ws://")

        val payload = buildJsonObject {
            put("voice", buildJsonObject {
                put("sessionId", state.sessionId)
                put("channelId", state.channelId)
                put("token", server.token)
                put("endpoint", normalizedEndpoint)
            })
        }

        val response = runCatching {
            sendPatch(
                url = "${baseHttpUrl()}/v4/sessions/$currentSessionId/players/$guildId",
                body = json.encodeToString(JsonObject.serializer(), payload)
            )
        }.getOrElse { error ->
            println("❌ Lavalink voiceUpdate 전송 실패(guildId=$guildId): ${error.message}")
            return
        }

        if (response.statusCode() !in 200..299) {
            println(
                "❌ Lavalink voiceUpdate 응답 실패(guildId=$guildId, status=${response.statusCode()}): ${response.body()}"
            )
            return
        }

        println("✅ Lavalink voiceUpdate 적용 완료(guildId=$guildId, body=${response.body()})")
    }

    private fun extractTrack(loaded: JsonObject): LavalinkTrack? {
        return when (loaded.stringOrNull("loadType")) {
            "track" -> loaded["data"]?.jsonObject?.toTrack()
            "search" -> loaded["data"]?.jsonArray?.firstOrNull()?.jsonObject?.toTrack()
            "playlist" -> loaded["data"]?.jsonObject?.get("tracks")?.jsonArray?.firstOrNull()?.jsonObject?.toTrack()
            else -> null
        }
    }

    private fun JsonObject.toTrack(): LavalinkTrack? {
        val encoded = stringOrNull("encoded") ?: return null
        val title = this["info"]?.jsonObject?.stringOrNull("title") ?: "알 수 없는 제목"
        return LavalinkTrack(encoded, title)
    }

    private fun sendGet(url: String): HttpResponse<String> {
        val request = HttpRequest.newBuilder(URI.create(url))
            .header("Authorization", password)
            .GET()
            .build()

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun sendPatch(url: String, body: String): HttpResponse<String> {
        val request = HttpRequest.newBuilder(URI.create(url))
            .header("Authorization", password)
            .header("Content-Type", "application/json")
            .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
            .build()

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun baseHttpUrl(): String {
        val scheme = if (secure) "https" else "http"
        return "$scheme://$host:$port"
    }

    private class LavalinkWebSocketListener : WebSocket.Listener {
        override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*> {
            val message = data.toString()

            runCatching {
                val jsonMessage = json.parseToJsonElement(message).jsonObject

                when (jsonMessage.stringOrNull("op")) {
                    "ready" -> {
                        sessionId = jsonMessage.stringOrNull("sessionId")
                        println("✅ Lavalink ready: sessionId=$sessionId")

                        voiceStates.keys.forEach { guildId ->
                            trySendVoiceUpdate(guildId)
                        }
                    }

                    "event" -> {
                        val type = jsonMessage.stringOrNull("type") ?: "unknown"
                        println("🎵 Lavalink event: type=$type payload=$message")
                    }

                    "stats" -> Unit

                    else -> {
                        println("ℹ️ Lavalink ws message: $message")
                    }
                }
            }.onFailure { error ->
                println("⚠️ Lavalink ws parse 실패: ${error.message}")
            }

            webSocket.request(1)
            return java.util.concurrent.CompletableFuture.completedFuture(null)
        }

        override fun onOpen(webSocket: WebSocket) {
            webSocket.request(1)
            println("✅ Lavalink websocket connected")
        }

        override fun onError(webSocket: WebSocket, error: Throwable) {
            println("❌ Lavalink websocket error: ${error.message}")
        }
    }
}

data class PlayResult(
    val success: Boolean,
    val message: String
)

private data class VoiceServerPayload(
    val token: String,
    val endpoint: String,
    val guildId: String
)

private data class VoiceStatePayload(
    val sessionId: String,
    val channelId: String
)

private data class LavalinkTrack(
    val encoded: String,
    val title: String
)

private fun JsonObject.stringOrNull(key: String): String? =
    this[key]?.jsonPrimitive?.content