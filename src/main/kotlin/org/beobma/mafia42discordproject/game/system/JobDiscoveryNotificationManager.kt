package org.beobma.mafia42discordproject.game.system

import org.beobma.mafia42discordproject.game.replay.GameReplayLogger
import org.beobma.mafia42discordproject.game.replay.ReplayVisibility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.reporter.ReporterAssets
import org.beobma.mafia42discordproject.job.evil.Evil

object JobDiscoveryNotificationManager {
    private const val HACKER_SUCCESS_IMAGE_URL =
        "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(6).webp"
    private const val HACKER_SYNC_IMAGE_URL =
        "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(11).png"

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
                            "특종입니다! ${event.target.member.effectiveName}님이 ${event.revealedJob.name}(이)라는 소식입니다!\n${event.imageUrl ?: ReporterAssets.PUBLIC_SCOOP_ARTICLE_IMAGE_URL}"
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

                rememberDiscoveredJob(game, event)
                notifyDiscoveredTarget(event, game)
                runCatching {
                    val message = buildDiscovererNotificationMessage(event)
                    if (game != null) {
                        GameReplayLogger.logDirectMessage(game, event.discoverer, message, "직업 발견 결과")
                    }
                    event.discoverer.member.getDmChannel().createMessage(message)
                }
            }
    }

    suspend fun notifyDiscoveredTarget(
        event: GameEvent.JobDiscovered,
        game: org.beobma.mafia42discordproject.game.Game? = null
    ) {
        if (event.isCancelled || event.isPublicReveal || !event.notifyTarget) return

        rememberDiscovererJob(game, event)
        runCatching {
            val message = buildTargetNotificationMessage(event)
            if (game != null) {
                GameReplayLogger.logDirectMessage(game, event.target, message, "직업 발견 알림")
            }
            event.target.member.getDmChannel().createMessage(message)
        }
    }

    private fun rememberDiscoveredJob(
        game: org.beobma.mafia42discordproject.game.Game?,
        event: GameEvent.JobDiscovered
    ) {
        game?.privateDisplayedJobNamesByObserver
            ?.getOrPut(event.discoverer.member.id, ::mutableMapOf)
            ?.set(event.target.member.id, event.revealedJob.name)
    }

    private fun rememberDiscovererJob(
        game: org.beobma.mafia42discordproject.game.Game?,
        event: GameEvent.JobDiscovered
    ) {
        val jobName = PrivateJobKnowledgePolicy.revealedDiscovererJobName(event.sourceAbilityName) ?: return
        game?.privateDisplayedJobNamesByObserver
            ?.getOrPut(event.target.member.id, ::mutableMapOf)
            ?.set(event.discoverer.member.id, jobName)
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
                "수사" -> {
                    append("그 사람의 직업은 ${event.revealedJob.name}.")
                }
                "특종" if event.resolvedAt == DiscoveryStep.NIGHT -> {
                    append("특종입니다! ${event.target.member.effectiveName}님이 ${event.revealedJob.name}(이)라는 소식입니다!")
                    appendLine()
                    append(event.imageUrl ?: ReporterAssets.PRIVATE_SCOOP_RESULT_IMAGE_URL)
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
                        append("${event.target.member.effectiveName}님이 ${event.revealedJob.name}${quotedInfoParticle(event.revealedJob.name)} 정보를 공유받았습니다.")
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

    private fun quotedInfoParticle(value: String): String {
        val lastChar = value.lastOrNull() ?: return "라는"
        if (lastChar !in '가'..'힣') return "라는"
        val hasFinalConsonant = ((lastChar.code - '가'.code) % 28) != 0
        return if (hasFinalConsonant) "이라는" else "라는"
    }

    private fun buildTargetNotificationMessage(event: GameEvent.JobDiscovered): String {
        if (event.sourceAbilityName == "해킹") {
            return "해커 ${event.discoverer.member.effectiveName}님이 자신의 정보를 전송하였습니다.\n$HACKER_SYNC_IMAGE_URL"
        }
        if (event.sourceAbilityName == "도굴") {
            val convertedJobName = if (event.actualJob is Evil) "악인" else "시민"
            return buildString {
                append("도굴꾼 (${event.discoverer.member.effectiveName}) 님에게 도굴당해 직업이 ${convertedJobName}으로 변경되었습니다.")
                event.imageUrl?.takeIf { it.isNotBlank() }?.let { url ->
                    appendLine()
                    append(url)
                }
            }
        }
        if (event.sourceAbilityName == "수사") {
            return buildString {
                append("형사 ${event.discoverer.member.effectiveName}님이 조사를 마쳤습니다.")
                event.imageUrl?.takeIf { it.isNotBlank() }?.let { url ->
                    appendLine()
                    append(url)
                }
            }
        }
        if (event.sourceAbilityName == "수습") {
            return buildString {
                append("수습당했습니다")
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

internal object PrivateJobKnowledgePolicy {
    fun revealedDiscovererJobName(sourceAbilityName: String?): String? = when (sourceAbilityName) {
        "수사" -> "형사"
        "해킹" -> "해커"
        "도굴" -> "도굴꾼"
        else -> null
    }
}
