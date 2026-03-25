package org.beobma.mafia42discordproject.lavalink

import dev.kord.core.Kord
import dev.kord.core.entity.Guild
import dev.kord.core.entity.channel.VoiceChannel
import dev.schlaubi.lavakord.LavaKord
import dev.schlaubi.lavakord.audio.*
import dev.schlaubi.lavakord.kord.getLink
import dev.schlaubi.lavakord.kord.lavakord

object LavalinkManager {
    private var lavaKord: LavaKord? = null

    suspend fun initialize(kord: Kord) {
        val host = System.getenv("LAVALINK_HOST") ?: error("LAVALINK_HOST 환경 변수가 설정되지 않았습니다.")
        val port = System.getenv("LAVALINK_PORT")?.toUShortOrNull()
            ?: error("LAVALINK_PORT 환경 변수가 올바르지 않습니다.")
        val password = System.getenv("LAVALINK_PASSWORD") ?: error("LAVALINK_PASSWORD 환경 변수가 설정되지 않았습니다.")
        val secure = System.getenv("LAVALINK_SECURE")?.toBooleanStrictOrNull() ?: false

        val lavalink = kord.lavakord()
        lavalink.addNode(
            hostname = host,
            port = port,
            password = password,
            secure = secure,
            name = "main"
        )

        lavaKord = lavalink
        println("✅ Lavalink 노드 연결 초기화 완료: host=$host, port=$port, secure=$secure")
    }

    suspend fun play(guild: Guild, voiceChannel: VoiceChannel, query: String): PlayResult {
        val lavalink = lavaKord ?: return PlayResult(false, "Lavalink가 초기화되지 않았습니다.")
        val link = guild.getLink(lavalink)
        link.connect(voiceChannel)

        val resolvedQuery = if (query.startsWith("http://") || query.startsWith("https://")) query else "ytsearch:$query"
        val loadedItem = link.loadItem(resolvedQuery)

        val track = when (loadedItem.loadType) {
            LoadType.TRACK_LOADED, LoadType.SEARCH_RESULT -> loadedItem.tracks.firstOrNull()
            LoadType.PLAYLIST_LOADED -> loadedItem.tracks.firstOrNull()
            LoadType.NO_MATCHES -> null
            LoadType.LOAD_FAILED -> return PlayResult(false, "트랙 로드에 실패했습니다: ${loadedItem.exception?.message ?: "원인을 확인하세요."}")
        }

        if (track == null) {
            return PlayResult(false, "재생할 트랙을 찾지 못했습니다.")
        }

        val player = link.player
        if (player.playingTrack != null) {
            player.queue(track)
            return PlayResult(true, "재생 중인 곡이 있어 대기열에 추가했습니다: ${track.info.title}")
        }

        player.playTrack(track)
        return PlayResult(true, "재생 시작: ${track.info.title}")
    }

    suspend fun registerLogging(kord: Kord) {
        val lavalink = lavaKord ?: return
        kord.guilds.collect { guild ->
            val link = guild.getLink(lavalink)
            link.player.on<TrackStartEvent> {
                println("▶️ 트랙 시작 guild=${guild.id.value} title=${track.info.title}")
            }
            link.player.on<TrackEndEvent> {
                println("⏹️ 트랙 종료 guild=${guild.id.value} title=${track.info.title} reason=$endReason")
            }
            link.player.on<TrackExceptionEvent> {
                println("❌ 트랙 로드 실패 guild=${guild.id.value} title=${track.info.title} message=${exception.message}")
            }
            link.on<NodeChangedEvent> {
                println("🔁 Lavalink 노드 변경 guild=${guild.id.value} from=${oldNode?.name} to=${newNode.name}")
            }
        }
    }
}

data class PlayResult(val success: Boolean, val message: String)
