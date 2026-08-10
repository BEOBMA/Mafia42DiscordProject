package org.beobma.mafia42discordproject

import dev.kord.common.annotation.KordPreview
import dev.kord.core.Kord
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.guild.VoiceServerUpdateEvent
import dev.kord.core.event.interaction.GuildAutoCompleteInteractionCreateEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.user.VoiceStateUpdateEvent
import dev.kord.core.on
import org.beobma.mafia42discordproject.command.CommandRegistry
import org.beobma.mafia42discordproject.command.DebugCommand
import org.beobma.mafia42discordproject.game.GameManager
import org.beobma.mafia42discordproject.game.MafiaExecutionProtectionManager
import org.beobma.mafia42discordproject.game.player.BestJobPreferenceManager
import org.beobma.mafia42discordproject.game.player.JobPreferenceManager
import org.beobma.mafia42discordproject.game.player.PreferencePresetManager
import org.beobma.mafia42discordproject.job.JobManager
import org.beobma.mafia42discordproject.job.ability.AbilityManager
import org.beobma.mafia42discordproject.lavalink.LavalinkManager
import org.beobma.mafia42discordproject.listener.AbilityPickButtonListener
import org.beobma.mafia42discordproject.listener.AnnihilationInteractionListener
import org.beobma.mafia42discordproject.listener.MainVoteListener
import org.beobma.mafia42discordproject.listener.ProsConsVoteListener
import org.beobma.mafia42discordproject.web.WebNotepadServer

private val messageTokenRegex = Regex("\\s+")

@OptIn(KordPreview::class)
suspend fun main() {
    val token = System.getenv("DISCORD_TOKEN")
        ?: error("DISCORD_TOKEN 환경 변수가 설정되지 않았습니다.")

    val kord = Kord(token)
    LavalinkManager.initialize(kord)

    val commands = CommandRegistry.all()

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
            GameManager.recordReplayChat(this)
            return@on
        }

        val tokens = content.removePrefix("!").trim().split(messageTokenRegex).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return@on

        val commandName = tokens.first().lowercase()
        val command = CommandRegistry.find(commandName) ?: return@on
        if (GameManager.handleSpiritCommands(this, commandName, tokens.drop(1))) return@on
        if (command != DebugCommand) GameManager.recordReplayChat(this)
        if (command != DebugCommand && GameManager.enforceDeadPlayerChatRestriction(this)) return@on
        command.handleMessage(this, tokens.drop(1))
    }

    kord.on<GuildAutoCompleteInteractionCreateEvent> {
        val command = CommandRegistry.find(interaction.command.rootName) ?: return@on
        command.handleAutoComplete(this)
    }

    kord.on<VoiceStateUpdateEvent> {
        LavalinkManager.handleVoiceStateUpdate(this, kord)
    }

    kord.on<VoiceServerUpdateEvent> {
        LavalinkManager.handleVoiceServerUpdate(this)
    }

    // UI 상호작용 버튼 리스너 일괄등록
    val interactionListeners = listOf(
        MainVoteListener,
        ProsConsVoteListener,
        AnnihilationInteractionListener,
        AbilityPickButtonListener
    )

    interactionListeners.forEach { listener ->
        listener.register(kord)
    }

    JobManager.registerAll()
    AbilityManager.registerAll()
    JobPreferenceManager.load()
    BestJobPreferenceManager.load()
    PreferencePresetManager.load()
    MafiaExecutionProtectionManager.load()
    WebNotepadServer.start()
    try {
        kord.login()
    } finally {
        WebNotepadServer.stop()
    }
}
