package org.beobma.mafia42discordproject

import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.ApplicationCommandType
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import org.beobma.mafia42discordproject.command.CommandRegistry
import org.beobma.mafia42discordproject.command.DiscordCommand

@OptIn(KordPreview::class)
suspend fun main() {
    val token = System.getenv("DISCORD_TOKEN")
        ?: error("DISCORD_TOKEN 환경 변수가 설정되지 않았습니다.")

    val kord = Kord(token)
    val commands = CommandRegistry.all()

    syncSlashCommands(kord, commands)

    kord.on<ReadyEvent> {
        println("✅ 로그인 완료: ${kord.getSelf().tag}")
        println("사용 가능한 슬래시 명령어: ${commands.joinToString { "/${it.name}" }}")
    }

    kord.on<GuildChatInputCommandInteractionCreateEvent> {
        val command = CommandRegistry.find(interaction.command.rootName) ?: return@on
        command.handle(this)
    }

    kord.login()
}

private suspend fun syncSlashCommands(kord: Kord, commands: List<DiscordCommand>) {
    val guildId = System.getenv("DISCORD_GUILD_ID")
        ?.toULongOrNull()
        ?.let(::Snowflake)

    if (guildId != null) {
        commands.forEach { command ->
            upsertGuildChatInputCommand(kord, guildId, command.name, command.description)
        }
        println("✅ 길드 슬래시 명령어 동기화 완료 (guildId=$guildId)")
        return
    }

    commands.forEach { command ->
        upsertGlobalChatInputCommand(kord, command.name, command.description)
    }
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
