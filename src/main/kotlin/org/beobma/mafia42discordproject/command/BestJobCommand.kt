package org.beobma.mafia42discordproject.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.suggestString
import dev.kord.core.event.interaction.GuildAutoCompleteInteractionCreateEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.entity.interaction.SubCommand
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.interaction.subCommand
import org.beobma.mafia42discordproject.discord.DiscordMessageManager
import org.beobma.mafia42discordproject.game.GameManager
import org.beobma.mafia42discordproject.game.player.BestJobPreferenceManager
import org.beobma.mafia42discordproject.game.player.JobPreferenceManager
import org.beobma.mafia42discordproject.job.JobManager

object BestJobCommand : DiscordCommand {
    override val name: String = "보석"
    override val description: String = "최선호 직업 1개를 설정하거나 보석 설정을 해제합니다."
    override val aliases: Set<String> = setOf("bestjob")

    private const val SET_SUBCOMMAND = "설정"
    private const val CLEAR_SUBCOMMAND = "설정안함"
    private const val JOB_OPTION = "직업"
    private const val MAX_AUTO_COMPLETE_CHOICES = 25

    override suspend fun registerGlobal(kord: Kord) {
        kord.createGlobalChatInputCommand(name, description) {
            registerOptions()
        }
    }

    override suspend fun registerGuild(kord: Kord, guildId: Snowflake) {
        kord.createGuildChatInputCommand(guildId, name, description) {
            registerOptions()
        }
    }

    override suspend fun handleAutoComplete(event: GuildAutoCompleteInteractionCreateEvent) {
        val query = event.interaction.focusedOption.value.trim()
        val userId = event.interaction.user.id.value
        val allowedJobs = BestJobPreferenceManager.buildAllowedJobNames(userId)

        val suggestions = allowedJobs
            .asSequence()
            .filter { query.isBlank() || it.contains(query, ignoreCase = true) }
            .take(MAX_AUTO_COMPLETE_CHOICES)
            .toList()

        event.interaction.suggestString {
            suggestions.forEach { jobName ->
                choice(jobName, jobName)
            }
        }
    }

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        val userId = event.interaction.user.id
        val command = event.interaction.command as? SubCommand
        val result = when (command?.name) {
            SET_SUBCOMMAND -> saveBestJob(userId, command.strings[JOB_OPTION])
            CLEAR_SUBCOMMAND -> clearBestJob(userId)
            else -> CommandResult(false, usage())
        }
        DiscordMessageManager.respondEphemeral(event, result.message)
    }

    override suspend fun handleMessage(event: MessageCreateEvent, args: List<String>) {
        val userId = event.message.author?.id ?: return
        val input = args.joinToString(" ").trim()
        val result = when {
            input in CLEAR_MESSAGE_ALIASES -> clearBestJob(userId)
            args.firstOrNull() == SET_SUBCOMMAND -> saveBestJob(userId, args.getOrNull(1))
            else -> saveBestJob(userId, args.firstOrNull())
        }
        event.message.channel.createMessage(result.message)
    }

    private data class CommandResult(val success: Boolean, val message: String)

    private fun saveBestJob(userId: Snowflake, selectedJobName: String?): CommandResult {
        if (GameManager.isInCurrentGame(userId)) {
            return CommandResult(false, "게임 참여 중에는 최선호 직업을 변경할 수 없습니다.")
        }

        if (JobPreferenceManager.get(userId.value).isNullOrEmpty()) {
            return CommandResult(false, "선호 직업이 없습니다. `/jobpreference`를 먼저 설정해 주세요.")
        }

        if (selectedJobName.isNullOrBlank()) {
            return CommandResult(false, usage())
        }

        val selectedJob = JobManager.findByName(selectedJobName)
            ?: return CommandResult(false, "존재하지 않는 직업입니다: $selectedJobName")

        if (!BestJobPreferenceManager.isAllowedJob(userId.value, selectedJob.name)) {
            return CommandResult(
                false,
                "해당 직업은 최선호로 설정할 수 없습니다. 선호 직업에 포함된 직업 또는 기본 포함 직업(의사)만 가능합니다."
            )
        }

        BestJobPreferenceManager.save(userId.value, selectedJob)
        return CommandResult(
            true,
            "`${selectedJob.name}`을(를) 최선호 직업으로 저장했습니다. 다음 게임부터 해당 직업 배정 확률이 추가로 증가합니다."
        )
    }

    private fun clearBestJob(userId: Snowflake): CommandResult {
        if (GameManager.isInCurrentGame(userId)) {
            return CommandResult(false, "게임 참여 중에는 최선호 직업을 변경할 수 없습니다.")
        }

        val previousBestJob = BestJobPreferenceManager.get(userId.value)
            ?: return CommandResult(true, "이미 보석 직업을 설정하지 않은 상태입니다.")

        BestJobPreferenceManager.clear(userId.value)
        return CommandResult(
            true,
            "`${previousBestJob.name}` 보석 설정을 해제했습니다. 다음 게임부터 보석에 의한 추가 배정 확률이 적용되지 않습니다."
        )
    }

    private fun usage(): String =
        "사용법: `/보석 설정 직업:<직업명>` 또는 `/보석 설정안함` (메시지: `!보석 <직업명>`, `!보석 설정안함`)"

    private fun dev.kord.rest.builder.interaction.ChatInputCreateBuilder.registerOptions() {
        subCommand(SET_SUBCOMMAND, "최선호 직업을 설정합니다.") {
            string(JOB_OPTION, "최선호로 설정할 직업") {
                required = true
                autocomplete = true
            }
        }
        subCommand(CLEAR_SUBCOMMAND, "보석 직업을 설정하지 않습니다.")
    }

    private val CLEAR_MESSAGE_ALIASES = setOf(CLEAR_SUBCOMMAND, "설정 안함", "해제", "없음")
}
