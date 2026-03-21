package org.beobma.mafia42discordproject.listener

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.beobma.mafia42discordproject.game.GameManager

object ProsConsVoteListener : InteractionListener {
    override fun register(kord: Kord) {
        kord.events.filterIsInstance<ButtonInteractionCreateEvent>()
            .onEach { event ->
                val interaction = event.interaction

                when (interaction.componentId) {
                    "vote_pros", "vote_cons" -> {
                        val deferredResponse = runCatching {
                            interaction.deferEphemeralResponse()
                        }.getOrElse { error ->
                            println("⚠️ 찬반 투표 인터랙션 응답 지연/실패: ${error.message}")
                            return@onEach
                        }

                        val voterId = interaction.user.id
                        val isPros = interaction.componentId == "vote_pros"

                        val isSuccess = GameManager.receiveProsConsVote(voterId, isPros)

                        runCatching {
                            deferredResponse.respond {
                                content = if (isSuccess) {
                                    if (isPros) "✅ **찬성**에 투표하셨습니다." else "✅ **반대**에 투표하셨습니다."
                                } else {
                                    "❌ 현재 찬반 투표 시간이 아닙니다."
                                }
                            }
                        }.onFailure { error ->
                            println("⚠️ 찬반 투표 응답 메시지 전송 실패: ${error.message}")
                        }
                    }
                }
            }.launchIn(kord)
    }
}
