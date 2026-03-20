package org.beobma.mafia42discordproject.listener

import dev.kord.common.entity.ButtonStyle
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.rest.builder.component.actionRow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.beobma.mafia42discordproject.game.GameManager

object AbilityPickButtonListener : InteractionListener {
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun register(kord: Kord) {
        kord.events.filterIsInstance<ButtonInteractionCreateEvent>()
            .onEach { event ->
                val interaction = event.interaction
                val pickNumber = GameManager.parseAbilityPickButtonId(interaction.componentId) ?: return@onEach
                val deferredResponse = interaction.deferPublicResponse()

                val resultMessage = GameManager.selectExtraAbility(interaction.user.id, pickNumber)
                val snapshot = GameManager.getAbilitySelectionSession(interaction.user.id)

                if (snapshot != null) {
                    GameManager.sendCurrentAbilityOptionImages(interaction.user.id)
                }

                deferredResponse.respond {
                    content = if (snapshot == null) {
                        resultMessage
                    } else {
                        snapshot.guideMessage
                    }

                    if (snapshot != null) {
                        actionRow {
                            repeat(snapshot.optionCount) { index ->
                                interactionButton(ButtonStyle.Primary, GameManager.abilityPickButtonId(index + 1)) {
                                    label = "${index + 1}번 선택"
                                }
                            }
                        }
                    }
                }

                if (snapshot != null) {
                    backgroundScope.launch {
                        GameManager.sendCurrentAbilityOptionImages(interaction.user.id)
                    }
                }
            }.launchIn(kord)
    }
}
