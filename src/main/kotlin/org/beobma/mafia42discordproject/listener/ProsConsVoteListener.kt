package org.beobma.mafia42discordproject.listener

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.SelectMenuInteractionCreateEvent
import dev.kord.core.on
import org.beobma.mafia42discordproject.discord.InteractionErrorHandler
import org.beobma.mafia42discordproject.game.GameManager

object ProsConsVoteListener : InteractionListener {
    override fun register(kord: Kord) {
        kord.on<SelectMenuInteractionCreateEvent> {
            InteractionErrorHandler.runSafely("select-pros-cons-vote") {
                val interaction = interaction
                if (interaction.componentId != "pros_cons_vote_select") return@runSafely

                val deferredResponse = runCatching {
                    interaction.deferEphemeralResponse()
                }.getOrElse { error ->
                    println("⚠️ 찬반 투표 인터랙션 응답 지연/실패: ${error.message}")
                    return@runSafely
                }

                val voterId = interaction.user.id
                val selectedVote = interaction.values.firstOrNull()
                val isPros = when (selectedVote) {
                    "pros" -> true
                    "cons" -> false
                    else -> {
                        runCatching {
                            deferredResponse.respond {
                                content = "❌ 올바르지 않은 찬반 투표 선택입니다."
                            }
                        }
                        return@runSafely
                    }
                }

                val isSuccess = GameManager.receiveProsConsVote(voterId, isPros)

                runCatching {
                    deferredResponse.respond {
                        content = if (isSuccess) {
                            if (isPros) "✅ **찬성**에 투표하셨습니다." else "✅ **반대**에 투표하셨습니다."
                        } else {
                            "❌ 현재 찬반 투표 시간이 아니거나, 이미 투표하여 변경할 수 없습니다."
                        }
                    }
                }.onFailure { error ->
                    println("⚠️ 찬반 투표 응답 메시지 전송 실패: ${error.message}")
                }
            }
        }
    }
}
