package org.beobma.mafia42discordproject.game.system

import dev.kord.core.behavior.channel.createMessage

object JobDiscoveryNotificationManager {
    private const val REPORTER_NIGHT_IMAGE_URL =
        "https://cdn.discordapp.com/attachments/1483977619258212392/1485034890822160434/2Ug6BvR2Ry6evJDRZ92kXS6a5bBOUX3yq21caYcXohO9xHqgnUKxXQqzGBGYh4IMXw_im8eVKDfvS_wS3dhuX-DG0fNtDccN_J5J5dzBxZdSj8DLDZhin5DjZg_fJPmAw0T7HMrmtYkB-ONZJ4Yn0Q.webp?ex=69c06604&is=69bf1484&hm=c02a309461ae9289eabef3e47ca20841dc3835a91cbef9086ff80994201e7b73&"
    private const val REPORTER_DAY_IMAGE_URL =
        "https://cdn.discordapp.com/attachments/1483977619258212392/1484999585356058734/vIvYT7m1pldkoEA8CUKQLj1PGHJv19FeUCOZIKmDPJwvfBQC5drbpQ6DmzhXuMiAnAoK64RxQcy2x6Syi6VEjQ3NaIic91roXKvwaZHAMRcgX8nICnvp11zQqmaxCZFSeTuKuMIKL5zU31iuymqK4Q.png?ex=69c04523&is=69bef3a3&hm=f7d86c2e95f613c46dfa5fcc64139921266e0974231c2a6ae5ea1727edf56a13&"

    suspend fun notifyDiscoveredTargets(events: List<GameEvent>, game: org.beobma.mafia42discordproject.game.Game? = null) {
        events.filterIsInstance<GameEvent.JobDiscovered>()
            .filter { !it.isCancelled }
            .forEach { event ->
                if (event.isPublicReveal) {
                    runCatching {
                        val message = if (event.sourceAbilityName == "특종") {
                            "특종입니다! ${event.target.member.effectiveName}님이 ${event.revealedJob.name}(이)라는 소식입니다!\n$REPORTER_DAY_IMAGE_URL"
                        } else {
                            "📢 [직업 공개] ${event.target.member.effectiveName}님의 직업은 [${event.revealedJob.name}] 입니다!"
                        }
                        game?.mainChannel?.createMessage(message)
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
            when {
                event.sourceAbilityName == "도굴" -> {
                    append("${event.revealedJob.name} 직업을 획득하였습니다.")
                }

                event.sourceAbilityName == "수습" -> {
                    append("${event.target.member.effectiveName}님의 직업은 ${event.revealedJob.name}입니다.")
                }

                event.sourceAbilityName == "특종" && event.resolvedAt == DiscoveryStep.NIGHT -> {
                    append("${event.target.member.effectiveName}의 직업은 ${event.revealedJob.name}")
                    appendLine()
                    append(REPORTER_NIGHT_IMAGE_URL)
                }

                else -> {
                    append("당신은 ${event.target.member.effectiveName}님의 직업이 [${event.revealedJob.name}](인) 것을 알아냈습니다.")

                    event.note?.takeIf { it.isNotBlank() }?.let { note ->
                        appendLine()
                        append("참고: $note")
                    }
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
