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

        val suggestions = JobManager.getAll()
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

        val rawInputs = (1..7).associate { index ->
            val optionName = "job$index"
            optionName to event.interaction.command.strings[optionName]
        }
        log("raw inputs=$rawInputs")

        val jobs = (1..7).mapNotNull { index ->
            val optionName = "job$index"
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

            if (matchedJob != null) {
                log("matched option=$optionName -> ${matchedJob.name}")
            } else {
                log("option=$optionName was not provided")
            }

            matchedJob
        }

        log("resolved jobs size=${jobs.size} jobs=${jobs.joinToString { it.name }}")

        if (jobs.size != 7) {
            log("failed: not all 7 jobs provided size=${jobs.size}")
            DiscordMessageManager.respondEphemeral(event, "직업 7개를 모두 입력해 주세요.")
            return
        }

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

        val policeOrAgentJobs = jobs.filter { it.name == "경찰" || it.name == "요원" }
        log("police/agent check count=${policeOrAgentJobs.size} jobs=${policeOrAgentJobs.joinToString { it.name }}")

        if (policeOrAgentJobs.size != 1) {
            log("failed: police/agent count invalid count=${policeOrAgentJobs.size}")
            DiscordMessageManager.respondEphemeral(event, "경찰/요원 중 정확히 1개를 선택해야 합니다.")
            return
        }

        val evilExcludingMafiaVillain = jobs.filter {
            it is Evil && it.name != "마피아" && it.name != "악인"
        }
        log(
            "evil excluding mafia/villain check count=${evilExcludingMafiaVillain.size} " +
                    "jobs=${evilExcludingMafiaVillain.joinToString { it.name }}"
        )

        if (evilExcludingMafiaVillain.size != 1) {
            log("failed: evil excluding mafia/villain count invalid count=${evilExcludingMafiaVillain.size}")
            DiscordMessageManager.respondEphemeral(
                event,
                "악인 계열(마피아, 악인 제외) 직업을 정확히 1개 선택해야 합니다."
            )
            return
        }

        val remainingJobs = jobs - policeOrAgentJobs.toSet() - evilExcludingMafiaVillain.toSet()
        log("remaining jobs size=${remainingJobs.size} jobs=${remainingJobs.joinToString { it.name }}")

        val invalidRemainingJobs = remainingJobs.filter {
            it.name == "경찰" || it.name == "요원" || it.name == "의사" || it is Evil
        }
        log("invalid remaining jobs=${invalidRemainingJobs.joinToString { it.name }}")

        if (remainingJobs.size != 5 || invalidRemainingJobs.isNotEmpty()) {
            log(
                "failed: remaining jobs invalid " +
                        "remainingSize=${remainingJobs.size} invalidRemaining=${invalidRemainingJobs.joinToString { it.name }}"
            )
            DiscordMessageManager.respondEphemeral(
                event,
                "나머지 5개는 경찰/요원/의사/악인 계열을 제외한 직업이어야 합니다."
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

    private fun dev.kord.rest.builder.interaction.ChatInputCreateBuilder.registerJobOptions() {
        repeat(7) { index ->
            val number = index + 1
            string("job$number", "${number}번째 직업") {
                required = true
                autocomplete = true
            }
        }
    }
}
