package org.beobma.mafia42discordproject.game.system

import dev.kord.core.behavior.channel.createMessage

object JobDiscoveryNotificationManager {
    private const val HACKER_SUCCESS_IMAGE_URL =
        "https://cdn.discordapp.com/attachments/1483977619258212392/1485044168807026829/gyDHS2t9F3_V9LLel5ruA2uMxzBL9YDSfNmrNwBYgqeQgkCYdzSFT2nyq2YjCmRXntxER4nFbnh5IUwh68shsvCMBzob8z_0KBu4n7tqmt-vdgLPxZO5eFpBNl-e3zs8OVEDKIFyA9xbOwOJdIVQgg.webp?ex=69c06ea8&is=69bf1d28&hm=231d2fa275547e67bd72b7a8f741c677d847a069fc872ae2f71b80108135bbb3&"
    private const val HACKER_SYNC_IMAGE_URL =
        "https://cdn.discordapp.com/attachments/1483977619258212392/1485044167678623886/videoframe_786.png?ex=69c06ea8&is=69bf1d28&hm=4c354fa891ce1be975ebb6798886a57eedd219e46875610d082bd580da74e316&"
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
                event.sourceAbilityName == "해킹" -> {
                    append("해킹 완료. ${event.target.member.effectiveName}님은 ${event.revealedJob.name}입니다.")
                    appendLine()
                    append(HACKER_SUCCESS_IMAGE_URL)
                }
                event.sourceAbilityName == "암시" -> {
                    append("${event.target.member.effectiveName}님은 ${event.revealedJob.name}입니다.")
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
        if (event.sourceAbilityName == "해킹") {
            return "해커 ${event.discoverer.member.effectiveName}님이 자신의 정보를 전송하였습니다.\n$HACKER_SYNC_IMAGE_URL"
        }

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
