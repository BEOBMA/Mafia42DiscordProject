package org.beobma.mafia42discordproject.game.annihilation

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.channel.edit
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import dev.kord.rest.builder.channel.addMemberOverwrite
import dev.kord.rest.builder.channel.addRoleOverwrite
import dev.kord.rest.builder.component.actionRow
import dev.kord.rest.builder.component.option
import kotlinx.coroutines.delay
import org.beobma.mafia42discordproject.discord.DiscordMessageManager.playGameSound
import org.beobma.mafia42discordproject.discord.DiscordMessageManager.sendMainChannelMessageWithImage
import org.beobma.mafia42discordproject.discord.DiscordMessageManager.sendMainChannelMessageWithImageAndSound
import org.beobma.mafia42discordproject.discord.DiscordMessageManager.sendMainChannerMessage
import org.beobma.mafia42discordproject.discord.DiscordMessageManager.stopLoopingGameSound
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GameLoopManager
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.loop.DAY_START_SOUND_PATH
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.replay.GameReplayLogger
import org.beobma.mafia42discordproject.game.replay.ReplayLogType
import org.beobma.mafia42discordproject.game.replay.ReplayVisibility
import org.beobma.mafia42discordproject.game.system.SystemImage
import org.beobma.mafia42discordproject.game.system.Team
import org.beobma.mafia42discordproject.job.definition.list.Agent
import org.beobma.mafia42discordproject.job.definition.list.Citizen
import org.beobma.mafia42discordproject.job.evil.Evil
import kotlin.math.ceil
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

object AnnihilationModeManager {
    const val SKIP_VOTE_VALUE: String = "__annihilation_skip__"

    private const val MOVE_BUTTON_PREFIX = "annihilation_move"
    private const val VOTE_SELECT_ID = "annihilation_vote_select"
    private const val AGENT_SELECT_PREFIX = "annihilation_agent_check"
    private const val NIGHT_DURATION_MS = 5_000L
    private const val DAWN_DURATION_MS = 5_000L
    private const val DAY_DURATION_MS = 120_000L
    private const val MOVEMENT_INTERVAL_MS = 20_000L
    private const val MOVEMENT_LIMIT_MS = 15_000L
    private const val VOTE_DURATION_MS = 120_000L
    private const val VOTE_SHORTEN_REMAINING_MS = 10_000L
    private const val AGENT_INVESTIGATION_DURATION_MS = 20_000L
    private const val CAPO_EXECUTION_COOLDOWN_MS = 10_000L
    private const val WIN_PROGRESS = 100
    private val locationVoiceChannelIds: Map<AnnihilationLocation, Snowflake> = mapOf(
        AnnihilationLocation.SQUARE to Snowflake(1525408703459758190L),
        AnnihilationLocation.ARCHIVE to Snowflake(1525408718689271908L),
        AnnihilationLocation.POLICE_STATION to Snowflake(1525408738247442522L),
        AnnihilationLocation.MARKET to Snowflake(1525408821151928510L),
        AnnihilationLocation.HOSPITAL to Snowflake(1525408839455735859L),
        AnnihilationLocation.CONVENIENCE_STORE to Snowflake(1525408858955190332L),
        AnnihilationLocation.RESIDENTIAL_AREA to Snowflake(1525408876004901007L),
        AnnihilationLocation.ALLEY to Snowflake(1525408891343732818L)
    )

    private val adjectives = listOf(
        "푸른", "은밀한", "고요한", "붉은", "차가운", "희미한", "검은", "하얀",
        "날카로운", "느린", "빠른", "작은", "깊은", "낡은", "새벽의", "한밤의"
    )
    private val nouns = listOf(
        "시계", "열쇠", "등불", "장미", "서류", "가면", "나침반", "우산",
        "라디오", "거울", "지도", "표식", "편지", "문장", "동전", "사진"
    )

    fun initialize(game: Game) {
        val state = AnnihilationGameState()
        val usedCodes = mutableSetOf<String>()
        game.playerDatas.forEach { player ->
            val code = generateSecretCode(usedCodes)
            state.identities[player.member.id] = SecretIdentity(code)
            state.locations[player.member.id] = AnnihilationLocation.SQUARE
            state.previousLocations[player.member.id] = AnnihilationLocation.SQUARE
        }
        game.annihilationState = state
    }

    suspend fun prepareGame(game: Game) {
        updateMainChannelPermissions(game)
        updateMafiaChannelPermissions(game)
        game.coupleChannel?.edit {
            addRoleOverwrite(game.guild.id) {
                denied = Permissions(Permission.ViewChannel, Permission.SendMessages, Permission.ReadMessageHistory)
            }
        }
        game.deadChannel?.edit {
            addRoleOverwrite(game.guild.id) {
                denied = Permissions(Permission.SendMessages)
            }
        }
    }

    suspend fun sendOpeningDms(game: Game) {
        val state = state(game) ?: return
        val mafiaSummary = game.playerDatas
            .filter(::isMafiaTeam)
            .joinToString("\n") { player -> "- ${player.member.effectiveName}: ${player.job?.name}" }

        game.playerDatas.forEach { player ->
            val identity = state.identities[player.member.id] ?: return@forEach
            val roleLine = when (player.job) {
                is Capo -> "당신은 카포입니다. 낮에 비밀 신원으로 처형을 지시할 수 있습니다."
                is Soldato -> "당신은 솔다토입니다. 카포를 보조하고 신분증을 훔칠 수 있습니다."
                is Agent -> "당신은 요원입니다. 사망하면 시민팀은 그 즉시 패배합니다."
                else -> "당신은 시민입니다."
            }
            val message = buildString {
                appendLine("말살 모드 신원 발급")
                appendLine("이름: ${player.member.effectiveName}")
                appendLine("비밀 신원: ${identity.code}")
                appendLine("직업: ${player.job?.name ?: "알 수 없음"}")
                appendLine(roleLine)
                if (isMafiaTeam(player)) {
                    appendLine()
                    appendLine("마피아팀")
                    append(mafiaSummary)
                }
            }
            runCatching {
                GameReplayLogger.logDirectMessage(game, player, message, "말살 모드 신원")
                player.member.getDmChannel().createMessage(message)
            }.onFailure { error ->
                println("말살 모드 신원 DM 전송 실패(${player.member.effectiveName}): ${error.message}")
            }
        }
    }

    suspend fun runGameLoop(game: Game) {
        prepareGame(game)
        while (game.isRunning) {
            startNight(game)
            delay(NIGHT_DURATION_MS.milliseconds)

            startDawn(game)
            delay(DAWN_DURATION_MS.milliseconds)
            if (checkProgressWin(game)) break

            startDay(game)
            runDay(game)
            if (!game.isRunning || checkProgressWin(game)) break

            startVote(game)
            runVote(game)
            if (!game.isRunning || checkProgressWin(game)) break
        }
    }

    suspend fun handleCommand(
        game: Game,
        userId: Snowflake,
        action: String?,
        secret: String?,
        location: String?,
        location2: String?,
        location3: String?,
        targetId: Snowflake?,
        target2Id: Snowflake?
    ): String {
        if (game.annihilationState == null) {
            return "말살 모드 게임이 아닙니다."
        }

        val actor = game.getPlayer(userId)
            ?: return "게임 참가자만 사용할 수 있습니다."
        val normalizedAction = action?.trim()?.lowercase().orEmpty()
        if (normalizedAction.isBlank()) {
            return "행동을 입력해 주세요. 예: 상태, 처형, 탐문, 마피아미션, 증명, 사칭, 합동수사, 카포미션, 솔다토미션, 직위양도, 신분증전달"
        }

        if (normalizedAction !in setOf("status", "상태") && actor.state.isDead) {
            return "사망한 플레이어는 말살 모드 행동을 사용할 수 없습니다."
        }

        if (normalizedAction !in setOf("status", "상태") && state(game)?.isMovementPhaseActive == true) {
            return "이동 페이즈 중에는 이동 선택 외 행동을 사용할 수 없습니다."
        }

        return when (normalizedAction) {
            "help", "도움", "도움말" -> buildHelp(game, actor)
            "status", "상태" -> buildStatus(game, actor)
            "execute", "처형", "실행" -> executeBySecret(game, actor, secret)
            "npc", "inquiry", "탐문" -> inquireNpc(game, actor)
            "mafia-mission", "mafia_mission", "마피아미션" -> performMafiaMission(game, actor)
            "prove", "증명" -> useAgentProof(game, actor)
            "impersonate", "사칭" -> useAgentImpersonation(game, actor)
            "joint", "합동수사" -> useJointInvestigation(game, actor, targetId, target2Id)
            "capo-mission", "capo_mission", "카포미션" -> useCapoForcedMission(game, actor, location, location2, location3)
            "soldato-mission", "soldato_mission", "솔다토미션" -> useSoldatoForcedMission(game, actor, location)
            "handover", "직위양도" -> handOverCapo(game, actor, targetId)
            "give-id", "give_id", "신분증전달" -> transferStolenIdentity(game, actor, targetId, secret)
            else -> "알 수 없는 말살 모드 행동입니다."
        }
    }

    fun moveButtonId(movementPhase: Int, location: AnnihilationLocation): String =
        "$MOVE_BUTTON_PREFIX:$movementPhase:${location.name}"

    fun voteSelectId(): String = VOTE_SELECT_ID

    fun agentSelectId(day: Int, suspectId: Snowflake): String =
        "$AGENT_SELECT_PREFIX:$day:${suspectId.value}"

    suspend fun receiveMoveSelection(game: Game, userId: Snowflake, componentId: String): String {
        val state = state(game) ?: return "말살 모드 게임이 아닙니다."
        if (!componentId.startsWith("$MOVE_BUTTON_PREFIX:")) return "이동 버튼이 아닙니다."
        val parts = componentId.split(":")
        val phase = parts.getOrNull(1)?.toIntOrNull() ?: return "만료된 이동 버튼입니다."
        val location = parts.getOrNull(2)?.let(AnnihilationLocation::parse) ?: return "알 수 없는 장소입니다."
        if (!state.isMovementPhaseActive || phase != state.movementPhaseNumber) {
            return "현재 이동 페이즈의 버튼이 아닙니다."
        }

        val player = game.getPlayer(userId) ?: return "게임 참가자만 이동할 수 있습니다."
        if (player.state.isDead) return "사망한 플레이어는 이동할 수 없습니다."
        if (state.movementSelections.containsKey(userId)) return "이미 이동 장소를 선택했습니다."
        val current = state.locations[userId] ?: AnnihilationLocation.SQUARE
        if (!current.isConnectedTo(location)) {
            return "${current.displayName}에서 ${location.displayName}(으)로는 한 번에 이동할 수 없습니다."
        }

        state.movementSelections[userId] = location
        notifyMafiaMovementIntel(game)
        return buildString {
            appendLine("${location.displayName}(으)로 이동을 선택했습니다.")
            append(destinationHintFor(game, player, location))
        }.trim()
    }

    suspend fun receiveVote(game: Game, voterId: Snowflake, rawValue: String?): String {
        val state = state(game) ?: return "말살 모드 게임이 아닙니다."
        if (game.currentPhase != GamePhase.VOTE) return "현재 투표 시간이 아닙니다."
        val voter = game.getPlayer(voterId) ?: return "게임 참가자만 투표할 수 있습니다."
        if (voter.state.isDead) return "사망한 플레이어는 투표할 수 없습니다."
        if (state.votes.containsKey(voterId)) return "이미 투표했습니다. 말살 모드 투표는 되돌리거나 수정할 수 없습니다."
        val value = rawValue ?: return "투표 대상을 선택해 주세요."
        if (value != SKIP_VOTE_VALUE) {
            val target = runCatching { Snowflake(value) }.getOrNull()?.let(game::getPlayer)
                ?: return "올바르지 않은 투표 대상입니다."
            if (target.state.isDead) return "사망한 플레이어에게는 투표할 수 없습니다."
        }

        state.votes[voterId] = value
        updateVoteSuspectIfNeeded(game)
        return if (value == SKIP_VOTE_VALUE) {
            "투표 건너뛰기를 선택했습니다."
        } else {
            val target = game.getPlayer(Snowflake(value))
            "${target?.member?.effectiveName ?: "알 수 없음"}님에게 투표했습니다."
        }
    }

    fun receiveAgentInvestigation(game: Game, userId: Snowflake, componentId: String, rawTargetId: String?): String {
        val state = state(game) ?: return "말살 모드 게임이 아닙니다."
        if (!componentId.startsWith("$AGENT_SELECT_PREFIX:")) return "요원 조사 선택 메뉴가 아닙니다."
        if (game.currentPhase != GamePhase.VOTE) return "현재 요원 조사 시간이 아닙니다."
        val parts = componentId.split(":")
        val expectedDay = parts.getOrNull(1)?.toIntOrNull() ?: return "만료된 요원 조사 메뉴입니다."
        val expectedSuspectId = parts.getOrNull(2)?.let { runCatching { Snowflake(it) }.getOrNull() }
            ?: return "만료된 요원 조사 메뉴입니다."
        if (expectedDay != game.dayCount || expectedSuspectId != state.voteSuspectId) {
            return "현재 요원 조사 메뉴가 아닙니다."
        }
        val agent = findAliveAgent(game) ?: return "생존한 요원이 없습니다."
        if (agent.member.id != userId) return "요원만 조사 대상을 선택할 수 있습니다."
        val targetId = rawTargetId?.let { runCatching { Snowflake(it) }.getOrNull() }
            ?: return "조사 대상을 선택해 주세요."
        val target = game.getPlayer(targetId) ?: return "올바르지 않은 조사 대상입니다."
        if (target.state.isDead) return "사망한 플레이어는 조사 대상으로 선택할 수 없습니다."
        if (state.agentInvestigationChoiceId != null) return "이미 조사 대상을 선택했습니다."
        state.agentInvestigationChoiceId = target.member.id
        return "${target.member.effectiveName}님을 조사 대상으로 선택했습니다."
    }

    private suspend fun startNight(game: Game) {
        game.currentPhase = GamePhase.NIGHT
        game.dayCount += 1
        GameReplayLogger.logPhase(game, "${game.dayCount}일차 밤")
        game.stopLoopingGameSound()
        muteAll(game, mute = true)
        game.sendMainChannerMessage("${game.dayCount}일차 밤입니다.")
    }

    private suspend fun startDawn(game: Game) {
        game.currentPhase = GamePhase.DAWN
        GameReplayLogger.logPhase(game, "${game.dayCount}일차 새벽")
        game.stopLoopingGameSound()
        game.sendMainChannerMessage("")
    }

    private suspend fun startDay(game: Game) {
        game.currentPhase = GamePhase.DAY
        GameReplayLogger.logPhase(game, "${game.dayCount}일차 낮")
        prepareDayState(game)
        updateMainChannelPermissions(game)
        updateMafiaChannelPermissions(game)
        muteAlivePlayersForDay(game)
        game.sendMainChannelMessageWithImageAndSound(
            imageLink = SystemImage.DAY_START.imageUrl,
            message = buildString {
                appendLine("${game.dayCount}일차 낮입니다. 낮 시간은 120초이며 연장할 수 없습니다.")
                appendLine("수사 진척도: ${state(game)?.citizenProgress ?: 0}/100")
                appendLine("말살 진척도: ${state(game)?.mafiaProgress ?: 0}/100")
                append("개인 이동 힌트와 사용 가능한 행동은 `/말살 상태`로 확인할 수 있습니다.")
            },
            soundPath = DAY_START_SOUND_PATH,
            loopSound = true
        )
        sendGuideMessage(game, "오늘의 장소별 미션 안내", buildPublicLocationBoard(game))
    }

    private suspend fun runDay(game: Game) {
        var remaining = DAY_DURATION_MS
        while (game.isRunning && game.currentPhase == GamePhase.DAY && remaining > 0L) {
            val chunk = minOf(MOVEMENT_INTERVAL_MS, remaining)
            delay(chunk.milliseconds)
            remaining -= chunk
            if (!game.isRunning || game.currentPhase != GamePhase.DAY || remaining <= 0L) break
            runMovementPhase(game)
            if (checkProgressWin(game)) break
        }
        game.stopLoopingGameSound()
    }

    private suspend fun startVote(game: Game) {
        val state = state(game) ?: return
        game.currentPhase = GamePhase.VOTE
        GameReplayLogger.logPhase(game, "${game.dayCount}일차 투표")
        state.votes.clear()
        state.voteSuspectId = null
        state.agentInvestigationChoiceId = null
        state.voteEndAtMillis = System.currentTimeMillis() + VOTE_DURATION_MS
        game.stopLoopingGameSound()
        notifyStolenIdentityOwners(game)

        val alivePlayers = alivePlayers(game)
        game.mainChannel?.createMessage {
            content = buildString {
                appendLine("투표 시간입니다. 투표는 한 번만 가능하며 건너뛰기를 선택할 수 있습니다.")
                appendLine("수사 진척도: ${state.citizenProgress}/100")
                append("말살 진척도: ${state.mafiaProgress}/100")
            }
            actionRow {
                stringSelect(VOTE_SELECT_ID) {
                    placeholder = "투표 대상 또는 건너뛰기 선택"
                    alivePlayers.forEach { player ->
                        option(player.member.effectiveName, player.member.id.toString()) {
                            description = "이 플레이어를 용의자로 지목합니다."
                        }
                    }
                    option("투표 건너뛰기", SKIP_VOTE_VALUE) {
                        description = "이번 투표를 건너뜁니다."
                    }
                }
            }
        }
    }

    private suspend fun runVote(game: Game) {
        val state = state(game) ?: return
        val alivePlayers = alivePlayers(game)
        val statusMessage = game.mainChannel?.createMessage(buildVoteStatus(game, alivePlayers))
        while (game.isRunning && game.currentPhase == GamePhase.VOTE) {
            val remaining = state.voteEndAtMillis - System.currentTimeMillis()
            if (remaining <= 0L) break
            delay(minOf(1_000L, remaining).milliseconds)
            statusMessage?.editContentSafely(buildVoteStatus(game, alivePlayers))
        }
        statusMessage?.editContentSafely(buildVoteStatus(game, alivePlayers))

        val suspect = state.voteSuspectId?.let(game::getPlayer)
        if (suspect == null || suspect.state.isDead) {
            game.sendMainChannelMessageWithImage(
                imageLink = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(60).webp",
                message = "용의자로 올라간 플레이어가 없어 투표가 종료되었습니다."
            )
            return
        }

        game.sendMainChannerMessage("${suspect.member.effectiveName}님이 최다 득표자로 지목되었습니다. 요원 조사 시간으로 넘어갑니다.")
        runAgentInvestigationPhase(game, suspect)
    }

    private suspend fun runMovementPhase(game: Game) {
        val state = state(game) ?: return
        state.movementPhaseNumber += 1
        state.isMovementPhaseActive = true
        state.movementSelections.clear()
        val phase = state.movementPhaseNumber
        val alivePlayers = alivePlayers(game)

        game.sendMainChannerMessage(
            buildString {
                appendLine("이동 페이즈 #$phase")
                appendLine("15초 안에 이동할 장소를 선택하세요. 이 시간 동안 낮 타이머는 흐르지 않습니다.")
                appendLine("현재 위치: ${alivePlayers.joinToString(", ") { player ->
                    "${player.member.effectiveName}=${currentLocation(state, player).displayName}"
                }}")
                appendLine()
                append(buildMovementRouteBoard(game, alivePlayers))
            }
        )
        game.mainChannel?.createMessage {
            content = buildString {
                appendLine("갈 장소를 선택하세요. 현재 위치와 연결된 장소만 처리됩니다.")
                append(buildCompactLocationBoard(game))
            }
            AnnihilationLocation.entries.chunked(4).forEach { rowLocations ->
                actionRow {
                    rowLocations.forEach { location ->
                        interactionButton(ButtonStyle.Primary, moveButtonId(phase, location)) {
                            label = location.displayName
                        }
                    }
                }
            }
        }
        notifyMafiaMovementIntel(game)

        val deadline = System.currentTimeMillis() + MOVEMENT_LIMIT_MS
        while (System.currentTimeMillis() < deadline && state.movementSelections.size < alivePlayers.size) {
            delay(500L.milliseconds)
        }

        alivePlayers.forEach { player ->
            state.movementSelections.putIfAbsent(player.member.id, currentLocation(state, player))
        }

        processMovementSelections(game, phase)
        state.isMovementPhaseActive = false
        state.movementSelections.clear()
    }

    private suspend fun processMovementSelections(game: Game, phase: Int) {
        val state = state(game) ?: return
        val alivePlayers = alivePlayers(game)
        state.previousLocations.clear()
        state.previousLocations.putAll(state.locations)

        alivePlayers.forEach { player ->
            val current = currentLocation(state, player)
            val selected = state.movementSelections[player.member.id] ?: current
            val next = if (current.isConnectedTo(selected)) selected else current
            state.locations[player.member.id] = next
            state.visitorsByLocationToday.getOrPut(next) { mutableSetOf() } += player.member.id
        }

        game.sendMainChannerMessage(
            buildString {
                appendLine("이동 페이즈 #$phase 결과")
                alivePlayers.groupBy { currentLocation(state, it) }
                    .toSortedMap(compareBy { it.ordinal })
                    .forEach { (location, players) ->
                        appendLine("- ${location.displayName}: ${players.joinToString(", ") { it.member.effectiveName }}")
                    }
            }.trim()
        )

        applyLocationVoiceChannelMoves(game)
        stealIdentitiesIfEligible(game)
        resolvePendingMovementEvents(game, phase)
        evaluateCitizenMissions(game)
        sendGuideMessage(game, "현재 위치별 행동 힌트", buildCurrentLocationActionBoard(game))
    }

    private suspend fun runAgentInvestigationPhase(game: Game, suspect: PlayerData) {
        val state = state(game) ?: return
        val agent = findAliveAgent(game)
        if (agent == null) {
            game.sendMainChannerMessage("생존한 요원이 없어 조사 없이 넘어갑니다.")
            return
        }

        game.mainChannel?.createMessage {
            content = "요원은 20초 안에 용의자를 조사할 사람을 선택하세요. 선택하지 않으면 요원이 용의자를 조사하는 것으로 처리됩니다."
            actionRow {
                stringSelect(agentSelectId(game.dayCount, suspect.member.id)) {
                    placeholder = "요원 조사 대상 선택"
                    alivePlayers(game).forEach { player ->
                        option(player.member.effectiveName, player.member.id.toString()) {
                            description = "이 플레이어를 조사 대상으로 선택합니다."
                        }
                    }
                }
            }
        }

        delay(AGENT_INVESTIGATION_DURATION_MS.milliseconds)
        val investigated = state.agentInvestigationChoiceId?.let(game::getPlayer) ?: agent
        resolveVoteSuspect(game, suspect, investigated)
    }

    private suspend fun resolveVoteSuspect(game: Game, suspect: PlayerData, investigated: PlayerData) {
        val investigatedMafia = isMafiaTeam(investigated)
        val suspectMafia = isMafiaTeam(suspect)
        game.sendMainChannerMessage("요원이 ${investigated.member.effectiveName}님을 조사했습니다.")

        if (investigatedMafia) {
            game.sendMainChannerMessage("조사 결과가 흐려져 아무 일도 일어나지 않았습니다.")
            return
        }

        if (!suspectMafia) {
            revealIdentity(game, suspect, "투표 결과 시민팀이 지목되어 비밀 신원이 공개되었습니다.")
            return
        }

        killPlayer(game, suspect, isVoteDeath = true)
        game.sendMainChannelMessageWithImage(
            imageLink = SystemImage.VOTE_EXECUTION.imageUrl,
            message = "${suspect.member.effectiveName}님이 투표로 처형당하였습니다."
        )

        if (suspect.job is Capo) {
            game.sendMainChannerMessage("카포가 투표로 사망하여 시민팀이 승리합니다.")
            GameLoopManager.endGame(game, Team.CITIZEN)
            return
        }

        if (suspect.job is Soldato) {
            returnStolenIdentitiesHeldBy(game, suspect)
        }
    }

    private suspend fun executeBySecret(game: Game, actor: PlayerData, rawSecret: String?): String {
        val state = state(game) ?: return "말살 모드 게임이 아닙니다."
        if (actor.job !is Capo) return "카포만 처형 대상을 고를 수 있습니다."
        if (game.currentPhase != GamePhase.DAY) return "말살 모드 처형은 낮에만 사용할 수 있습니다."
        if (state.isMovementPhaseActive) return "이동 페이즈 중에는 처형할 수 없습니다."
        val blockedPhase = state.executionBlockedMovementPhase
        if (blockedPhase != null && state.movementPhaseNumber < blockedPhase) {
            return "요원의 사칭으로 다음 이동 페이즈 전까지 처형할 수 없습니다."
        }
        val now = System.currentTimeMillis()
        val remainingCooldown = CAPO_EXECUTION_COOLDOWN_MS - (now - state.lastCapoExecutionAtMillis)
        if (remainingCooldown > 0L) {
            return "처형 대기시간이 남아 있습니다. ${ceil(remainingCooldown / 1000.0).toInt()}초 후 다시 시도해 주세요."
        }

        val secret = rawSecret?.trim().orEmpty()
        if (secret.isBlank()) return "처형할 대상의 비밀 신원을 입력해 주세요."
        val target = state.identities.entries
            .firstOrNull { (_, identity) -> identity.code == secret }
            ?.key
            ?.let(game::getPlayer)
            ?: return "해당 비밀 신원을 가진 생존 플레이어를 찾을 수 없습니다."
        if (target.state.isDead) return "이미 사망한 플레이어입니다."
        if (isMafiaTeam(target)) return "마피아팀은 처형 대상으로 지정할 수 없습니다."

        val identity = state.identities[target.member.id] ?: return "대상의 신원 정보를 확인할 수 없습니다."
        if (!identity.isPubliclyRevealed && target.member.id !in state.knownIdentityOwnerIdsByMafia) {
            return "마피아팀이 아직 알지 못하거나 공개되지 않은 비밀 신원입니다."
        }

        state.lastCapoExecutionAtMillis = now
        state.mafiaExecutionLocationsToday += currentLocation(state, actor)
        killPlayer(game, target, isVoteDeath = false)
        game.sendMainChannelMessageWithImage(
            imageLink = SystemImage.DEATH_BY_MAFIA.imageUrl,
            message = "${target.member.effectiveName}님이 말살되었습니다."
        )

        if (target.job is Agent) {
            game.sendMainChannerMessage("마피아팀이 요원을 처형하는 데 성공하여 승리합니다.")
            GameLoopManager.endGame(game, Team.MAFIA)
        }
        return "${target.member.effectiveName}님을 처형했습니다."
    }

    private suspend fun inquireNpc(game: Game, actor: PlayerData): String {
        val state = state(game) ?: return "말살 모드 게임이 아닙니다."
        if (isMafiaTeam(actor)) return "NPC 탐문은 시민팀 미션입니다."
        if (game.currentPhase != GamePhase.DAY) return "탐문은 낮에만 가능합니다."
        val actorLocation = currentLocation(state, actor)
        val mission = state.citizenMissions.firstOrNull { mission ->
            !mission.isCompleted &&
                mission.type == CitizenMissionType.NPC_INQUIRY &&
                mission.location == actorLocation
        }

        val clue = when (actorLocation) {
            in state.mafiaExecutionLocationsToday ->
                "${actorLocation.displayName}에서 오늘 누군가 수상한 행동을 했어."

            in state.mafiaMissionLocationsToday ->
                "${actorLocation.displayName}에서 수상한 흔적이 있었어."

            else -> {
                val visits = state.visitorsByLocationToday[actorLocation]?.size ?: 0
                "오늘 ${actorLocation.displayName}에 찾아온 사람은 ${visits}명이야."
            }
        }

        if (mission != null) {
            completeMission(game, mission, "NPC 탐문 미션 완료")
        }

        return clue
    }

    private suspend fun performMafiaMission(game: Game, actor: PlayerData): String {
        val state = state(game) ?: return "말살 모드 게임이 아닙니다."
        if (!isMafiaTeam(actor)) return "마피아팀만 수행할 수 있는 미션입니다."
        if (game.currentPhase != GamePhase.DAY) return "마피아 미션은 낮에만 수행할 수 있습니다."
        val count = state.mafiaMissionUseCountByPlayer[actor.member.id] ?: 0
        if (count >= 2) return "하루에 각 마피아는 최대 2번만 마피아 미션을 수행할 수 있습니다."
        val location = currentLocation(state, actor)
        if (location !in state.mafiaNpcLocations) {
            return "이 장소에는 오늘 마피아 미션을 수행할 NPC가 없습니다. 오늘 대상: ${state.mafiaNpcLocations.joinToString(", ") { it.displayName }}"
        }
        val key = "${game.dayCount}:${actor.member.id.value}:${location.name}"
        if (!state.mafiaMissionDoneKeys.add(key)) {
            return "이 장소의 마피아 미션은 이미 수행했습니다."
        }

        state.mafiaMissionUseCountByPlayer[actor.member.id] = count + 1
        state.mafiaMissionLocationsToday += location
        addProgress(game, mafiaDelta = 3, reason = "마피아팀이 ${location.displayName} NPC 미션을 수행했습니다.")
        return "마피아 미션을 수행했습니다. 말살 진척도 +3"
    }

    private suspend fun useAgentProof(game: Game, actor: PlayerData): String {
        val state = state(game) ?: return "말살 모드 게임이 아닙니다."
        if (actor.job !is Agent) return "요원만 증명을 사용할 수 있습니다."
        if (state.agentProofUsed) return "증명은 게임당 한 번만 사용할 수 있습니다."
        if (game.currentPhase != GamePhase.DAY) return "증명은 낮에만 사용할 수 있습니다."
        val location = AnnihilationLocation.entries.random()
        state.agentProofUsed = true
        state.pendingProofPlan = AgentProofPlan(state.movementPhaseNumber + 1, location)
        game.sendMainChannerMessage("요원의 증명: 다음 이동 페이즈에 ${location.displayName}에 협력자가 등장합니다.")
        return "증명을 사용했습니다."
    }

    private suspend fun useAgentImpersonation(game: Game, actor: PlayerData): String {
        val state = state(game) ?: return "말살 모드 게임이 아닙니다."
        if (actor.job !is Agent) return "요원만 사칭을 사용할 수 있습니다."
        if (state.agentImpersonationUsed) return "사칭은 게임당 한 번만 사용할 수 있습니다."
        if (game.currentPhase != GamePhase.DAY) return "사칭은 낮에만 사용할 수 있습니다."
        state.agentImpersonationUsed = true
        state.executionBlockedMovementPhase = state.movementPhaseNumber + 1
        game.sendMainChannerMessage("요원의 사칭: 다음 이동 페이즈 전까지 마피아팀은 처형할 수 없습니다.")
        return "사칭을 사용했습니다."
    }

    private suspend fun useJointInvestigation(
        game: Game,
        actor: PlayerData,
        firstTargetId: Snowflake?,
        secondTargetId: Snowflake?
    ): String {
        val state = state(game) ?: return "말살 모드 게임이 아닙니다."
        if (actor.job !is Agent) return "요원만 합동수사를 사용할 수 있습니다."
        if (state.agentJointInvestigationDay == game.dayCount) return "합동수사는 하루에 한 번만 사용할 수 있습니다."
        if (game.currentPhase != GamePhase.DAY) return "합동수사는 낮에만 사용할 수 있습니다."
        val first = firstTargetId?.let(game::getPlayer) ?: return "A 대상을 선택해 주세요."
        val second = secondTargetId?.let(game::getPlayer) ?: return "B 대상을 선택해 주세요."
        if (first.state.isDead || second.state.isDead) return "사망한 플레이어는 합동수사 대상으로 선택할 수 없습니다."
        if (first.member.id == second.member.id) return "서로 다른 두 플레이어를 선택해 주세요."

        state.agentJointInvestigationDay = game.dayCount
        val mission = CitizenMission(
            id = nextMissionId(state),
            type = CitizenMissionType.JOINT_INVESTIGATION,
            description = "${first.member.effectiveName}와 ${second.member.effectiveName}가 오늘 안에 같은 장소에서 미션 수행",
            reward = 5,
            actorId = first.member.id,
            targetId = second.member.id
        )
        state.citizenMissions += mission
        game.sendMainChannerMessage("합동수사: ${mission.description}에 성공하면 수사 진척도 +5")
        return "합동수사를 발동했습니다."
    }

    private suspend fun useCapoForcedMission(
        game: Game,
        actor: PlayerData,
        firstRaw: String?,
        secondRaw: String?,
        thirdRaw: String?
    ): String {
        val state = state(game) ?: return "말살 모드 게임이 아닙니다."
        if (actor.job !is Capo) return "카포만 이 능력을 사용할 수 있습니다."
        val lastDay = state.capoMissionLastUsedDay
        if (lastDay != null && game.dayCount - lastDay < 2) return "카포 미션은 2일에 1번만 사용할 수 있습니다."
        if (game.currentPhase != GamePhase.DAY) return "카포 미션은 낮에만 사용할 수 있습니다."
        val locations = listOf(firstRaw, secondRaw, thirdRaw).mapNotNull(AnnihilationLocation::parse).toSet()
        if (locations.size != 3) return "서로 다른 장소 3개를 입력해 주세요."
        state.capoMissionLastUsedDay = game.dayCount
        state.pendingCapoMission = CapoForcedMission(game.dayCount, state.movementPhaseNumber + 1, locations)
        game.sendMainChannerMessage("카포의 지령: 다음 이동 페이즈에 ${locations.joinToString(", ") { it.displayName }}에 각각 최소 1명 이상 있어야 합니다.")
        return "카포 미션을 발동했습니다."
    }

    private suspend fun useSoldatoForcedMission(game: Game, actor: PlayerData, rawLocation: String?): String {
        val state = state(game) ?: return "말살 모드 게임이 아닙니다."
        if (actor.job !is Soldato) return "솔다토만 이 능력을 사용할 수 있습니다."
        val lastDay = state.soldatoMissionLastUsedDay
        if (lastDay != null && game.dayCount - lastDay < 2) return "솔다토 미션은 2일에 1번만 사용할 수 있습니다."
        if (game.currentPhase != GamePhase.DAY) return "솔다토 미션은 낮에만 사용할 수 있습니다."
        val location = AnnihilationLocation.parse(rawLocation) ?: return "장소를 입력해 주세요."
        state.soldatoMissionLastUsedDay = game.dayCount
        state.pendingSoldatoMission = SoldatoForcedMission(game.dayCount, state.movementPhaseNumber + 1, location)
        game.sendMainChannerMessage("솔다토의 지령: 다음 이동 페이즈에 생존 시민팀 절반 이상이 ${location.displayName}에 있어야 합니다.")
        return "솔다토 미션을 발동했습니다."
    }

    private suspend fun handOverCapo(game: Game, actor: PlayerData, targetId: Snowflake?): String {
        val state = state(game) ?: return "말살 모드 게임이 아닙니다."
        if (actor.job !is Capo) return "카포만 직위를 넘길 수 있습니다."
        val target = targetId?.let(game::getPlayer) ?: return "직위를 넘길 솔다토를 선택해 주세요."
        if (target.job !is Soldato || target.state.isDead) return "생존한 솔다토에게만 카포 직위를 넘길 수 있습니다."
        if (currentLocation(state, actor) != currentLocation(state, target)) {
            return "같은 장소에 있을 때에만 직위를 넘길 수 있습니다."
        }

        actor.job = Soldato()
        target.job = Capo()
        game.sendMainChannerMessage("카포 직위가 ${actor.member.effectiveName}님에서 ${target.member.effectiveName}님으로 넘어갔습니다.")
        return "카포 직위를 넘겼습니다."
    }

    private fun transferStolenIdentity(
        game: Game,
        actor: PlayerData,
        targetId: Snowflake?,
        rawSecret: String?
    ): String {
        val state = state(game) ?: return "말살 모드 게임이 아닙니다."
        if (!isMafiaTeam(actor)) return "마피아팀만 신분증을 전달할 수 있습니다."
        val target = targetId?.let(game::getPlayer) ?: return "신분증을 전달할 마피아팀 대상을 선택해 주세요."
        if (!isMafiaTeam(target) || target.state.isDead) return "생존한 마피아팀에게만 신분증을 전달할 수 있습니다."
        if (currentLocation(state, actor) != currentLocation(state, target)) {
            return "같은 장소에 있을 때에만 신분증을 전달할 수 있습니다."
        }
        val secret = rawSecret?.trim().orEmpty()
        val ownerId = state.identities.entries.firstOrNull { it.value.code == secret }?.key
            ?: return "전달할 신분증의 비밀 신원을 입력해 주세요."
        if (state.stolenIdentityHolderByOwner[ownerId] != actor.member.id) {
            return "해당 신분증을 보유하고 있지 않습니다."
        }

        state.stolenIdentityHolderByOwner[ownerId] = target.member.id
        return "${target.member.effectiveName}님에게 신분증을 전달했습니다."
    }

    private suspend fun prepareDayState(game: Game) {
        val state = state(game) ?: return
        if (state.missionDay != game.dayCount) {
            state.missionDay = game.dayCount
            state.extraMissionGrantedDay = null
            state.citizenMissions.clear()
            state.mafiaNpcLocations.clear()
            state.mafiaMissionUseCountByPlayer.clear()
            state.mafiaMissionDoneKeys.clear()
            state.mafiaMissionLocationsToday.clear()
            state.mafiaExecutionLocationsToday.clear()
            state.visitorsByLocationToday.clear()
            state.mafiaNpcLocations += AnnihilationLocation.entries.shuffled().take(3)
            repeat(2) { state.citizenMissions += createRandomCitizenMission(game, state) }
            announceDailyMissions(game)
        }
    }

    private suspend fun announceDailyMissions(game: Game) {
        val state = state(game) ?: return
        sendGuideMessage(
            game,
            "오늘의 시민팀 미션",
            buildString {
                state.citizenMissions.forEach { mission ->
                    appendLine("- [${mission.id}] ${mission.description} (${if (mission.isCompleted) "완료" else "진행 중"})")
                }
                appendLine()
                appendLine(buildPublicLocationBoard(game))
            }.trim()
        )
        game.mafiaChannel?.createMessage(
            buildString {
                appendLine("오늘의 마피아팀 편의 정보")
                appendLine("NPC 미션 가능 지역: ${state.mafiaNpcLocations.joinToString(", ") { it.displayName }}")
                appendLine("카포 처형은 공개되었거나 훔쳐 알아낸 비밀 신원으로만 가능합니다.")
                append(buildMafiaLocationBoard(game))
            }.trim()
        )
    }

    private fun createRandomCitizenMission(game: Game, state: AnnihilationGameState): CitizenMission {
        val alive = alivePlayers(game)
        val citizenTeam = alive.filterNot(::isMafiaTeam)
        val type = CitizenMissionType.entries
            .filter { it != CitizenMissionType.JOINT_INVESTIGATION }
            .random()
        val location = AnnihilationLocation.entries.random()
        val secondLocation = AnnihilationLocation.entries
            .filter { it != location && location.isConnectedTo(it) }
            .randomOrNull() ?: AnnihilationLocation.SQUARE
        val actor = citizenTeam.randomOrNull()
        val target = alive.filter { it.member.id != actor?.member?.id }.randomOrNull()

        return when (type) {
            CitizenMissionType.GO_LOCATION -> CitizenMission(
                id = nextMissionId(state),
                type = type,
                description = "시민팀 중 누군가 ${location.displayName}에 도착",
                location = location
            )
            CitizenMissionType.MEET_PLAYER -> CitizenMission(
                id = nextMissionId(state),
                type = type,
                description = "${actor?.member?.effectiveName ?: "지정 플레이어"}가 ${target?.member?.effectiveName ?: "대상"}와 같은 장소에 있기",
                actorId = actor?.member?.id,
                targetId = target?.member?.id
            )
            CitizenMissionType.BE_ALONE -> CitizenMission(
                id = nextMissionId(state),
                type = type,
                description = "시민팀 중 누군가 아무와도 접촉하지 않고 혼자 장소에 있기"
            )
            CitizenMissionType.LOST_ITEM -> CitizenMission(
                id = nextMissionId(state),
                type = type,
                description = "${location.displayName}에서 분실물을 습득한 뒤 ${secondLocation.displayName}(으)로 이동",
                location = location,
                secondLocation = secondLocation
            )
            CitizenMissionType.NPC_INQUIRY -> CitizenMission(
                id = nextMissionId(state),
                type = type,
                description = "${location.displayName}의 NPC를 찾아가 탐문",
                location = location
            )
            CitizenMissionType.JOINT_INVESTIGATION -> error("합동수사는 별도 생성됩니다.")
        }
    }

    private suspend fun evaluateCitizenMissions(game: Game) {
        val state = state(game) ?: return
        state.citizenMissions
            .filterNot { it.isCompleted }
            .toList()
            .forEach { mission ->
                when (mission.type) {
                    CitizenMissionType.GO_LOCATION -> {
                        val location = mission.location ?: return@forEach
                        val success = alivePlayers(game).any { !isMafiaTeam(it) && currentLocation(state, it) == location }
                        if (success) completeMission(game, mission, "지정 지역 도착")
                    }
                    CitizenMissionType.MEET_PLAYER -> {
                        val actor = mission.actorId?.let(game::getPlayer) ?: return@forEach
                        val target = mission.targetId?.let(game::getPlayer) ?: return@forEach
                        if (!actor.state.isDead && !target.state.isDead && currentLocation(state, actor) == currentLocation(state, target)) {
                            completeMission(game, mission, "지정 플레이어 접촉")
                        }
                    }
                    CitizenMissionType.BE_ALONE -> {
                        val byLocation = alivePlayers(game).groupBy { currentLocation(state, it) }
                        val success = byLocation.values.any { players -> players.size == 1 && !isMafiaTeam(players.first()) }
                        if (success) completeMission(game, mission, "단독 체류")
                    }
                    CitizenMissionType.LOST_ITEM -> {
                        val pickup = mission.location ?: return@forEach
                        val delivery = mission.secondLocation ?: return@forEach
                        val holder = mission.holderId?.let(game::getPlayer)
                        if (holder == null || holder.state.isDead) {
                            val newHolder = alivePlayers(game).firstOrNull { !isMafiaTeam(it) && currentLocation(state, it) == pickup }
                            if (newHolder != null) {
                                mission.holderId = newHolder.member.id
                                game.sendMainChannerMessage("${newHolder.member.effectiveName}님이 ${pickup.displayName}에서 분실물을 습득했습니다.")
                            }
                        } else if (currentLocation(state, holder) == delivery) {
                            completeMission(game, mission, "분실물 전달")
                        }
                    }
                    CitizenMissionType.NPC_INQUIRY -> Unit
                    CitizenMissionType.JOINT_INVESTIGATION -> {
                        val first = mission.actorId?.let(game::getPlayer) ?: return@forEach
                        val second = mission.targetId?.let(game::getPlayer) ?: return@forEach
                        if (!first.state.isDead && !second.state.isDead && currentLocation(state, first) == currentLocation(state, second)) {
                            completeMission(game, mission, "합동수사 성공")
                        }
                    }
                }
            }

        grantExtraCitizenMissionIfNeeded(game)
    }

    private suspend fun grantExtraCitizenMissionIfNeeded(game: Game) {
        val state = state(game) ?: return
        if (state.extraMissionGrantedDay == game.dayCount) return
        val dailyMissions = state.citizenMissions.filter { it.type != CitizenMissionType.JOINT_INVESTIGATION }
        if (dailyMissions.isEmpty() || dailyMissions.any { !it.isCompleted }) return
        state.extraMissionGrantedDay = game.dayCount
        val mission = createRandomCitizenMission(game, state)
        state.citizenMissions += mission
        game.sendMainChannerMessage("시민팀이 오늘의 모든 미션을 처음 완료하여 추가 미션을 얻었습니다.\n- [${mission.id}] ${mission.description}")
    }

    private suspend fun completeMission(game: Game, mission: CitizenMission, reason: String) {
        if (mission.isCompleted) return
        mission.isCompleted = true
        addProgress(game, citizenDelta = mission.reward, reason = "$reason: ${mission.description}")
    }

    private suspend fun addProgress(
        game: Game,
        citizenDelta: Int = 0,
        mafiaDelta: Int = 0,
        reason: String
    ) {
        val state = state(game) ?: return
        if (citizenDelta > 0) state.citizenProgress = (state.citizenProgress + citizenDelta).coerceAtMost(WIN_PROGRESS)
        if (mafiaDelta > 0) state.mafiaProgress = (state.mafiaProgress + mafiaDelta).coerceAtMost(WIN_PROGRESS)
        game.sendMainChannerMessage(
            "$reason\n수사 진척도: ${state.citizenProgress}/100, 말살 진척도: ${state.mafiaProgress}/100"
        )
        checkProgressWin(game)
    }

    private suspend fun resolvePendingMovementEvents(game: Game, phase: Int) {
        val state = state(game) ?: return
        state.pendingProofPlan
            ?.takeIf { !it.resolved && it.movementPhase == phase }
            ?.let { plan ->
                plan.resolved = true
                val candidates = alivePlayers(game)
                    .filterNot(::isMafiaTeam)
                    .filter { currentLocation(state, it) == plan.location }
                val confirmed = candidates.randomOrNull()
                if (confirmed != null) {
                    state.identities[confirmed.member.id]?.confirmedCitizen = true
                    game.sendMainChannerMessage("협력자가 ${plan.location.displayName}에서 ${confirmed.member.effectiveName}님이 확실한 시민임을 증명했습니다.")
                } else {
                    game.sendMainChannerMessage("협력자가 ${plan.location.displayName}에 등장했지만 시민팀 플레이어를 만나지 못했습니다.")
                }
            }

        state.pendingCapoMission
            ?.takeIf { !it.resolved && it.movementPhase == phase }
            ?.let { mission ->
                mission.resolved = true
                val missing = mission.locations.filter { location ->
                    alivePlayers(game).none { currentLocation(state, it) == location }
                }
                if (missing.isNotEmpty()) {
                    addProgress(
                        game,
                        mafiaDelta = 10,
                        reason = "카포의 지령 실패 지역 발생: ${missing.joinToString(", ") { it.displayName }}"
                    )
                } else {
                    game.sendMainChannerMessage("카포의 지령을 막아 말살 진척도가 오르지 않았습니다.")
                }
            }

        state.pendingSoldatoMission
            ?.takeIf { !it.resolved && it.movementPhase == phase }
            ?.let { mission ->
                mission.resolved = true
                val aliveCitizenCount = alivePlayers(game).count { !isMafiaTeam(it) }
                val required = ceil(aliveCitizenCount / 2.0).toInt()
                val present = alivePlayers(game).count { !isMafiaTeam(it) && currentLocation(state, it) == mission.location }
                if (present < required) {
                    addProgress(
                        game,
                        mafiaDelta = 10,
                        reason = "솔다토의 지령을 막지 못했습니다. ${mission.location.displayName} 시민팀 $present/$required"
                    )
                } else {
                    game.sendMainChannerMessage("솔다토의 지령을 막아 말살 진척도가 오르지 않았습니다.")
                }
            }

        if (state.executionBlockedMovementPhase == phase) {
            state.executionBlockedMovementPhase = null
            game.sendMainChannerMessage("요원의 사칭으로 인한 처형 제한이 해제되었습니다.")
        }
    }

    private suspend fun stealIdentitiesIfEligible(game: Game) {
        val state = state(game) ?: return
        val stolenLocationThisPhase = mutableSetOf<AnnihilationLocation>()
        val mafiaPlayers = alivePlayers(game).filter(::isMafiaTeam)
        val targets = alivePlayers(game).filterNot(::isMafiaTeam)

        mafiaPlayers.forEach { mafia ->
            targets.forEach { target ->
                val currentSame = currentLocation(state, mafia) == currentLocation(state, target)
                val key = streakKey(mafia, target)
                val nextStreak = if (currentSame) (state.samePlaceStreaks[key] ?: 0) + 1 else 0
                state.samePlaceStreaks[key] = nextStreak
                val location = currentLocation(state, mafia)
                if (
                    nextStreak >= 2 &&
                    location !in stolenLocationThisPhase &&
                    target.member.id !in state.stolenIdentityHolderByOwner
                ) {
                    state.stolenIdentityHolderByOwner[target.member.id] = mafia.member.id
                    state.knownIdentityOwnerIdsByMafia += target.member.id
                    stolenLocationThisPhase += location
                    val identity = state.identities[target.member.id]?.code ?: "알 수 없음"
                    game.mafiaChannel?.createMessage(
                        "${mafia.member.effectiveName}님이 ${location.displayName}에서 ${target.member.effectiveName}님의 신분증을 훔쳤습니다. 비밀 신원: $identity"
                    )
                }
            }
        }
    }

    private suspend fun notifyStolenIdentityOwners(game: Game) {
        val state = state(game) ?: return
        val stolenOwners = state.stolenIdentityHolderByOwner.keys
            .mapNotNull(game::getPlayer)
            .filter { !it.state.isDead && !isMafiaTeam(it) }
        if (stolenOwners.isEmpty()) return

        game.sendMainChannerMessage("투표 시간이 시작되며 시민팀 일부가 신분증 도난 사실을 알아차렸습니다.")
        stolenOwners.forEach { owner ->
            runCatching {
                owner.member.getDmChannel().createMessage("당신의 신분증이 도둑맞았습니다.")
            }
        }
    }

    private suspend fun applyLocationVoiceChannelMoves(game: Game) {
        val state = state(game) ?: return
        val failedPlayers = mutableListOf<String>()

        alivePlayers(game).forEach { player ->
            val location = currentLocation(state, player)
            val channelId = locationVoiceChannelIds[location] ?: return@forEach
            val currentVoiceChannelId = runCatching {
                player.member.getVoiceStateOrNull()?.channelId
            }.getOrNull()
            if (currentVoiceChannelId == channelId) return@forEach

            runCatching {
                player.member.edit {
                    voiceChannelId = channelId
                }
            }.onFailure { error ->
                failedPlayers += "${player.member.effectiveName}(${location.displayName}: ${error.message ?: "알 수 없는 오류"})"
            }
        }

        if (failedPlayers.isNotEmpty()) {
            game.sendMainChannerMessage(
                "일부 플레이어를 장소 음성 채널로 이동하지 못했습니다.\n${failedPlayers.joinToString("\n") { "- $it" }}"
            )
        }
    }

    private suspend fun revealIdentity(game: Game, player: PlayerData, reason: String) {
        val state = state(game) ?: return
        val identity = state.identities[player.member.id] ?: return
        identity.isPubliclyRevealed = true
        game.sendMainChannerMessage("$reason\n${player.member.effectiveName}님의 비밀 신원: ${identity.code}")
    }

    private fun updateVoteSuspectIfNeeded(game: Game) {
        val state = state(game) ?: return
        if (state.voteSuspectId != null) return
        val alivePlayers = alivePlayers(game)
        val threshold = suspectThreshold(alivePlayers.size)
        val leader = voteCounts(game).maxByOrNull { it.value } ?: return
        if (leader.value < threshold) return
        state.voteSuspectId = leader.key
        val remaining = state.voteEndAtMillis - System.currentTimeMillis()
        if (remaining > VOTE_SHORTEN_REMAINING_MS) {
            state.voteEndAtMillis = System.currentTimeMillis() + VOTE_SHORTEN_REMAINING_MS
        }
    }

    private fun buildVoteStatus(game: Game, alivePlayers: List<PlayerData>): String {
        val state = state(game) ?: return "말살 모드 투표 상태를 확인할 수 없습니다."
        val counts = voteCounts(game)
        val leader = counts.maxByOrNull { it.value }
        val threshold = suspectThreshold(alivePlayers.size)
        return buildString {
            appendLine("말살 모드 투표 현황")
            alivePlayers.forEach { voter ->
                val rawTarget = state.votes[voter.member.id]
                val display = when {
                    rawTarget == null -> "미투표"
                    rawTarget == SKIP_VOTE_VALUE -> "건너뛰기"
                    else -> game.getPlayer(Snowflake(rawTarget))?.member?.effectiveName ?: "알 수 없음"
                }
                appendLine("- ${voter.member.effectiveName} -> $display")
            }
            appendLine()
            if (state.voteSuspectId != null) {
                val suspect = game.getPlayer(state.voteSuspectId!!)
                appendLine("용의자: ${suspect?.member?.effectiveName ?: "알 수 없음"}")
                append("남은 투표 시간은 최대 10초로 조정되었습니다.")
            } else if (leader != null) {
                val leaderPlayer = game.getPlayer(leader.key)
                val needed = (threshold - leader.value).coerceAtLeast(0)
                append("현재 최다 득표: ${leaderPlayer?.member?.effectiveName ?: "없음"} ${leader.value}표, 용의자 상정까지 ${needed}표 필요")
            } else {
                append("아직 득표자가 없습니다. 용의자 상정 필요 표: $threshold")
            }
        }
    }

    private fun voteCounts(game: Game): Map<Snowflake, Int> {
        val state = state(game) ?: return emptyMap()
        return state.votes.values
            .filter { it != SKIP_VOTE_VALUE }
            .mapNotNull { raw -> runCatching { Snowflake(raw) }.getOrNull() }
            .groupingBy { it }
            .eachCount()
    }

    private fun suspectThreshold(aliveCount: Int): Int = (aliveCount / 2) + 1

    private suspend fun checkProgressWin(game: Game): Boolean {
        val state = state(game) ?: return false
        val winner = when {
            state.citizenProgress >= WIN_PROGRESS -> Team.CITIZEN
            state.mafiaProgress >= WIN_PROGRESS -> Team.MAFIA
            else -> null
        } ?: return false
        GameLoopManager.endGame(game, winner)
        return true
    }

    private suspend fun killPlayer(game: Game, victim: PlayerData, isVoteDeath: Boolean) {
        if (victim.state.isDead) return
        victim.state.isDead = true
        victim.state.diedDayCount = game.dayCount
        runCatching {
            victim.member.edit {
                muted = true
            }
        }
        game.mainChannel?.edit {
            addMemberOverwrite(victim.member.id) {
                denied = Permissions(Permission.SendMessages)
            }
        }
        game.deadChannel?.createMessage(victim.member.mention)
        GameReplayLogger.log(
            game = game,
            type = ReplayLogType.DEATH,
            visibility = ReplayVisibility.PUBLIC,
            title = if (isVoteDeath) "투표 사망" else "말살",
            body = "${victim.member.effectiveName} 사망"
        )
    }

    private fun returnStolenIdentitiesHeldBy(game: Game, holder: PlayerData) {
        val state = state(game) ?: return
        state.stolenIdentityHolderByOwner.entries.removeIf { (_, holderId) -> holderId == holder.member.id }
    }

    private suspend fun updateMainChannelPermissions(game: Game) {
        val mainChannel = game.mainChannel ?: return
        mainChannel.edit {
            addRoleOverwrite(game.guild.id) {
                allowed = Permissions(Permission.ReadMessageHistory)
                denied = Permissions()
            }
            game.playerDatas.forEach { player ->
                addMemberOverwrite(player.member.id) {
                    allowed = Permissions(Permission.ViewChannel)
                    denied = if (player.state.isDead) Permissions(Permission.SendMessages) else Permissions()
                }
            }
            game.spectatorMembers.forEach { spectator ->
                addMemberOverwrite(spectator.id) {
                    allowed = Permissions(Permission.ViewChannel)
                    denied = Permissions(Permission.SendMessages)
                }
            }
        }
    }

    private suspend fun updateMafiaChannelPermissions(game: Game) {
        val mafiaChannel = game.mafiaChannel ?: return
        mafiaChannel.edit {
            addRoleOverwrite(game.guild.id) {
                denied = Permissions(Permission.ViewChannel, Permission.ReadMessageHistory, Permission.SendMessages)
            }
            game.playerDatas.forEach { player ->
                if (isMafiaTeam(player)) {
                    addMemberOverwrite(player.member.id) {
                        allowed = Permissions(Permission.ViewChannel, Permission.SendMessages)
                        denied = Permissions(Permission.ReadMessageHistory)
                    }
                } else {
                    addMemberOverwrite(player.member.id) {
                        denied = Permissions(Permission.ViewChannel, Permission.SendMessages, Permission.ReadMessageHistory)
                    }
                }
            }
        }
    }

    private suspend fun muteAll(game: Game, mute: Boolean) {
        game.playerDatas.forEach { player ->
            runCatching {
                player.member.edit { muted = mute }
            }
        }
        game.spectatorMembers.forEach { spectator ->
            runCatching {
                spectator.edit { muted = true }
            }
        }
    }

    private suspend fun muteAlivePlayersForDay(game: Game) {
        game.playerDatas.forEach { player ->
            runCatching {
                player.member.edit { muted = player.state.isDead }
            }
        }
        game.spectatorMembers.forEach { spectator ->
            runCatching {
                spectator.edit { muted = true }
            }
        }
    }

    private suspend fun notifyMafiaMovementIntel(game: Game) {
        val state = state(game) ?: return
        if (!state.isMovementPhaseActive) return
        val selected = state.movementSelections
        val lines = alivePlayers(game).joinToString("\n") { player ->
            val current = currentLocation(state, player)
            val choice = selected[player.member.id]?.displayName ?: "미선택"
            "- ${player.member.effectiveName}: 현재 ${current.displayName}, 선택 $choice"
        }
        game.mafiaChannel?.createMessage("이동 페이즈 정보\n$lines")
    }

    private fun buildStatus(game: Game, actor: PlayerData): String {
        val state = state(game) ?: return "말살 모드 상태가 없습니다."
        val identity = state.identities[actor.member.id]
        val location = currentLocation(state, actor)
        val stolen = state.stolenIdentityHolderByOwner
            .filterValues { it == actor.member.id }
            .keys
            .mapNotNull { ownerId ->
                val owner = game.getPlayer(ownerId) ?: return@mapNotNull null
                val code = state.identities[ownerId]?.code ?: return@mapNotNull null
                "${owner.member.effectiveName}($code)"
            }
        return buildString {
            appendLine("말살 모드 상태")
            appendLine("이름: ${actor.member.effectiveName}")
            appendLine("직업: ${actor.job?.name ?: "알 수 없음"}")
            appendLine("비밀 신원: ${identity?.code ?: "알 수 없음"}")
            appendLine("현재 위치: ${location.displayName}")
            appendLine("수사 진척도: ${state.citizenProgress}/100")
            appendLine("말살 진척도: ${state.mafiaProgress}/100")
            if (stolen.isNotEmpty()) {
                appendLine("보유 신분증: ${stolen.joinToString(", ")}")
            }
            if (identity?.confirmedCitizen == true) {
                appendLine("공개 상태: 확실한 시민")
            }
            appendLine()
            appendLine("이동 가능 장소")
            appendLine(connectedLocations(location).joinToString(", ") { it.displayName })
            appendLine()
            appendLine("내 이동 힌트")
            appendLine(buildPersonalRouteHint(game, actor))
            appendLine()
            appendLine("현재 위치 행동")
            appendLine(currentLocationActions(game, actor).ifEmpty { listOf("바로 수행할 특수 행동 없음") }.joinToString("\n") { "- $it" })
            appendLine()
            appendLine("사용 가능한 말살 명령")
            append(availableActionHints(game, actor).joinToString("\n") { "- $it" })
        }.trim()
    }

    private fun buildHelp(game: Game, actor: PlayerData): String {
        return buildString {
            appendLine("말살 모드 도움")
            appendLine("- `/말살 상태`: 내 위치, 이동 가능 장소, 미션 힌트, 사용 가능 행동 확인")
            appendLine("- `/말살 탐문`: 현재 장소의 NPC 탐문 미션 수행")
            appendLine("- `/말살 마피아미션`: 마피아팀 NPC 미션 수행")
            appendLine("- `/말살 처형 secret:비밀 신원`: 카포 처형")
            appendLine("- `/말살 증명`, `/말살 사칭`, `/말살 합동수사`: 요원 능력")
            appendLine("- `/말살 카포미션`, `/말살 솔다토미션`: 마피아팀 공개 압박 미션")
            appendLine()
            appendLine("현재 내 힌트")
            append(buildStatus(game, actor))
        }.trim()
    }

    private suspend fun sendGuideMessage(game: Game, title: String, body: String) {
        val content = "## $title\n${body.trim()}".trim()
        splitDiscordMessage(content).forEach { chunk ->
            game.sendMainChannerMessage(chunk)
        }
    }

    private fun splitDiscordMessage(message: String, maxLength: Int = 1800): List<String> {
        if (message.length <= maxLength) return listOf(message)
        val chunks = mutableListOf<String>()
        var current = StringBuilder()
        message.lineSequence().forEach { line ->
            if (current.isNotEmpty() && current.length + line.length + 1 > maxLength) {
                chunks += current.toString().trimEnd()
                current = StringBuilder()
            }
            if (line.length > maxLength) {
                line.chunked(maxLength).forEach { chunks += it }
            } else {
                current.appendLine(line)
            }
        }
        if (current.isNotBlank()) chunks += current.toString().trimEnd()
        return chunks
    }

    private fun buildPublicLocationBoard(game: Game): String {
        return buildString {
            appendLine("장소별 공개 미션/이벤트")
            AnnihilationLocation.entries.forEach { location ->
                val hints = publicLocationHints(game, location)
                appendLine("- ${location.displayName}: ${hints.ifEmpty { listOf("표시된 공개 미션 없음") }.joinToString(" / ")}")
            }
        }.trim()
    }

    private fun buildCompactLocationBoard(game: Game): String {
        return buildString {
            AnnihilationLocation.entries.forEach { location ->
                val hints = publicLocationHints(game, location).take(2)
                if (hints.isNotEmpty()) {
                    appendLine("- ${location.displayName}: ${hints.joinToString(" / ")}")
                }
            }
        }.trim().ifBlank { "현재 공개 위치 미션은 없습니다." }
    }

    private fun buildMafiaLocationBoard(game: Game): String {
        return buildString {
            AnnihilationLocation.entries.forEach { location ->
                val hints = mafiaLocationHints(game, location)
                appendLine("- ${location.displayName}: ${hints.ifEmpty { listOf("특이사항 없음") }.joinToString(" / ")}")
            }
        }.trim()
    }

    private fun publicLocationHints(game: Game, location: AnnihilationLocation): List<String> {
        val state = state(game) ?: return emptyList()
        val hints = mutableListOf<String>()

        state.citizenMissions.filterNot { it.isCompleted }.forEach { mission ->
            when (mission.type) {
                CitizenMissionType.GO_LOCATION -> {
                    if (mission.location == location) hints += "미션 #${mission.id}: 시민팀 도착"
                }
                CitizenMissionType.NPC_INQUIRY -> {
                    if (mission.location == location) hints += "미션 #${mission.id}: `/말살 탐문`"
                }
                CitizenMissionType.LOST_ITEM -> {
                    if (mission.location == location && mission.holderId == null) {
                        hints += "미션 #${mission.id}: 분실물 습득"
                    }
                    if (mission.secondLocation == location) {
                        val holderName = mission.holderId?.let(game::getPlayer)?.member?.effectiveName ?: "습득자"
                        hints += "미션 #${mission.id}: $holderName 분실물 전달지"
                    }
                }
                CitizenMissionType.MEET_PLAYER -> {
                    val actor = mission.actorId?.let(game::getPlayer)
                    val target = mission.targetId?.let(game::getPlayer)
                    if (actor != null && currentLocation(state, actor) == location) {
                        hints += "미션 #${mission.id}: ${actor.member.effectiveName} 대기 중"
                    }
                    if (target != null && currentLocation(state, target) == location) {
                        hints += "미션 #${mission.id}: ${target.member.effectiveName} 위치"
                    }
                }
                CitizenMissionType.JOINT_INVESTIGATION -> {
                    val first = mission.actorId?.let(game::getPlayer)
                    val second = mission.targetId?.let(game::getPlayer)
                    if (listOfNotNull(first, second).any { currentLocation(state, it) == location }) {
                        hints += "합동수사 #${mission.id}: 대상 위치"
                    }
                }
                CitizenMissionType.BE_ALONE -> Unit
            }
        }

        state.pendingProofPlan
            ?.takeIf { !it.resolved && it.location == location }
            ?.let { hints += "다음 이동 페이즈 협력자" }
        state.pendingCapoMission
            ?.takeIf { !it.resolved && location in it.locations }
            ?.let { hints += "카포 지령 방어 필요" }
        state.pendingSoldatoMission
            ?.takeIf { !it.resolved && it.location == location }
            ?.let { hints += "솔다토 지령 방어 필요" }

        return hints.distinct()
    }

    private fun mafiaLocationHints(game: Game, location: AnnihilationLocation): List<String> {
        val state = state(game) ?: return emptyList()
        val hints = publicLocationHints(game, location).toMutableList()
        if (location in state.mafiaNpcLocations) {
            hints += "마피아 NPC 미션 가능"
        }
        if (location in state.mafiaExecutionLocationsToday) {
            hints += "오늘 처형 발생"
        }
        if (location in state.mafiaMissionLocationsToday) {
            hints += "오늘 마피아 미션 수행"
        }
        return hints.distinct()
    }

    private fun buildMovementRouteBoard(game: Game, players: List<PlayerData>): String {
        return buildString {
            appendLine("플레이어별 이동 힌트")
            players.forEach { player ->
                appendLine("- ${player.member.effectiveName}: ${buildPersonalRouteHint(game, player)}")
            }
        }.trim()
    }

    private fun buildPersonalRouteHint(game: Game, player: PlayerData): String {
        val state = state(game) ?: return "말살 모드 상태 없음"
        val current = currentLocation(state, player)
        return connectedLocations(current)
            .joinToString(" | ") { location ->
                "${location.displayName}: ${destinationHintFor(game, player, location)}"
            }
    }

    private fun destinationHintFor(game: Game, player: PlayerData, destination: AnnihilationLocation): String {
        val state = state(game) ?: return "상태 없음"
        val current = currentLocation(state, player)
        if (!current.isConnectedTo(destination)) return "이동 불가"

        val hints = mutableListOf<String>()
        hints += publicLocationHints(game, destination)
        if (!isMafiaTeam(player)) {
            hints += citizenRouteHints(game, player, destination)
        } else {
            hints += mafiaRouteHints(game, player, destination)
        }

        return hints.distinct().take(4).joinToString(", ").ifBlank { "특이사항 없음" }
    }

    private fun citizenRouteHints(game: Game, player: PlayerData, destination: AnnihilationLocation): List<String> {
        val state = state(game) ?: return emptyList()
        val hints = mutableListOf<String>()
        state.citizenMissions.filterNot { it.isCompleted }.forEach { mission ->
            when (mission.type) {
                CitizenMissionType.GO_LOCATION -> {
                    if (mission.location == destination) hints += "도착 미션 가능(+${mission.reward})"
                }
                CitizenMissionType.NPC_INQUIRY -> {
                    if (mission.location == destination) hints += "탐문 가능"
                }
                CitizenMissionType.LOST_ITEM -> {
                    if (mission.location == destination && mission.holderId == null) hints += "분실물 습득 가능"
                    if (mission.secondLocation == destination && mission.holderId == player.member.id) hints += "분실물 전달 가능"
                }
                CitizenMissionType.MEET_PLAYER -> {
                    val actorMatch = mission.actorId == player.member.id
                    val targetMatch = mission.targetId == player.member.id
                    val other = when {
                        actorMatch -> mission.targetId?.let(game::getPlayer)
                        targetMatch -> mission.actorId?.let(game::getPlayer)
                        else -> null
                    }
                    if (other != null && !other.state.isDead && currentLocation(state, other) == destination) {
                        hints += "${other.member.effectiveName}와 접촉 가능"
                    }
                }
                CitizenMissionType.JOINT_INVESTIGATION -> {
                    val related = mission.actorId == player.member.id || mission.targetId == player.member.id
                    val otherId = if (mission.actorId == player.member.id) mission.targetId else mission.actorId
                    val other = otherId?.let(game::getPlayer)
                    if (related && other != null && !other.state.isDead && currentLocation(state, other) == destination) {
                        hints += "합동수사 가능(+${mission.reward})"
                    }
                }
                CitizenMissionType.BE_ALONE -> {
                    val occupants = alivePlayers(game).count { currentLocation(state, it) == destination }
                    if (occupants == 0 || (occupants == 1 && destination == currentLocation(state, player))) {
                        hints += "혼자 있기 후보"
                    }
                }
            }
        }
        return hints
    }

    private fun mafiaRouteHints(game: Game, player: PlayerData, destination: AnnihilationLocation): List<String> {
        val state = state(game) ?: return emptyList()
        val hints = mutableListOf<String>()
        if (destination in state.mafiaNpcLocations) {
            val used = state.mafiaMissionUseCountByPlayer[player.member.id] ?: 0
            hints += if (used < 2) "마피아 미션 가능(${2 - used}회 남음)" else "마피아 미션 일일 한도 도달"
        }
        val stealCandidates = alivePlayers(game)
            .filterNot(::isMafiaTeam)
            .filter { target -> currentLocation(state, target) == destination }
            .filter { target -> (state.samePlaceStreaks[streakKey(player, target)] ?: 0) >= 1 }
            .map { it.member.effectiveName }
        if (stealCandidates.isNotEmpty()) {
            hints += "신분증 탈취 후보: ${stealCandidates.joinToString(", ")}"
        }
        if (player.job is Capo) {
            val knownCount = state.knownIdentityOwnerIdsByMafia.count { ownerId ->
                game.getPlayer(ownerId)?.state?.isDead == false
            }
            hints += "카포 처형용 신원 ${knownCount}개 파악"
        }
        return hints
    }

    private fun buildCurrentLocationActionBoard(game: Game): String {
        val state = state(game) ?: return "말살 모드 상태 없음"
        return alivePlayers(game)
            .joinToString("\n") { player ->
                val actions = currentLocationActions(game, player)
                "- ${player.member.effectiveName}(${currentLocation(state, player).displayName}): ${
                    actions.ifEmpty { listOf("즉시 행동 없음") }.joinToString(", ")
                }"
            }
    }

    private fun currentLocationActions(game: Game, player: PlayerData): List<String> {
        val state = state(game) ?: return emptyList()
        val location = currentLocation(state, player)
        val actions = mutableListOf<String>()
        if (!isMafiaTeam(player)) {
            if (state.citizenMissions.any { !it.isCompleted && it.type == CitizenMissionType.NPC_INQUIRY && it.location == location }) {
                actions += "`/말살 탐문`"
            }
            state.citizenMissions
                .filter { !it.isCompleted && it.type == CitizenMissionType.LOST_ITEM && it.secondLocation == location && it.holderId == player.member.id }
                .forEach { actions += "분실물 전달 대기" }
        } else {
            if (location in state.mafiaNpcLocations && (state.mafiaMissionUseCountByPlayer[player.member.id] ?: 0) < 2) {
                actions += "`/말살 마피아미션`"
            }
            if (player.job is Capo) {
                actions += "`/말살 처형 secret:<비밀 신원>`"
            }
        }
        return actions.distinct()
    }

    private fun availableActionHints(game: Game, player: PlayerData): List<String> {
        val state = state(game) ?: return emptyList()
        val hints = mutableListOf("`/말살 상태`", "`/말살 도움`")
        hints += currentLocationActions(game, player)
        when (player.job) {
            is Agent -> {
                if (!state.agentProofUsed) hints += "`/말살 증명`"
                if (!state.agentImpersonationUsed) hints += "`/말살 사칭`"
                if (state.agentJointInvestigationDay != game.dayCount) hints += "`/말살 합동수사 target:@A target2:@B`"
            }
            is Capo -> {
                val lastDay = state.capoMissionLastUsedDay
                if (lastDay == null || game.dayCount - lastDay >= 2) {
                    hints += "`/말살 카포미션 location:장소1 location2:장소2 location3:장소3`"
                }
                hints += "`/말살 직위양도 target:@솔다토`"
                hints += "`/말살 신분증전달 target:@솔다토 secret:<비밀 신원>`"
            }
            is Soldato -> {
                val lastDay = state.soldatoMissionLastUsedDay
                if (lastDay == null || game.dayCount - lastDay >= 2) {
                    hints += "`/말살 솔다토미션 location:장소`"
                }
                hints += "`/말살 신분증전달 target:@카포 secret:<비밀 신원>`"
            }
        }
        return hints.distinct()
    }

    private fun connectedLocations(location: AnnihilationLocation): List<AnnihilationLocation> =
        AnnihilationLocation.entries.filter { location.isConnectedTo(it) }

    private fun currentLocation(state: AnnihilationGameState, player: PlayerData): AnnihilationLocation =
        state.locations[player.member.id] ?: AnnihilationLocation.SQUARE

    private fun alivePlayers(game: Game): List<PlayerData> =
        game.playerDatas.filter { !it.state.isDead }

    private fun findAliveAgent(game: Game): PlayerData? =
        game.playerDatas.firstOrNull { !it.state.isDead && it.job is Agent }

    private fun isMafiaTeam(player: PlayerData): Boolean =
        player.job is Capo || player.job is Soldato || player.job is Evil && player.job !is Citizen && player.job !is Agent

    private fun state(game: Game): AnnihilationGameState? = game.annihilationState

    private fun generateSecretCode(usedCodes: MutableSet<String>): String {
        while (true) {
            val code = "${adjectives.random()} ${nouns.random()}"
            if (usedCodes.add(code)) return code
        }
    }

    private fun streakKey(mafia: PlayerData, target: PlayerData): String =
        "${mafia.member.id.value}:${target.member.id.value}"

    private fun nextMissionId(state: AnnihilationGameState): Int =
        state.nextMissionId++

    private suspend fun Message.editContentSafely(newContent: String) {
        runCatching {
            edit {
                content = newContent
            }
        }
    }
}
