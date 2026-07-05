package org.beobma.mafia42discordproject.game.system

import org.beobma.mafia42discordproject.game.replay.GameReplayLogger
import org.beobma.mafia42discordproject.game.replay.ReplayVisibility

object JobDiscoveryNotificationManager {
    private const val HACKER_SUCCESS_IMAGE_URL =
        "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(6).webp"
    private const val HACKER_SYNC_IMAGE_URL =
        "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(11).png"
    private const val REPORTER_NIGHT_IMAGE_URL =
        "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(6).png"
    private const val REPORTER_DAY_IMAGE_URL =
        "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(14).webp"

    suspend fun notifyDiscoveredTargets(events: List<GameEvent>, game: org.beobma.mafia42discordproject.game.Game? = null) {
        events.filterIsInstance<GameEvent.JobDiscovered>()
            .filter { !it.isCancelled }
            .forEach { event ->
                SwindlerManager.notifyFooledByDiscovery(event)

                if (event.isPublicReveal) {
                    event.target.state.isJobPubliclyRevealed = true
                    game?.publiclyRevealedJobNames?.add(event.revealedJob.name)
                    if (event.sourceAbilityName == "처세" || event.sourceAbilityName == "방탄") {
                        return@forEach
                    }
                    runCatching {
                        val message = if (event.sourceAbilityName == "특종") {
                            "특종입니다! ${event.target.member.effectiveName}님이 ${event.revealedJob.name}(이)라는 소식입니다!\n$REPORTER_DAY_IMAGE_URL"
                        } else {
                            "📢 [직업 공개] ${event.target.member.effectiveName}님의 직업은 [${event.revealedJob.name}] 입니다!"
                        }
                        if (game != null) {
                            GameReplayLogger.logSystem(
                                game = game,
                                title = "직업 공개",
                                body = message,
                                visibility = ReplayVisibility.PUBLIC
                            )
                        }
                        game?.mainChannel?.createMessage(message)
                    }
                    return@forEach
                }

                if (event.notifyTarget) {
                    runCatching {
                        val message = buildTargetNotificationMessage(event)
                        if (game != null) {
                            GameReplayLogger.logDirectMessage(game, event.target, message, "직업 발견 알림")
                        }
                        event.target.member.getDmChannel().createMessage(message)
                    }
                }
                runCatching {
                    val message = buildDiscovererNotificationMessage(event)
                    if (game != null) {
                        GameReplayLogger.logDirectMessage(game, event.discoverer, message, "직업 발견 결과")
                    }
                    event.discoverer.member.getDmChannel().createMessage(message)
                }
            }
    }

    private fun buildDiscovererNotificationMessage(event: GameEvent.JobDiscovered): String {
        return buildString {
            when (event.sourceAbilityName) {
                "도굴" -> {
                    append("${event.revealedJob.name} 직업을 획득하였습니다.")
                    event.note?.takeIf { it.isNotBlank() }?.let { note ->
                        appendLine()
                        append(note)
                    }
                }
                "수습" -> {
                    append("${event.target.member.effectiveName}님의 직업은 ${event.revealedJob.name}입니다.")
                }
                "특종" if event.resolvedAt == DiscoveryStep.NIGHT -> {
                    append("${event.target.member.effectiveName}의 직업은 ${event.revealedJob.name}")
                    appendLine()
                    append(REPORTER_NIGHT_IMAGE_URL)
                }
                "해킹" -> {
                    append("해킹 완료. ${event.target.member.effectiveName}님은 ${event.revealedJob.name}입니다.")
                    appendLine()
                    append(HACKER_SUCCESS_IMAGE_URL)
                }
                "암시" -> {
                    append("${event.target.member.effectiveName}님은 ${event.revealedJob.name}입니다.")
                }
                "이슈" -> {
                    if (event.triggeredByTact) {
                        append("${event.target.member.effectiveName}님이 당신의 정체를 알아냈습니다!")
                        appendLine()
                        append("${event.target.member.effectiveName}님은 ${event.revealedJob.name}입니다.")
                    } else {
                        append("(${event.target.member.effectiveName})님이 (${event.revealedJob.name})이라는 정보를 공유받았습니다.")
                    }
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
        if (event.sourceAbilityName == "도굴") {
            return buildString {
                append("도굴꾼 (${event.discoverer.member.effectiveName}) 님에게 도굴당해 직업이 시민으로 변경되었습니다.")
                event.imageUrl?.takeIf { it.isNotBlank() }?.let { url ->
                    appendLine()
                    append(url)
                }
            }
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
