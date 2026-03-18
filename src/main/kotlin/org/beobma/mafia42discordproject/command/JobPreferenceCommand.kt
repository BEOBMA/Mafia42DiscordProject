package org.beobma.mafia42discordproject.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
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

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        val user = event.interaction.user
        if (GameManager.isInCurrentGame(user.id)) {
            DiscordMessageManager.respondEphemeral(event, "게임 참여 중에는 선호 직업을 변경할 수 없습니다.")
            return
        }

        val jobs = (1..7).mapNotNull { index ->
            val optionName = "job$index"
            val jobName = event.interaction.command.strings[optionName]
            val matchedJob = jobName?.let(JobManager::findByName)
            if (jobName != null && matchedJob == null) {
                DiscordMessageManager.respondEphemeral(
                    event,
                    "존재하지 않는 직업이 있습니다: $jobName\\n가능한 직업: ${JobManager.getAll().joinToString { it.name }}"
                )
                return
            }
            matchedJob
        }

        if (jobs.size != 7) {
            DiscordMessageManager.respondEphemeral(event, "직업 7개를 모두 입력해 주세요.")
            return
        }

        val duplicateNames = jobs.groupingBy(Job::name).eachCount().filterValues { it > 1 }.keys
        if (duplicateNames.isNotEmpty()) {
            DiscordMessageManager.respondEphemeral(
                event,
                "직업은 중복 없이 선택해야 합니다. 중복 직업: ${duplicateNames.joinToString()}"
            )
            return
        }

        val policeOrAgentJobs = jobs.filter { it.name == "경찰" || it.name == "요원" }
        if (policeOrAgentJobs.size != 1) {
            DiscordMessageManager.respondEphemeral(event, "경찰/요원 중 정확히 1개를 선택해야 합니다.")
            return
        }

        val evilExcludingMafiaVillain = jobs.filter {
            it is Evil && it.name != "마피아" && it.name != "악인"
        }
        if (evilExcludingMafiaVillain.size != 1) {
            DiscordMessageManager.respondEphemeral(
                event,
                "Evil 계열(마피아, 악인 제외) 직업을 정확히 1개 선택해야 합니다."
            )
            return
        }

        val remainingJobs = jobs - policeOrAgentJobs.toSet() - evilExcludingMafiaVillain.toSet()
        val invalidRemainingJobs = remainingJobs.filter {
            it.name == "경찰" || it.name == "요원" || it.name == "의사" || it is Evil
        }
        if (remainingJobs.size != 5 || invalidRemainingJobs.isNotEmpty()) {
            DiscordMessageManager.respondEphemeral(
                event,
                "나머지 5개는 경찰/요원/의사/Evil 계열을 제외한 직업이어야 합니다."
            )
            return
        }

        JobPreferenceManager.save(user.id.value, jobs)
        DiscordMessageManager.respondEphemeral(
            event,
            "선호 직업 7개가 저장되었습니다:\\n${jobs.joinToString("\\n") { "• ${it.name}" }}"
        )
    }

    private fun dev.kord.rest.builder.interaction.ChatInputCreateBuilder.registerJobOptions() {
        repeat(7) { index ->
            val number = index + 1
            string("job$number", "$number번째 직업") {
                required = true
            }
        }
    }
}
