package org.beobma.mafia42discordproject.listener

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.SelectMenuInteractionCreateEvent
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.beobma.mafia42discordproject.game.GameManager

object MainVoteListener : InteractionListener {
    override fun register(kord: Kord) {
        kord.events.filterIsInstance<SelectMenuInteractionCreateEvent>()
            .onEach { event ->
                val interaction = event.interaction

                if (interaction.componentId == "main_vote_select") {
                    val voterId = interaction.user.id
                    val targetIdString = interaction.values.first()

                    val isSuccess = GameManager.receiveMainVote(voterId, targetIdString)

                    interaction.deferEphemeralResponse().respond {
                        content = if (isSuccess) {
                            "✅ 투표가 정상적으로 접수되었습니다. (다른 사람을 선택하여 표를 바꿀 수 있습니다)"
                        } else {
                            "❌ 현재 투표 시간이 아니거나 게임이 진행 중이 아닙니다."
                        }
                    }
                }
            }.launchIn(kord)
    }
}