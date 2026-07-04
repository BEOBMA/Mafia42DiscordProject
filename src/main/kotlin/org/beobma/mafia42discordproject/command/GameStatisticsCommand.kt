package org.beobma.mafia42discordproject.command

import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import org.beobma.mafia42discordproject.game.statistics.GameStatisticsManager

object GameStatisticsCommand : DiscordCommand {
    override val name: String = "gamestats"
    override val description: String = "저장된 게임 아카이브를 합쳐 통계 데이터 파일을 생성합니다."
    override val koreanName: String = "통계생성"
    override val aliases: Set<String> = setOf("statistics", "통계", "통계생성")

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        val response = event.interaction.deferEphemeralResponse()
        val result = GameStatisticsManager.generate()
        response.respond {
            content = buildResultMessage(result)
        }
    }

    override suspend fun handleMessage(event: MessageCreateEvent, args: List<String>) {
        val result = GameStatisticsManager.generate()
        event.message.channel.createMessage(buildResultMessage(result))
    }

    private fun buildResultMessage(result: GameStatisticsManager.GenerationResult): String {
        return buildString {
            appendLine("통계 데이터 파일을 생성했습니다.")
            appendLine("- 출력: `${result.outputPath}`")
            appendLine("- 아카이브 파일: ${result.totalArchiveFileCount}개")
            appendLine("- 반영된 고유 게임: ${result.processedArchiveCount}개")
            appendLine("- 새로 반영된 게임: ${result.newArchiveCount}개")
            if (result.duplicateArchiveFileCount > 0) {
                appendLine("- 중복으로 제외한 파일: ${result.duplicateArchiveFileCount}개")
            }
            if (result.unreadableArchiveCount > 0) {
                appendLine("- 읽지 못한 파일: ${result.unreadableArchiveCount}개")
            }
        }
    }
}
