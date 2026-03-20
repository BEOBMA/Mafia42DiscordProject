package org.beobma.mafia42discordproject.game.system

import dev.kord.core.behavior.channel.createMessage

object JobDiscoveryNotificationManager {
    suspend fun notifyDiscoveredTargets(events: List<GameEvent>) {
        events.filterIsInstance<GameEvent.JobDiscovered>()
            .forEach { event ->
                runCatching {
                    event.target.member.getDmChannel().createMessage(
                        buildTargetNotificationMessage(event)
                    )
                }
            }
    }

    private fun buildTargetNotificationMessage(event: GameEvent.JobDiscovered): String {
        return buildString {
            append(event.discoverer.member.effectiveName)
            append("님이 당신의 직업을 알아냈습니다.")

            event.sourceAbilityName?.takeIf { it.isNotBlank() }?.let { abilityName ->
                appendLine()
                append("발견 수단: ")
                append(abilityName)
            }
        }
    }
}
