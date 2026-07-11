package org.beobma.mafia42discordproject.listener

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.SelectMenuInteractionCreateEvent
import dev.kord.core.on
import org.beobma.mafia42discordproject.discord.InteractionErrorHandler
import org.beobma.mafia42discordproject.game.GameManager
import org.beobma.mafia42discordproject.game.annihilation.AnnihilationModeManager

object AnnihilationInteractionListener : InteractionListener {
    override fun register(kord: Kord) {
        kord.on<ButtonInteractionCreateEvent> {
            InteractionErrorHandler.runSafely("annihilation-move-button") {
                val interaction = interaction
                if (!interaction.componentId.startsWith("annihilation_move:")) return@runSafely
                val deferred = runCatching {
                    interaction.deferEphemeralResponse()
                }.getOrElse { error ->
                    println("말살 이동 버튼 응답 지연 실패: ${error.message}")
                    return@runSafely
                }
                val message = GameManager.receiveAnnihilationMoveSelection(
                    userId = interaction.user.id,
                    componentId = interaction.componentId
                )
                deferred.respond { content = message }
            }
        }

        kord.on<SelectMenuInteractionCreateEvent> {
            InteractionErrorHandler.runSafely("annihilation-select") {
                val interaction = interaction
                val componentId = interaction.componentId
                if (
                    componentId != AnnihilationModeManager.voteSelectId() &&
                    !componentId.startsWith("annihilation_agent_check:")
                ) {
                    return@runSafely
                }

                val deferred = runCatching {
                    interaction.deferEphemeralResponse()
                }.getOrElse { error ->
                    println("말살 셀렉트 응답 지연 실패: ${error.message}")
                    return@runSafely
                }

                val value = interaction.values.firstOrNull()
                val message = if (componentId == AnnihilationModeManager.voteSelectId()) {
                    GameManager.receiveAnnihilationVote(
                        voterId = interaction.user.id,
                        rawValue = value
                    )
                } else {
                    GameManager.receiveAnnihilationAgentInvestigation(
                        userId = interaction.user.id,
                        componentId = componentId,
                        rawTargetId = value
                    )
                }

                deferred.respond { content = message }
            }
        }
    }
}
