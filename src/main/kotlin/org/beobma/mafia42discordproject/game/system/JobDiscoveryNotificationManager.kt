package org.beobma.mafia42discordproject.game.system

import dev.kord.core.behavior.channel.createMessage

object JobDiscoveryNotificationManager {
    suspend fun notifyDiscoveredTargets(events: List<GameEvent>, game: org.beobma.mafia42discordproject.game.Game? = null) {
        events.filterIsInstance<GameEvent.JobDiscovered>()
            .filter { !it.isCancelled }
            .forEach { event ->
                if (event.isPublicReveal) {
                    runCatching {
                        game?.mainChannel?.createMessage(
                            "📢 [직업 공개] ${event.target.member.effectiveName}님의 직업은 [${event.revealedJob.name}] 입니다!"
                        )
                    }
                    return@forEach
                }

                if (event.notifyTarget) {
                    runCatching {
                        event.target.member.getDmChannel().createMessage(
                            buildTargetNotificationMessage(event)
                        )
                    }
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
            } else if (event.sourceAbilityName == "수습") {
                append("${event.target.member.effectiveName}님의 직업은 ${event.revealedJob.name}입니다.")
            } else {
                append("당신은 ${event.target.member.effectiveName}님의 직업이 [${event.revealedJob.name}](인) 것을 알아냈습니다.")

                event.note?.takeIf { it.isNotBlank() }?.let { note ->
                    appendLine()
                    append("참고: $note")
                }
            }
            event.imageUrl?.takeIf { it.isNotBlank() }?.let { url ->
                appendLine()
                append(url)
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
            event.imageUrl?.takeIf { it.isNotBlank() }?.let { url ->
                appendLine()
                append(url)
            }
        }
    }
}
