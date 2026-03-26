package org.beobma.mafia42discordproject.command

import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import org.beobma.mafia42discordproject.discord.DiscordMessageManager
import org.beobma.mafia42discordproject.game.player.JobPreferenceManager

object JobPreferenceStatusCommand : DiscordCommand {
    override val name: String = "jobpreference-status"
    override val description: String = "현재 저장된 선호 직업을 확인합니다."
    override val koreanName: String = "선호직업확인"
    override val aliases: Set<String> = setOf("선호직업확인", "직업선호상태")

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        val userId = event.interaction.user.id.value
        val jobs = JobPreferenceManager.get(userId)

        if (jobs.isNullOrEmpty()) {
            DiscordMessageManager.respondEphemeral(
                event,
                "저장된 선호 직업이 없습니다. /jobpreference 명령어로 먼저 설정해 주세요."
            )
            return
        }

        DiscordMessageManager.respondEphemeral(
            event,
            "현재 저장된 선호 직업입니다:\n${jobs.joinToString("\n") { "• ${it.name}" }}"
        )
    }

    override suspend fun handleMessage(event: MessageCreateEvent, args: List<String>) {
        val userId = event.message.author?.id?.value ?: return
        val jobs = JobPreferenceManager.get(userId)

        if (jobs.isNullOrEmpty()) {
            event.message.channel.createMessage(
                "저장된 선호 직업이 없습니다. `!jobpreference` 또는 `/jobpreference` 명령어로 먼저 설정해 주세요."
            )
            return
        }

        event.message.channel.createMessage(
            "현재 저장된 선호 직업입니다:\n${jobs.joinToString("\n") { "• ${it.name}" }}"
        )
    }
}
