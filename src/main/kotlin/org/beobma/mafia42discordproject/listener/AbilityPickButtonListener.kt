package org.beobma.mafia42discordproject.listener

import dev.kord.core.Kord
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.on
import org.beobma.mafia42discordproject.game.GameManager

object AbilityPickButtonListener : InteractionListener {
    override fun register(kord: Kord) {
        kord.on<ButtonInteractionCreateEvent> {
            val interaction = interaction
            val pickNumber = GameManager.parseAbilityPickButtonId(interaction.componentId) ?: return@on
            val deferredResponse = runCatching {
                interaction.deferEphemeralResponse()
            }.getOrElse { error ->
                println("⚠️ 능력 선택 인터랙션 응답 지연/실패: ${error.message}")
                return@on
            }

            runCatching {
                interaction.message.edit {
                    components = mutableListOf()
                }
            }.onFailure { error ->
                println("⚠️ 능력 선택 버튼 비활성화 실패: ${error.message}")
            }

            val resultMessage = runCatching {
                GameManager.selectExtraAbility(interaction.user.id, pickNumber)
            }.getOrElse { error ->
                println("⚠️ 능력 선택 처리 실패: ${error.message}")
                "❌ 능력 선택 처리 중 오류가 발생했습니다. 잠시 후 같은 번호를 다시 눌러주세요."
            }

            val snapshot = GameManager.getAbilitySelectionSession(interaction.user.id)
            var responseMessage = resultMessage

            if (snapshot != null) {
                val promptSent = runCatching {
                    GameManager.sendCurrentAbilitySelectionPrompt(interaction.user.id)
                }.getOrElse { error ->
                    println("⚠️ 다음 라운드 능력 선택 안내 전송 실패: ${error.message}")
                    false
                }

                if (!promptSent) {
                    responseMessage += "\n⚠️ DM 전송이 지연되거나 실패했습니다. DM 수신 설정을 확인한 뒤 버튼을 다시 시도해 주세요."
                }
            }

            runCatching {
                deferredResponse.respond {
                    content = responseMessage
                }
            }.onFailure { error ->
                println("⚠️ 능력 선택 응답 메시지 전송 실패: ${error.message}")
            }
        }
    }
}
