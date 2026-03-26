package org.beobma.mafia42discordproject.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.suggestString
import dev.kord.core.event.interaction.GuildAutoCompleteInteractionCreateEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.interaction.string
import org.beobma.mafia42discordproject.discord.DiscordMessageManager
import org.beobma.mafia42discordproject.game.GameManager
import org.beobma.mafia42discordproject.game.player.BestJobPreferenceManager
import org.beobma.mafia42discordproject.game.player.JobPreferenceManager
import org.beobma.mafia42discordproject.job.JobManager

object BestJobCommand : DiscordCommand {
    override val name: String = "보석"
    override val description: String = "최선호 직업 1개를 설정해 직업 배정 확률을 높입니다."
    override val aliases: Set<String> = setOf("bestjob")

    private const val jobOption = "직업"
    private const val maxAutoCompleteChoices = 25

    override suspend fun registerGlobal(kord: Kord) {
        kord.createGlobalChatInputCommand(name, description) {
            string(jobOption, "최선호로 설정할 직업") {
                required = true
                autocomplete = true
            }
        }
    }

    override suspend fun registerGuild(kord: Kord, guildId: Snowflake) {
        kord.createGuildChatInputCommand(guildId, name, description) {
            string(jobOption, "최선호로 설정할 직업") {
                required = true
                autocomplete = true
            }
        }
    }

    override suspend fun handleAutoComplete(event: GuildAutoCompleteInteractionCreateEvent) {
        val query = event.interaction.focusedOption.value.trim()
        val userId = event.interaction.user.id.value
        val allowedJobs = BestJobPreferenceManager.buildAllowedJobNames(userId)

        val suggestions = allowedJobs
            .asSequence()
            .filter { query.isBlank() || it.contains(query, ignoreCase = true) }
            .take(maxAutoCompleteChoices)
            .toList()

        event.interaction.suggestString {
            suggestions.forEach { jobName ->
                choice(jobName, jobName)
            }
        }
    }

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        val userId = event.interaction.user.id
        val selectedJobName = event.interaction.command.strings[jobOption]

        val result = saveBestJob(userId, selectedJobName)
        DiscordMessageManager.respondEphemeral(event, result.message)
    }

    override suspend fun handleMessage(event: MessageCreateEvent, args: List<String>) {
        val userId = event.message.author?.id ?: return
        val selectedJobName = args.firstOrNull()

        val result = saveBestJob(userId, selectedJobName)
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
            return CommandResult(false, "사용법: `/보석 직업:<직업명>` 또는 `!보석 <직업명>`")
        }

        val selectedJob = JobManager.findByName(selectedJobName)
            ?: return CommandResult(false, "존재하지 않는 직업입니다: $selectedJobName")

        if (!BestJobPreferenceManager.isAllowedJob(userId.value, selectedJob.name)) {
            return CommandResult(
                false,
                "해당 직업은 최선호로 설정할 수 없습니다. 선호 직업에 포함된 직업 또는 기본 포함 직업(의사/마피아/경찰계열)만 가능합니다."
            )
        }

        BestJobPreferenceManager.save(userId.value, selectedJob)
        return CommandResult(
            true,
            "`${selectedJob.name}`을(를) 최선호 직업으로 저장했습니다. 다음 게임부터 해당 직업 배정 확률이 추가로 증가합니다."
        )
    }
}
