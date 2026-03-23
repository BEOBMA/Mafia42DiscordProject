package org.beobma.mafia42discordproject

import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.interaction.GuildAutoCompleteInteractionCreateEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import kotlinx.coroutines.flow.toList
import org.beobma.mafia42discordproject.command.CommandRegistry
import org.beobma.mafia42discordproject.command.DebugCommand
import org.beobma.mafia42discordproject.command.DiscordCommand
import org.beobma.mafia42discordproject.game.GameManager
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
        GameManager.relayNightPrivateChat(this)
        if (!content.startsWith("!")) {
            if (GameManager.enforceDeadPlayerChatRestriction(this)) return@on
            return@on
        }

        val tokens = content.removePrefix("!").trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return@on

        val commandName = tokens.first().lowercase()
        val command = CommandRegistry.find(commandName) ?: return@on
        if (GameManager.handleSpiritCommands(this, commandName, tokens.drop(1))) return@on
        if (command != DebugCommand && GameManager.enforceDeadPlayerChatRestriction(this)) return@on
        command.handleMessage(this, tokens.drop(1))
    }

    kord.on<GuildAutoCompleteInteractionCreateEvent> {
        val command = CommandRegistry.find(interaction.command.rootName) ?: return@on
        command.handleAutoComplete(this)
    }

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
        deleteAllGuildCommands(kord, guildId)

        commands.forEach { command ->
            runCatching {
                command.registerGuild(kord, guildId)
            }.onSuccess {
                println("➕ 길드 명령어를 생성했습니다: /${command.name} (guildId=$guildId)")
            }.onFailure { error ->
                println("⚠️ 길드 명령어 생성 실패: /${command.name} (guildId=$guildId), reason=${error.message}")
            }
        }

        println("✅ 길드 슬래시 명령어 전체 재생성 완료 (guildId=$guildId)")
        return
    }

    deleteAllGlobalCommands(kord)

    commands.forEach { command ->
        runCatching {
            command.registerGlobal(kord)
        }.onSuccess {
            println("➕ 글로벌 명령어를 생성했습니다: /${command.name}")
        }.onFailure { error ->
            println("⚠️ 글로벌 명령어 생성 실패: /${command.name}, reason=${error.message}")
        }
    }

    println("✅ 글로벌 슬래시 명령어 전체 재생성 완료")
    println("ℹ️ 빠른 반영이 필요하면 DISCORD_GUILD_ID를 설정하세요.")
}

private suspend fun deleteAllGuildCommands(kord: Kord, guildId: Snowflake) {
    val existingCommands = kord.getGuildApplicationCommands(guildId).toList()

    if (existingCommands.isEmpty()) {
        println("ℹ️ 삭제할 길드 명령어가 없습니다. (guildId=$guildId)")
        return
    }

    existingCommands.forEach { command ->
        runCatching {
            command.delete()
        }.onSuccess {
            println("🗑️ 길드 명령어 삭제: ${command.name} (guildId=$guildId)")
        }.onFailure { error ->
            println("⚠️ 길드 명령어 삭제 실패: ${command.name} (guildId=$guildId), reason=${error.message}")
        }
    }
}

private suspend fun deleteAllGlobalCommands(kord: Kord) {
    val existingCommands = kord.getGlobalApplicationCommands().toList()

    if (existingCommands.isEmpty()) {
        println("ℹ️ 삭제할 글로벌 명령어가 없습니다.")
        return
    }

    existingCommands.forEach { command ->
        runCatching {
            command.delete()
        }.onSuccess {
            println("🗑️ 글로벌 명령어 삭제: ${command.name}")
        }.onFailure { error ->
            println("⚠️ 글로벌 명령어 삭제 실패: ${command.name}, reason=${error.message}")
        }
    }
}