package org.beobma.mafia42discordproject.game.replay

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.player.PlayerData

object GameReplayLogger {
    private val urlRegex = Regex("""https?://\S+""")

    fun log(
        game: Game,
        type: ReplayLogType,
        visibility: ReplayVisibility,
        title: String,
        body: String,
        actor: PlayerData? = null,
        recipients: List<ReplayRecipient> = emptyList(),
        imageUrls: List<String> = emptyList(),
        relatedEventId: String? = null
    ) {
        runCatching {
            val rawBody = body.trim()
            val urls = (imageUrls + urlRegex.findAll(rawBody).map { it.value.trimEnd(')', ']', ',', '.') })
                .filter(String::isNotBlank)
                .distinct()
            val cleanBody = removeImageUrls(rawBody, urls)

            synchronized(game) {
                val sequence = game.nextReplaySequence
                game.nextReplaySequence += 1
                game.replayLogs += ReplayLogEntry(
                    sequence = sequence,
                    timestampMillis = System.currentTimeMillis(),
                    dayCount = game.dayCount,
                    phase = game.currentPhase,
                    type = type,
                    actorId = actor?.member?.id,
                    actorName = actor?.member?.effectiveName,
                    actorJobName = actor?.job?.name,
                    recipients = recipients,
                    visibility = visibility,
                    title = title.take(120),
                    body = cleanBody,
                    imageUrls = urls,
                    relatedEventId = relatedEventId
                )
            }
        }.onFailure { error ->
            println("[GameReplayLogger] replay log failed: ${error.message}")
        }
    }

    private fun removeImageUrls(body: String, urls: List<String>): String {
        var sanitized = body
        urls.forEach { url ->
            sanitized = sanitized.replace(url, "")
        }
        return urlRegex.replace(sanitized, "")
            .replace(Regex("""[ \t]+\n"""), "\n")
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()
    }

    fun recipient(player: PlayerData, scope: ReplayVisibility): ReplayRecipient =
        ReplayRecipient(player.member.id, player.member.effectiveName, scope)

    fun logGameStart(game: Game, modeName: String) {
        val playerSummary = game.playerDatas.joinToString("\n") { player ->
            val jobName = player.job?.name ?: "미배정"
            "- ${player.member.effectiveName}: $jobName"
        }
        log(
            game = game,
            type = ReplayLogType.GAME_START,
            visibility = ReplayVisibility.SYSTEM_INTERNAL,
            title = "게임 시작",
            body = "모드: $modeName\n참가자 ${game.playerDatas.size}명\n$playerSummary"
        )
    }

    fun logGameEnd(game: Game, endReason: String, winningTeamName: String?) {
        val result = winningTeamName ?: endReason
        log(
            game = game,
            type = ReplayLogType.GAME_END,
            visibility = ReplayVisibility.PUBLIC,
            title = "게임 종료",
            body = result
        )
    }

    fun logPhase(game: Game, title: String, body: String = title) {
        log(
            game = game,
            type = ReplayLogType.PHASE_START,
            visibility = ReplayVisibility.PUBLIC,
            title = title,
            body = body
        )
    }

    fun logChat(
        game: Game,
        actor: PlayerData,
        body: String,
        visibility: ReplayVisibility,
        title: String = chatTitle(visibility),
        recipients: List<ReplayRecipient> = emptyList(),
        recipientDescription: String? = null
    ) {
        val type = when (visibility) {
            ReplayVisibility.MAFIA_CHANNEL -> ReplayLogType.CHAT_MAFIA
            ReplayVisibility.COUPLE_CHANNEL -> ReplayLogType.CHAT_COUPLE
            ReplayVisibility.DEAD_CHANNEL -> ReplayLogType.CHAT_DEAD
            else -> ReplayLogType.CHAT_PUBLIC
        }
        val destination = recipientDescription
            ?: recipients.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.name }
            ?: visibilityDestination(visibility)
        log(
            game = game,
            type = type,
            visibility = visibility,
            title = title,
            body = formatChatBody(actor, destination, body),
            actor = actor,
            recipients = recipients
        )
    }

    private fun chatTitle(visibility: ReplayVisibility): String = when (visibility) {
        ReplayVisibility.MAFIA_CHANNEL -> "마피아 채팅"
        ReplayVisibility.COUPLE_CHANNEL -> "연인 채팅"
        ReplayVisibility.DEAD_CHANNEL -> "사망자 채팅"
        else -> "공개 채팅"
    }

    private fun visibilityDestination(visibility: ReplayVisibility): String = when (visibility) {
        ReplayVisibility.PUBLIC -> "공개 채널"
        ReplayVisibility.MAFIA_CHANNEL -> "마피아 채널"
        ReplayVisibility.COUPLE_CHANNEL -> "연인 채널"
        ReplayVisibility.DEAD_CHANNEL -> "사망자 채널"
        ReplayVisibility.DIRECT_MESSAGE -> "DM"
        ReplayVisibility.EPHEMERAL -> "개인 응답"
        ReplayVisibility.SYSTEM_INTERNAL -> "시스템"
    }

    private fun formatChatBody(actor: PlayerData, destination: String, body: String): String {
        val content = body.trim().ifBlank { "(내용 없음)" }
        return "보낸 사람: ${actor.member.effectiveName}\n받은 사람/곳: $destination\n내용: $content"
    }

    fun logDirectMessage(
        game: Game,
        recipient: PlayerData,
        body: String,
        title: String = "개인 DM",
        actor: PlayerData? = null
    ) {
        log(
            game = game,
            type = ReplayLogType.DIRECT_MESSAGE,
            visibility = ReplayVisibility.DIRECT_MESSAGE,
            title = title,
            body = body,
            actor = actor,
            recipients = listOf(recipient(recipient, ReplayVisibility.DIRECT_MESSAGE))
        )
    }

    fun logEphemeral(
        game: Game,
        recipient: PlayerData,
        body: String,
        title: String = "개인 응답"
    ) {
        log(
            game = game,
            type = ReplayLogType.EPHEMERAL,
            visibility = ReplayVisibility.EPHEMERAL,
            title = title,
            body = body,
            actor = recipient,
            recipients = listOf(recipient(recipient, ReplayVisibility.EPHEMERAL))
        )
    }

    fun logSystem(
        game: Game,
        title: String,
        body: String,
        visibility: ReplayVisibility = ReplayVisibility.PUBLIC,
        actor: PlayerData? = null,
        recipients: List<ReplayRecipient> = emptyList()
    ) {
        log(
            game = game,
            type = ReplayLogType.SYSTEM_RESULT,
            visibility = visibility,
            title = title,
            body = body,
            actor = actor,
            recipients = recipients
        )
    }
}
