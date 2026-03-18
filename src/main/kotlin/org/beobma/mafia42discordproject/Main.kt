package org.beobma.mafia42discordproject

import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.ApplicationCommandType
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull

@OptIn(KordPreview::class)
suspend fun main() {
    val token = System.getenv("DISCORD_TOKEN")
        ?: error("DISCORD_TOKEN 환경 변수가 설정되지 않았습니다.")

    val kord = Kord(token)

    syncSlashCommands(kord)

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

private suspend fun syncSlashCommands(kord: Kord) {
    val guildId = System.getenv("DISCORD_GUILD_ID")
        ?.toULongOrNull()
        ?.let(::Snowflake)

    if (guildId != null) {
        upsertGuildChatInputCommand(kord, guildId, "ping", "봇 응답 속도를 확인합니다.")
        upsertGuildChatInputCommand(kord, guildId, "hello", "봇이 인사합니다.")
        println("✅ 길드 슬래시 명령어 동기화 완료 (guildId=$guildId)")
        return
    }

    upsertGlobalChatInputCommand(kord, "ping", "봇 응답 속도를 확인합니다.")
    upsertGlobalChatInputCommand(kord, "hello", "봇이 인사합니다.")
    println("✅ 글로벌 슬래시 명령어 동기화 완료")
    println("ℹ️ 빠른 반영이 필요하면 DISCORD_GUILD_ID를 설정하세요.")
}

private suspend fun upsertGlobalChatInputCommand(kord: Kord, name: String, description: String) {
    kord.getGlobalApplicationCommands()
        .filter { it.type == ApplicationCommandType.ChatInput && it.name == name }
        .firstOrNull()
        ?.delete()

    kord.createGlobalChatInputCommand(name, description)
}

private suspend fun upsertGuildChatInputCommand(kord: Kord, guildId: Snowflake, name: String, description: String) {
    kord.getGuildApplicationCommands(guildId)
        .filter { it.type == ApplicationCommandType.ChatInput && it.name == name }
        .firstOrNull()
        ?.delete()

    kord.createGuildChatInputCommand(guildId, name, description)
}
