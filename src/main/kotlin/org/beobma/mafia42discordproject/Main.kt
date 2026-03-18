package org.beobma.mafia42discordproject

import dev.kord.common.annotation.KordPreview
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.on

@OptIn(KordPreview::class)
suspend fun main() {
    val token = System.getenv("DISCORD_TOKEN")
        ?: error("DISCORD_TOKEN 환경 변수가 설정되지 않았습니다.")

    val kord = Kord(token)

    kord.on<ReadyEvent> {
        println("✅ 로그인 완료: ${kord.getSelf().tag}")
        println("사용 가능한 슬래시 명령어: /ping, /hello")
    }

    kord.on<GuildChatInputCommandInteractionCreateEvent> {
        when (interaction.command.rootName) {
            "ping" -> interaction.respondPublic {
                content = "Pong! 🏓"
            }

            "hello" -> interaction.respondPublic {
                val mention = interaction.user.mention
                content = "$mention 반가워요! Kotlin + Kord 봇이 동작 중입니다."
            }
        }
    }

    kord.login()
}
