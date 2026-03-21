package org.beobma.mafia42discordproject.listener

import dev.kord.core.Kord
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.on
import org.beobma.mafia42discordproject.game.GameManager

object JobPickButtonListener : InteractionListener {
    override fun register(kord: Kord) {
        kord.on<ButtonInteractionCreateEvent> {
            val interaction = interaction
            val payload = GameManager.parseJobPickButtonId(interaction.componentId) ?: return@on
            if (payload.ownerUserId != interaction.user.id) {
                runCatching {
                    interaction.deferEphemeralResponse().respond {
                        content = "이 버튼은 다른 플레이어의 직업 선택 버튼입니다."
                    }
                }
                return@on
            }

            val deferredResponse = runCatching {
                interaction.deferEphemeralResponse()
            }.getOrElse { error ->
                println("⚠️ 직업 선택 인터랙션 응답 지연/실패: ${error.message}")
                return@on
            }

            runCatching {
                interaction.message.edit {
                    components = mutableListOf()
                }
            }.onFailure { error ->
                println("⚠️ 직업 선택 버튼 비활성화 실패: ${error.message}")
            }

            val resultMessage = runCatching {
                GameManager.selectJob(interaction.user.id, payload.pickNumber)
            }.getOrElse { error ->
                println("⚠️ 직업 선택 처리 실패: ${error.message}")
                "❌ 직업 선택 처리 중 오류가 발생했습니다. 잠시 후 같은 번호를 다시 눌러주세요."
            }

            runCatching {
                deferredResponse.respond {
                    content = resultMessage
                }
            }.onFailure { error ->
                println("⚠️ 직업 선택 응답 메시지 전송 실패: ${error.message}")
            }
        }
    }
}
