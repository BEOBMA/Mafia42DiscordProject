package org.beobma.mafia42discordproject

import dev.kord.common.annotation.KordPreview
import dev.kord.core.Kord
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on

private const val COMMAND_PREFIX = "/"

@OptIn(KordPreview::class)
suspend fun main() {
    val token = System.getenv("DISCORD_TOKEN")
        ?: error("DISCORD_TOKEN 환경 변수가 설정되지 않았습니다.")

    val kord = Kord(token)

    kord.on<ReadyEvent> {
        println("✅ 로그인 완료: ${kord.getSelf().tag}")
        println("사용 가능한 기본 명령어: !ping, !hello")
    }

    kord.on<MessageCreateEvent> {
        if (message.author?.isBot == true) return@on

        when (message.content.trim()) {
            "${COMMAND_PREFIX}ping" -> {
                message.channel.createMessage("Pong! 🏓")
            }

            "${COMMAND_PREFIX}hello" -> {
                val mention = message.author?.mention ?: "안녕하세요"
                message.channel.createMessage("$mention 반가워요! Kotlin + Kord 봇이 동작 중입니다.")
            }
        }
    }

    kord.login()
}
