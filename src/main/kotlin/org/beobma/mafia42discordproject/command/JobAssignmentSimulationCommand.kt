package org.beobma.mafia42discordproject.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.interaction.string
import org.beobma.mafia42discordproject.game.GameManager

object JobAssignmentSimulationCommand : DiscordCommand {
    override val name: String = "직업시뮬"
    override val description: String = "가상 플레이어 8명의 직업 배정을 N회 시뮬레이션합니다."
    override val aliases: Set<String> = setOf("jobsim", "직업시뮬레이션")

    private const val countOption = "횟수"
    private const val minSimulationCount = 1
    private const val maxSimulationCount = 30
    private const val maxMessageLength = 1800

    override suspend fun registerGlobal(kord: Kord) {
        kord.createGlobalChatInputCommand(name, description) {
            string(countOption, "시뮬레이션 반복 횟수 (1~30)") {
                required = true
            }
        }
    }

    override suspend fun registerGuild(kord: Kord, guildId: Snowflake) {
        kord.createGuildChatInputCommand(guildId, name, description) {
            string(countOption, "시뮬레이션 반복 횟수 (1~30)") {
                required = true
            }
        }
    }

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        val rawCount = event.interaction.command.strings[countOption]
        val validation = validateCount(rawCount)
        if (validation.errorMessage != null) {
            event.interaction.deferEphemeralResponse().respond {
                content = validation.errorMessage
            }
            return
        }

        val simulationCount = validation.value ?: return
        val result = GameManager.simulateJobAssignmentForVirtualPlayers(simulationCount)
        val report = buildReport(simulationCount, result)
        val chunks = chunkMessages(report)

        val deferred = event.interaction.deferPublicResponse()
        deferred.respond {
            content = chunks.firstOrNull() ?: "출력할 결과가 없습니다."
        }

        val channel = event.interaction.channel
        chunks.drop(1).forEach { chunk ->
            channel.createMessage(chunk)
        }
    }

    override suspend fun handleMessage(event: MessageCreateEvent, args: List<String>) {
        val validation = validateCount(args.firstOrNull())
        if (validation.errorMessage != null) {
            event.message.channel.createMessage(validation.errorMessage)
            return
        }

        val simulationCount = validation.value ?: return
        val result = GameManager.simulateJobAssignmentForVirtualPlayers(simulationCount)
        val report = buildReport(simulationCount, result)

        chunkMessages(report).forEach { chunk ->
            event.message.channel.createMessage(chunk)
        }
    }

    private data class CountValidation(val value: Int? = null, val errorMessage: String? = null)

    private fun validateCount(rawCount: String?): CountValidation {
        val parsed = rawCount?.toIntOrNull()
            ?: return CountValidation(errorMessage = "횟수는 숫자로 입력해 주세요. 예: `/직업시뮬 횟수:10`, `!직업시뮬 10`")

        if (parsed !in minSimulationCount..maxSimulationCount) {
            return CountValidation(errorMessage = "횟수는 $minSimulationCount 이상 $maxSimulationCount 이하로 입력해 주세요.")
        }
        return CountValidation(value = parsed)
    }

    private fun buildReport(count: Int, result: GameManager.JobAssignmentSimulationResult): String {
        val totalAssignments = count * 8
        val summary = result.assignedJobCountByName.entries.joinToString("\n") { (jobName, assignedCount) ->
            val ratio = assignedCount * 100.0 / totalAssignments.toDouble()
            "- $jobName: ${assignedCount}회 (${String.format("%.2f", ratio)}%)"
        }

        return buildString {
            appendLine("# 직업 배정 시뮬레이션 결과")
            appendLine("- 반복 횟수: ${count}회")
            appendLine("- 가상 플레이어 수: 8명")
            appendLine("- 총 배정 수: ${totalAssignments}건")
            appendLine()
            appendLine("## 누적 직업 배정 통계")
            appendLine(summary.ifBlank { "- 통계 없음" })
            appendLine()
            appendLine("## 회차별 과정/결과")
            result.lines.forEach { line -> appendLine(line) }
        }
    }

    private fun chunkMessages(message: String): List<String> {
        if (message.length <= maxMessageLength) return listOf(message)

        val chunks = mutableListOf<String>()
        val lines = message.lines()
        val buffer = StringBuilder()

        lines.forEach { line ->
            val candidate = if (buffer.isEmpty()) line else "${buffer}\n$line"
            if (candidate.length > maxMessageLength && buffer.isNotEmpty()) {
                chunks += buffer.toString()
                buffer.clear()
                buffer.append(line)
            } else if (candidate.length > maxMessageLength) {
                val sliced = line.chunked(maxMessageLength)
                chunks += sliced.dropLast(1)
                buffer.clear()
                buffer.append(sliced.last())
            } else {
                if (buffer.isNotEmpty()) buffer.append('\n')
                buffer.append(line)
            }
        }

        if (buffer.isNotEmpty()) {
            chunks += buffer.toString()
        }

        return chunks
    }
}
