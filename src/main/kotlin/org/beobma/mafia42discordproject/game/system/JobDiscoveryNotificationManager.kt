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
                runCatching {
                    event.discoverer.member.getDmChannel().createMessage(
                        buildDiscovererNotificationMessage(event)
                    )
                }
            }
    }

    private fun buildDiscovererNotificationMessage(event: GameEvent.JobDiscovered): String {
        return buildString {
            if (event.sourceAbilityName == "도굴") {
                append("${event.revealedJob.name} 직업을 획득하였습니다.")
            } else {
                append("당신은 ${event.target.member.effectiveName}님의 직업이 [${event.revealedJob.name}](인) 것을 알아냈습니다.")

                event.note?.takeIf { it.isNotBlank() }?.let { note ->
                    appendLine()
                    append("참고: $note")
                }
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
