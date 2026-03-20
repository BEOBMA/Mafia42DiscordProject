package org.beobma.mafia42discordproject

import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.ApplicationCommandType
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.interaction.GuildAutoCompleteInteractionCreateEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import org.beobma.mafia42discordproject.command.CommandRegistry
import org.beobma.mafia42discordproject.command.DiscordCommand
import org.beobma.mafia42discordproject.game.player.JobPreferenceManager
import org.beobma.mafia42discordproject.job.JobManager
import org.beobma.mafia42discordproject.job.ability.AbilityManager
import org.beobma.mafia42discordproject.listener.AbilityPickButtonListener
import org.beobma.mafia42discordproject.listener.MainVoteListener
import org.beobma.mafia42discordproject.listener.ProsConsVoteListener

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

    kord.on<MessageCreateEvent> {
        if (message.author?.isBot == true) return@on

        val content = message.content.trim()
        if (!content.startsWith("!")) return@on

        val tokens = content.removePrefix("!").trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return@on

        val command = CommandRegistry.find(tokens.first()) ?: return@on
        command.handleMessage(this, tokens.drop(1))
    }

    kord.on<GuildAutoCompleteInteractionCreateEvent> {
        val command = CommandRegistry.find(interaction.command.rootName) ?: return@on
        command.handleAutoComplete(this)
    }

    // UI 상호작용 버튼 리스너 일괄등록
    val interactionListeners = listOf(
        MainVoteListener,
        ProsConsVoteListener,
        AbilityPickButtonListener
    )

    interactionListeners.forEach { listener ->
        listener.register(kord)
    }

    JobManager.registerAll()
    AbilityManager.registerAll()
    JobPreferenceManager.load()
    kord.login()
}

private suspend fun syncSlashCommands(kord: Kord, commands: List<DiscordCommand>) {
    val guildId = System.getenv("DISCORD_GUILD_ID")
        ?.toULongOrNull()
        ?.let(::Snowflake)

    if (guildId != null) {
        commands.forEach { command ->
            upsertGuildChatInputCommand(kord, guildId, command)
        }
        println("✅ 길드 슬래시 명령어 동기화 완료 (guildId=$guildId)")
        return
    }

    commands.forEach { command ->
        upsertGlobalChatInputCommand(kord, command)
    }
    println("✅ 글로벌 슬래시 명령어 동기화 완료")
    println("ℹ️ 빠른 반영이 필요하면 DISCORD_GUILD_ID를 설정하세요.")
}

private suspend fun upsertGlobalChatInputCommand(kord: Kord, command: DiscordCommand) {
    val existingCommand = kord.getGlobalApplicationCommands()
        .filter { it.type == ApplicationCommandType.ChatInput && it.name == command.name }
        .firstOrNull()

    if (existingCommand != null) {
        println("ℹ️ 글로벌 명령어가 이미 존재하여 생성을 건너뜁니다: /${command.name}")
        return
    }

    runCatching {
        command.registerGlobal(kord)
    }.onSuccess {
        println("➕ 글로벌 명령어를 생성했습니다: /${command.name}")
    }.onFailure { error ->
        println("⚠️ 글로벌 명령어 생성 실패로 건너뜁니다: /${command.name}, reason=${error.message}")
    }
}

private suspend fun upsertGuildChatInputCommand(kord: Kord, guildId: Snowflake, command: DiscordCommand) {
    val existingCommand = kord.getGuildApplicationCommands(guildId)
        .filter { it.type == ApplicationCommandType.ChatInput && it.name == command.name }
        .firstOrNull()

    if (existingCommand != null) {
        println("ℹ️ 길드 명령어가 이미 존재하여 생성을 건너뜁니다: /${command.name} (guildId=$guildId)")
        return
    }

    runCatching {
        command.registerGuild(kord, guildId)
    }.onSuccess {
        println("➕ 길드 명령어를 생성했습니다: /${command.name} (guildId=$guildId)")
    }.onFailure { error ->
        println("⚠️ 길드 명령어 생성 실패로 건너뜁니다: /${command.name} (guildId=$guildId), reason=${error.message}")
    }
}
