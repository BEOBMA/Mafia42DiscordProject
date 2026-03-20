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
import org.beobma.mafia42discordproject.game.player.JobPreferenceManager
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.JobManager
import org.beobma.mafia42discordproject.job.evil.Evil

object JobPreferenceCommand : DiscordCommand {
    override val name: String = "jobpreference"
    override val description: String = "게임 외 시간에 선호 직업 7개를 설정합니다."

    private const val maxAutoCompleteChoices = 25
    private const val assistantOption = "보조계열"
    private const val policeOption = "경찰계열"
    private val specialOptions = (1..5).map { "특수직업$it" }
    private val optionNames = listOf(assistantOption, policeOption) + specialOptions

    override suspend fun registerGlobal(kord: Kord) {
        kord.createGlobalChatInputCommand(name, description) {
            registerJobOptions()
        }
    }

    override suspend fun registerGuild(kord: Kord, guildId: Snowflake) {
        kord.createGuildChatInputCommand(guildId, name, description) {
            registerJobOptions()
        }
    }

    override suspend fun handleAutoComplete(event: GuildAutoCompleteInteractionCreateEvent) {
        val query = event.interaction.focusedOption.value.trim()
        val optionName = event.interaction.focusedOption.name

        val selectedJobNames = event.interaction.command.strings
            .filterKeys { it != optionName }
            .values
            .toSet()

        val suggestions = getAllowedJobsByOption(optionName)
            .asSequence()
            .filterNot { optionName in specialOptions && it.name in selectedJobNames }
            .map(Job::name)
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
        val result = savePreference(
            userId = event.interaction.user.id,
            rawJobNamesByOption = optionNames.associateWith { optionName -> event.interaction.command.strings[optionName] }
        )
        if (!result.success) {
            DiscordMessageManager.respondEphemeral(event, result.message)
            return
        }
        DiscordMessageManager.respondEphemeral(event, result.message)
    }

    override suspend fun handleMessage(event: MessageCreateEvent, args: List<String>) {
        val userId = event.message.author?.id ?: return
        val parsedJobs = parseMessageArgs(args)
        if (!parsedJobs.success) {
            event.message.channel.createMessage(parsedJobs.message)
            return
        }

        val result = savePreference(
            userId = userId,
            rawJobNamesByOption = optionNames.zip(parsedJobs.values).toMap()
        )
        event.message.channel.createMessage(result.message)
    }

    private data class CommandResult(val success: Boolean, val message: String, val values: List<String> = emptyList())

    private fun parseMessageArgs(args: List<String>): CommandResult {
        if (args.size != optionNames.size) {
            return CommandResult(
                success = false,
                message = "사용법: !jobpreference <보조계열> <경찰계열> <특수직업1> <특수직업2> <특수직업3> <특수직업4> <특수직업5>"
            )
        }
        return CommandResult(success = true, message = "", values = args)
    }

    private fun savePreference(userId: Snowflake, rawJobNamesByOption: Map<String, String?>): CommandResult {
        fun log(message: String) {
            println("[jobpreference] $message")
        }

        log("handle start userId=${userId.value}")
        if (GameManager.isInCurrentGame(userId)) {
            log("blocked: user is already in current game userId=${userId.value}")
            return CommandResult(false, "게임 참여 중에는 선호 직업을 변경할 수 없습니다.")
        }

        log("raw inputs=$rawJobNamesByOption")
        val jobsByOption = mutableMapOf<String, Job>()

        for (optionName in optionNames) {
            val jobName = rawJobNamesByOption[optionName]
            log("checking option=$optionName input=$jobName")
            val matchedJob = jobName?.let(JobManager::findByName)

            if (jobName != null && matchedJob == null) {
                log("invalid job detected option=$optionName input=$jobName")
                return CommandResult(
                    false,
                    "존재하지 않는 직업이 있습니다: $jobName\n가능한 직업: ${JobManager.getAll().joinToString { it.name }}"
                )
            }

            if (matchedJob == null) {
                log("failed: option=$optionName was not provided")
                return CommandResult(false, "직업 7개를 모두 입력해 주세요.")
            }

            if (matchedJob !in getAllowedJobsByOption(optionName)) {
                log("failed: option=$optionName has invalid category job=${matchedJob.name}")
                return CommandResult(false, "${getOptionDisplayName(optionName)}에는 선택할 수 없는 직업입니다: ${matchedJob.name}")
            }

            jobsByOption[optionName] = matchedJob
        }

        val jobs = optionNames.mapNotNull { jobsByOption[it] }
        val duplicateNames = jobs.groupingBy(Job::name).eachCount().filterValues { it > 1 }.keys
        if (duplicateNames.isNotEmpty()) {
            log("failed: duplicate jobs found duplicates=${duplicateNames.joinToString()}")
            return CommandResult(false, "직업은 중복 없이 선택해야 합니다. 중복 직업: ${duplicateNames.joinToString()}")
        }

        JobPreferenceManager.save(userId.value, jobs)
        return CommandResult(true, "선호 직업 7개가 저장되었습니다:\n${jobs.joinToString("\n") { "• ${it.name}" }}")
    }

    private fun getAllowedJobsByOption(optionName: String): List<Job> = when (optionName) {
        assistantOption -> JobManager.getAll().filter { it is Evil && it.name != "마피아" && it.name != "악인" }
        policeOption -> JobManager.getAll().filter { it.name == "경찰" || it.name == "요원" }
        in specialOptions -> JobManager.getAll().filter {
            it.name != "경찰" &&
                it.name != "요원" &&
                it.name != "의사" &&
                it.name != "시민" &&
                it !is Evil
        }
        else -> JobManager.getAll()
    }

    private fun getOptionDisplayName(optionName: String): String = when (optionName) {
        assistantOption -> "보조계열"
        policeOption -> "경찰계열"
        in specialOptions -> "특수직업"
        else -> optionName
    }

    private fun dev.kord.rest.builder.interaction.ChatInputCreateBuilder.registerJobOptions() {
        string(assistantOption, "보조계열") {
            required = true
            autocomplete = true
        }

        string(policeOption, "경찰계열") {
            required = true
            autocomplete = true
        }

        repeat(5) { index ->
            val number = index + 1
            string("특수직업$number", "특수직업$number") {
                required = true
                autocomplete = true
            }
        }
    }
}
