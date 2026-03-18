package org.beobma.mafia42discordproject.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.suggestString
import dev.kord.core.event.interaction.GuildAutoCompleteInteractionCreateEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
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
    private const val assistantOption = "assistant"
    private const val policeOption = "police"
    private val specialOptions = (1..5).map { "special$it" }
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

        val suggestions = getAllowedJobsByOption(optionName)
            .asSequence()
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
        fun log(message: String) {
            println("[jobpreference] $message")
        }

        val user = event.interaction.user
        log("handle start userId=${user.id.value} username=${user.username}")

        if (GameManager.isInCurrentGame(user.id)) {
            log("blocked: user is already in current game userId=${user.id.value}")
            DiscordMessageManager.respondEphemeral(event, "게임 참여 중에는 선호 직업을 변경할 수 없습니다.")
            return
        }

        val rawInputs = optionNames.associateWith { optionName -> event.interaction.command.strings[optionName] }
        log("raw inputs=$rawInputs")

        val jobsByOption = mutableMapOf<String, Job>()

        for (optionName in optionNames) {
            val jobName = event.interaction.command.strings[optionName]
            log("checking option=$optionName input=$jobName")

            val matchedJob = jobName?.let(JobManager::findByName)

            if (jobName != null && matchedJob == null) {
                log(
                    "invalid job detected option=$optionName input=$jobName possibleJobs=${
                        JobManager.getAll().joinToString { it.name }
                    }"
                )
                DiscordMessageManager.respondEphemeral(
                    event,
                    "존재하지 않는 직업이 있습니다: $jobName\n가능한 직업: ${JobManager.getAll().joinToString { it.name }}"
                )
                return
            }

            if (matchedJob == null) {
                log("failed: option=$optionName was not provided")
                DiscordMessageManager.respondEphemeral(event, "직업 7개를 모두 입력해 주세요.")
                return
            }

            if (matchedJob !in getAllowedJobsByOption(optionName)) {
                log("failed: option=$optionName has invalid category job=${matchedJob.name}")
                DiscordMessageManager.respondEphemeral(
                    event,
                    "${getOptionDisplayName(optionName)}에는 선택할 수 없는 직업입니다: ${matchedJob.name}"
                )
                return
            }

            log("matched option=$optionName -> ${matchedJob.name}")
            jobsByOption[optionName] = matchedJob
        }

        val jobs = optionNames.mapNotNull { jobsByOption[it] }
        log("resolved jobs size=${jobs.size} jobs=${jobs.joinToString { it.name }}")

        val duplicateNames = jobs.groupingBy(Job::name).eachCount().filterValues { it > 1 }.keys
        log("duplicate check duplicates=$duplicateNames")

        if (duplicateNames.isNotEmpty()) {
            log("failed: duplicate jobs found duplicates=${duplicateNames.joinToString()}")
            DiscordMessageManager.respondEphemeral(
                event,
                "직업은 중복 없이 선택해야 합니다. 중복 직업: ${duplicateNames.joinToString()}"
            )
            return
        }

        log("saving preference userId=${user.id.value} jobs=${jobs.joinToString { it.name }}")
        JobPreferenceManager.save(user.id.value, jobs)

        log("success: preference saved userId=${user.id.value}")
        DiscordMessageManager.respondEphemeral(
            event,
            "선호 직업 7개가 저장되었습니다:\n${jobs.joinToString("\n") { "• ${it.name}" }}"
        )
    }

    private fun getAllowedJobsByOption(optionName: String): List<Job> = when (optionName) {
        assistantOption -> JobManager.getAll().filter { it is Evil && it.name != "마피아" }
        policeOption -> JobManager.getAll().filter { it.name == "경찰" || it.name == "요원" }
        in specialOptions -> JobManager.getAll().filter {
            it.name != "경찰" && it.name != "요원" && it.name != "의사" && it !is Evil
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
            string("special$number", "특수직업$number") {
                required = true
                autocomplete = true
            }
        }
    }
}
