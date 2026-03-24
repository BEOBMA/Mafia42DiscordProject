package org.beobma.mafia42discordproject.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import org.beobma.mafia42discordproject.discord.InteractionErrorHandler
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GameManager
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.job.JobManager

object DebugCommand : DiscordCommand {
    override val name: String = "debug"
    override val description: String = "디버깅용 보조 명령어"
    override val koreanName: String = "디버그"
    override val aliases: Set<String> = setOf("디버그")

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        InteractionErrorHandler.runSafely("slash-debug") {
            event.interaction.deferEphemeralResponse().respond {
                content = "디버그 명령어는 메시지 명령어 `!debug` 로 사용해 주세요."
            }
        }
    }

    override suspend fun handleMessage(event: MessageCreateEvent, args: List<String>) {
        val game = event.member?.id?.let(GameManager::getCurrentGameFor)
        if (game == null) {
            event.message.channel.createMessage("현재 진행 중인 게임에 참가한 상태에서만 디버그 명령어를 사용할 수 있습니다.")
            return
        }

        if (args.isEmpty()) {
            event.message.channel.createMessage(buildHelpMessage())
            return
        }

        when (args.first().lowercase()) {
            "help", "도움말" -> event.message.channel.createMessage(buildHelpMessage())
            "status", "상태" -> event.message.channel.createMessage(buildStatus(game))
            "phase", "페이즈" -> handlePhase(event, game, args.drop(1))
            "day", "낮" -> handleDay(event, game, args.drop(1))
            "job", "직업" -> handleJob(event, game, args.drop(1))
            "dead", "사망" -> handleDead(event, game, args.drop(1))
            "shamaned", "성불" -> handleShamaned(event, game, args.drop(1))
            "reset", "초기화" -> handleReset(event, game, args.drop(1))
            "playsound", "소리재생" -> handlePlaySound(event, args.drop(1))
            else -> event.message.channel.createMessage("알 수 없는 디버그 하위 명령어입니다. `!debug help` 를 확인해 주세요.")
        }
    }

    private suspend fun handlePlaySound(event: MessageCreateEvent, args: List<String>) {
        val soundName = args.joinToString(" ").trim()
        if (soundName.isBlank()) {
            event.message.channel.createMessage("사용법: `!debug playsound <외부 오디오 URL 또는 파일 경로>`")
            return
        }

        val result = GameManager.playSound(soundName)
        if (result.isSuccess) {
            event.message.channel.createMessage("사운드 재생 완료: `$soundName`")
            return
        }

        val reason = result.exceptionOrNull()?.message ?: "알 수 없는 오류"
        event.message.channel.createMessage("사운드 재생 실패: `$soundName`\n사유: $reason")
    }

    private suspend fun handlePhase(event: MessageCreateEvent, game: Game, args: List<String>) {
        val phaseInput = args.firstOrNull()?.uppercase()
        val phase = phaseInput?.let { runCatching { GamePhase.valueOf(it) }.getOrNull() }

        if (phase == null) {
            event.message.channel.createMessage("사용법: `!debug phase <DAY|NIGHT|VOTE|END>`")
            return
        }

        val previous = game.currentPhase
        game.currentPhase = phase
        event.message.channel.createMessage("페이즈를 `${previous.name}` → `${phase.name}` 으로 변경했습니다.")
    }

    private suspend fun handleDay(event: MessageCreateEvent, game: Game, args: List<String>) {
        val dayCount = args.firstOrNull()?.toIntOrNull()
        if (dayCount == null || dayCount < 0) {
            event.message.channel.createMessage("사용법: `!debug day <0 이상의 숫자>`")
            return
        }

        val previous = game.dayCount
        game.dayCount = dayCount
        event.message.channel.createMessage("dayCount를 `$previous` → `$dayCount` 로 변경했습니다.")
    }

    private suspend fun handleJob(event: MessageCreateEvent, game: Game, args: List<String>) {
        if (args.size < 2) {
            event.message.channel.createMessage("사용법: `!debug job <@유저|me> <직업명>`")
            return
        }

        val target = resolvePlayer(event, game, args[0]) ?: run {
            event.message.channel.createMessage("대상 플레이어를 찾지 못했습니다. `@멘션`, `유저ID`, `me` 중 하나를 사용해 주세요.")
            return
        }

        val requestedJobName = args.drop(1).joinToString(" ")
        val matchedJobName = JobManager.getAll()
            .firstOrNull { it.name.equals(requestedJobName, ignoreCase = true) }
            ?.name

        if (matchedJobName == null) {
            val allJobNames = JobManager.getAll().joinToString(", ") { it.name }
            event.message.channel.createMessage("직업을 찾지 못했습니다: `$requestedJobName`\n사용 가능한 직업: $allJobNames")
            return
        }

        val created = JobManager.createByName(matchedJobName)
        if (created == null) {
            event.message.channel.createMessage("직업 인스턴스 생성에 실패했습니다: `$matchedJobName`")
            return
        }

        val previous = target.job?.name ?: "없음"
        target.job = created
        event.message.channel.createMessage("${target.member.effectiveName}의 직업을 `$previous` → `${created.name}` 으로 변경했습니다.")
    }

    private suspend fun handleDead(event: MessageCreateEvent, game: Game, args: List<String>) {
        if (args.size < 2) {
            event.message.channel.createMessage("사용법: `!debug dead <@유저|me> <on|off>`")
            return
        }

        val target = resolvePlayer(event, game, args[0]) ?: run {
            event.message.channel.createMessage("대상 플레이어를 찾지 못했습니다.")
            return
        }

        val value = parseBooleanToggle(args[1])
        if (value == null) {
            event.message.channel.createMessage("토글 값은 `on/off`, `true/false`, `1/0` 중 하나여야 합니다.")
            return
        }

        val previous = target.state.isDead
        target.state.isDead = value
        event.message.channel.createMessage("${target.member.effectiveName}의 사망 상태를 `$previous` → `$value` 로 변경했습니다.")
    }

    private suspend fun handleShamaned(event: MessageCreateEvent, game: Game, args: List<String>) {
        if (args.size < 2) {
            event.message.channel.createMessage("사용법: `!debug shamaned <@유저|me> <on|off>`")
            return
        }

        val target = resolvePlayer(event, game, args[0]) ?: run {
            event.message.channel.createMessage("대상 플레이어를 찾지 못했습니다.")
            return
        }

        val value = parseBooleanToggle(args[1])
        if (value == null) {
            event.message.channel.createMessage("토글 값은 `on/off`, `true/false`, `1/0` 중 하나여야 합니다.")
            return
        }

        val previous = target.state.isShamaned
        target.state.isShamaned = value
        event.message.channel.createMessage("${target.member.effectiveName}의 성불 상태를 `$previous` → `$value` 로 변경했습니다.")
    }

    private suspend fun handleReset(event: MessageCreateEvent, game: Game, args: List<String>) {
        if (args.isEmpty()) {
            event.message.channel.createMessage("사용법: `!debug reset <@유저|me>`")
            return
        }

        val target = resolvePlayer(event, game, args[0]) ?: run {
            event.message.channel.createMessage("대상 플레이어를 찾지 못했습니다.")
            return
        }

        target.state.isDead = false
        target.state.isShamaned = false
        target.state.hasUsedDailyAbility = false
        target.state.hasUsedOneTimeAbility = false
        target.state.isSilenced = false
        target.state.isThreatened = false
        target.state.isTamed = false
        target.state.resetForNextPhase()

        event.message.channel.createMessage("${target.member.effectiveName}의 상태를 테스트 기본값으로 초기화했습니다.")
    }

    private fun resolvePlayer(event: MessageCreateEvent, game: Game, raw: String): PlayerData? {
        val normalized = raw.trim()
        val callerId = event.member?.id

        val targetId = when {
            normalized.equals("me", ignoreCase = true) -> callerId
            else -> parseSnowflake(normalized)
        } ?: return null

        return game.playerDatas.firstOrNull { it.member.id == targetId }
    }

    private fun parseSnowflake(raw: String): Snowflake? {
        val mentionPattern = Regex("<@!?(\\d+)>")
        val mentionMatch = mentionPattern.matchEntire(raw)
        val idValue = mentionMatch?.groupValues?.getOrNull(1) ?: raw
        return idValue.toULongOrNull()?.let(::Snowflake)
    }

    private fun parseBooleanToggle(raw: String): Boolean? {
        return when (raw.lowercase()) {
            "on", "true", "1" -> true
            "off", "false", "0" -> false
            else -> null
        }
    }

    private fun buildStatus(game: Game): String {
        val playerStatus = game.playerDatas.joinToString("\n") { player ->
            "- ${player.member.effectiveName}: 직업=${player.job?.name ?: "없음"}, dead=${player.state.isDead}, shamaned=${player.state.isShamaned}"
        }

        return buildString {
            appendLine("[디버그 상태]")
            appendLine("phase=${game.currentPhase}, dayCount=${game.dayCount}, players=${game.playerDatas.size}")
            append(playerStatus)
        }
    }

    private fun buildHelpMessage(): String {
        return """
            디버그 명령어 사용법
            - !debug help
            - !debug status
            - !debug phase <DAY|NIGHT|VOTE|END>
            - !debug day <숫자>
            - !debug job <@유저|me> <직업명>
            - !debug dead <@유저|me> <on|off>
            - !debug shamaned <@유저|me> <on|off>
            - !debug reset <@유저|me>
            - !debug playsound <외부 오디오 URL 또는 파일 경로>

            영매(성불) 테스트 추천 순서
            1) !debug dead <대상> on
            2) !debug shamaned <대상> on/off
            3) !debug phase DAY 또는 NIGHT
            4) !debug status 로 상태 확인
        """.trimIndent()
    }
}
