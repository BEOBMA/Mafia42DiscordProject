package org.beobma.mafia42discordproject.listener

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.SelectMenuInteractionCreateEvent
import dev.kord.core.on
import org.beobma.mafia42discordproject.discord.InteractionErrorHandler
import org.beobma.mafia42discordproject.game.GameManager

object MainVoteListener : InteractionListener {
    override fun register(kord: Kord) {
        kord.on<SelectMenuInteractionCreateEvent> {
            InteractionErrorHandler.runSafely("select-main-vote") {
                val interaction = interaction
                if (interaction.componentId != "main_vote_select") return@runSafely

                val deferredResponse = runCatching {
                    interaction.deferEphemeralResponse()
                }.getOrElse { error ->
                    println("⚠️ 본투표 인터랙션 응답 지연/실패: ${error.message}")
                    return@runSafely
                }

                val voterId = interaction.user.id
                val targetIdString = interaction.values.first()
                val voteResult = GameManager.receiveMainVote(voterId, targetIdString)

                runCatching {
                    deferredResponse.respond {
                        content = when (voteResult) {
                            GameManager.VoteSubmissionResult.SUCCESS ->
                                "✅ 투표가 정상적으로 접수되었습니다. (다른 사람을 선택하여 표를 바꿀 수 있습니다)"
                            GameManager.VoteSubmissionResult.THREATENED ->
                                "협박받아 투표할 수 없습니다"
                            GameManager.VoteSubmissionResult.PROTECTED ->
                                "축복으로 오늘은 해당 플레이어를 투표 대상으로 지목할 수 없습니다."
                            GameManager.VoteSubmissionResult.FAILURE ->
                                "❌ 현재 투표 시간이 아니거나 게임이 진행 중이 아닙니다."
                        }
                    }
                }.onFailure { error ->
                    println("⚠️ 본투표 응답 메시지 전송 실패: ${error.message}")
                }
            }
        }
    }
}
