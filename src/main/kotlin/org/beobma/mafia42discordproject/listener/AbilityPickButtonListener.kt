package org.beobma.mafia42discordproject.listener

import dev.kord.core.Kord
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.beobma.mafia42discordproject.game.GameManager

object AbilityPickButtonListener : InteractionListener {
    override fun register(kord: Kord) {
        kord.events.filterIsInstance<ButtonInteractionCreateEvent>()
            .onEach { event ->
                val interaction = event.interaction
                val pickNumber = GameManager.parseAbilityPickButtonId(interaction.componentId) ?: return@onEach
                val deferredResponse = interaction.deferPublicResponse()

                runCatching {
                    interaction.message.edit {
                        components = mutableListOf()
                    }
                }

                val resultMessage = GameManager.selectExtraAbility(interaction.user.id, pickNumber)
                val snapshot = GameManager.getAbilitySelectionSession(interaction.user.id)

                deferredResponse.respond {
                    content = resultMessage
                }

                if (snapshot != null) {
                    GameManager.sendCurrentAbilityOptionImages(interaction.user.id)
                    GameManager.sendCurrentAbilityPickButtons(interaction.user.id)
                } else {
                    GameManager.sendFinalSelectedAbilityImages(interaction.user.id)
                }
            }.launchIn(kord)
    }
}
