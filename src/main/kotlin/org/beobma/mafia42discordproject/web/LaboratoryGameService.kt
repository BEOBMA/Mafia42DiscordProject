package org.beobma.mafia42discordproject.web

import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.job.JobManager
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.evil.Evil

internal enum class LaboratoryPhase(val label: String) {
    SETUP("게임 설정"),
    NIGHT("밤"),
    DAY("낮"),
    MAIN_VOTE("본투표"),
    PROS_CONS("찬반 투표")
}

internal data class LaboratoryAbilityDefinition(
    val name: String,
    val description: String,
    val image: String,
    val phase: LaboratoryPhase,
    val requiresTarget: Boolean,
    val requiresJobSelection: Boolean
)

internal data class LaboratoryJobDefinition(
    val name: String,
    val description: String,
    val image: String?,
    val isEvil: Boolean,
    val abilities: List<LaboratoryAbilityDefinition>
)

internal data class LaboratoryPlayerSetup(
    val name: String,
    val jobName: String?
)

internal data class LaboratoryPlayerSnapshot(
    val id: String,
    val name: String,
    val isHuman: Boolean,
    val isAlive: Boolean,
    val jobName: String?,
    val jobImage: String?,
    val abilities: List<LaboratoryAbilityDefinition>
)

internal data class LaboratoryActionSnapshot(
    val actorId: String,
    val actorName: String,
    val abilityName: String,
    val targetId: String?,
    val targetName: String?,
    val selectedJobName: String?
)

internal data class LaboratoryEventSnapshot(
    val sequence: Long,
    val dayCount: Int,
    val phase: LaboratoryPhase,
    val title: String,
    val body: String
)

internal data class LaboratorySnapshot(
    val phase: LaboratoryPhase,
    val dayCount: Int,
    val players: List<LaboratoryPlayerSnapshot>,
    val jobs: List<LaboratoryJobDefinition>,
    val actions: List<LaboratoryActionSnapshot>,
    val votes: Map<String, String?>,
    val prosConsVotes: Map<String, Boolean>,
    val defenseTargetId: String?,
    val events: List<LaboratoryEventSnapshot>
)

internal data class LaboratorySessionCreated(
    val token: String,
    val snapshot: LaboratorySnapshot
)

internal data class LaboratoryOperationResult(
    val success: Boolean,
    val message: String,
    val snapshot: LaboratorySnapshot? = null
)

internal class LaboratoryGameService(
    private val catalogProvider: () -> List<LaboratoryJobDefinition>,
    private val nowMillis: () -> Long = System::currentTimeMillis
) {
    private data class LaboratoryPlayer(
        val id: String,
        var name: String,
        val isHuman: Boolean,
        var jobName: String?,
        var isAlive: Boolean = true
    )

    private data class LaboratoryAction(
        val actorId: String,
        val abilityName: String,
        val targetId: String?,
        val selectedJobName: String?
    )

    private data class LaboratoryEvent(
        val sequence: Long,
        val dayCount: Int,
        val phase: LaboratoryPhase,
        val title: String,
        val body: String
    )

    private data class LaboratorySession(
        val token: String,
        var lastAccessMillis: Long,
        var phase: LaboratoryPhase = LaboratoryPhase.SETUP,
        var dayCount: Int = 0,
        var players: MutableList<LaboratoryPlayer> = defaultPlayers(),
        val actions: MutableMap<String, LaboratoryAction> = linkedMapOf(),
        val votes: MutableMap<String, String?> = linkedMapOf(),
        val prosConsVotes: MutableMap<String, Boolean> = linkedMapOf(),
        var defenseTargetId: String? = null,
        val events: MutableList<LaboratoryEvent> = mutableListOf(),
        var nextSequence: Long = 1
    )

    private val secureRandom = SecureRandom()
    private val sessions = ConcurrentHashMap<String, LaboratorySession>()

    fun createSession(): LaboratorySessionCreated {
        pruneExpiredSessions()
        val token = ByteArray(32)
            .also(secureRandom::nextBytes)
            .let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }
        val session = LaboratorySession(token = token, lastAccessMillis = nowMillis())
        sessions[token] = session
        return LaboratorySessionCreated(token, synchronized(session) { snapshot(session) })
    }

    fun clear() {
        sessions.clear()
    }

    fun getState(token: String?): LaboratorySnapshot? = session(token)?.let { current ->
        synchronized(current) { snapshot(current) }
    }

    fun updateSetup(token: String?, setup: List<LaboratoryPlayerSetup>): LaboratoryOperationResult = operate(token) { session ->
        if (session.phase != LaboratoryPhase.SETUP) return@operate failure("게임 시작 후에는 참가자 구성을 변경할 수 없습니다.")
        if (setup.size !in MIN_PLAYERS..MAX_PLAYERS) {
            return@operate failure("실험실 참가자는 $MIN_PLAYERS~${MAX_PLAYERS}명으로 설정해 주세요.")
        }
        val catalog = catalogProvider().associateBy(LaboratoryJobDefinition::name)
        val sanitized = setup.mapIndexed { index, player ->
            val name = player.name.trim().take(MAX_PLAYER_NAME_LENGTH)
                .ifBlank { if (index == 0) "나" else "봇 $index" }
            val jobName = player.jobName?.trim()?.takeIf(String::isNotEmpty)
            if (jobName != null && jobName !in catalog) {
                return@operate failure("존재하지 않는 직업입니다: $jobName")
            }
            LaboratoryPlayer(
                id = if (index == 0) HUMAN_PLAYER_ID else "bot-$index",
                name = name,
                isHuman = index == 0,
                jobName = jobName
            )
        }
        session.players = sanitized.toMutableList()
        session.actions.clear()
        session.votes.clear()
        session.prosConsVotes.clear()
        success("참가자와 직업 설정을 저장했습니다.")
    }

    fun start(token: String?): LaboratoryOperationResult = operate(token) { session ->
        if (session.phase != LaboratoryPhase.SETUP) return@operate failure("이미 실험실 게임이 시작되었습니다.")
        if (session.players.any { it.jobName == null }) return@operate failure("모든 플레이어의 직업을 선택해 주세요.")
        session.phase = LaboratoryPhase.NIGHT
        session.dayCount = 1
        session.players.forEach { it.isAlive = true }
        clearPhaseInputs(session)
        record(session, "실험 시작", "직업을 직접 배정한 ${session.players.size}인 실험실 게임을 시작했습니다.")
        success("1일차 밤을 시작했습니다.")
    }

    fun submitAction(
        token: String?,
        actorId: String,
        abilityName: String,
        targetId: String?,
        selectedJobName: String?
    ): LaboratoryOperationResult = operate(token) { session ->
        if (session.phase == LaboratoryPhase.SETUP) return@operate failure("먼저 실험실 게임을 시작해 주세요.")
        val actor = session.players.firstOrNull { it.id == actorId } ?: return@operate failure("행동할 플레이어를 찾을 수 없습니다.")
        if (!actor.isAlive) return@operate failure("사망한 플레이어는 행동할 수 없습니다.")
        val job = catalogProvider().firstOrNull { it.name == actor.jobName } ?: return@operate failure("직업 정보를 찾을 수 없습니다.")
        val ability = job.abilities.firstOrNull { it.name == abilityName && it.isUsableDuring(session.phase) }
            ?: return@operate failure("현재 단계에 사용할 수 없는 능력입니다.")
        val target = targetId?.let { id -> session.players.firstOrNull { it.id == id } }
        if (targetId != null && target == null) return@operate failure("능력 대상을 찾을 수 없습니다.")
        if (ability.requiresTarget && target == null) return@operate failure("능력 대상을 선택해 주세요.")
        val selectedJob = selectedJobName?.trim()?.takeIf(String::isNotEmpty)
        if (ability.requiresJobSelection && selectedJob == null) return@operate failure("직업을 선택해 주세요.")
        if (selectedJob != null && catalogProvider().none { it.name == selectedJob }) {
            return@operate failure("존재하지 않는 직업을 선택했습니다.")
        }
        session.actions[actor.id] = LaboratoryAction(actor.id, ability.name, target?.id, selectedJob)
        val detail = buildString {
            append("${actor.name}: ${ability.name}")
            target?.let { append(" → ${it.name}") }
            selectedJob?.let { append(" [$it]") }
        }
        record(session, "능력 결정", detail)
        success("${actor.name}의 능력 행동을 저장했습니다.")
    }

    fun castMainVote(token: String?, voterId: String, targetId: String?): LaboratoryOperationResult = operate(token) { session ->
        if (session.phase != LaboratoryPhase.MAIN_VOTE) return@operate failure("현재는 본투표 단계가 아닙니다.")
        val voter = livingPlayer(session, voterId) ?: return@operate failure("투표할 수 있는 플레이어를 찾을 수 없습니다.")
        val target = targetId?.let { livingPlayer(session, it) }
        if (targetId != null && target == null) return@operate failure("유효한 투표 대상을 찾을 수 없습니다.")
        session.votes[voter.id] = target?.id
        record(session, "투표 결정", "${voter.name} → ${target?.name ?: "기권"}")
        success("${voter.name}의 투표를 저장했습니다.")
    }

    fun castProsConsVote(token: String?, voterId: String, isPros: Boolean): LaboratoryOperationResult = operate(token) { session ->
        if (session.phase != LaboratoryPhase.PROS_CONS) return@operate failure("현재는 찬반 투표 단계가 아닙니다.")
        val voter = livingPlayer(session, voterId) ?: return@operate failure("투표할 수 있는 플레이어를 찾을 수 없습니다.")
        session.prosConsVotes[voter.id] = isPros
        record(session, "찬반 결정", "${voter.name}: ${if (isPros) "찬성" else "반대"}")
        success("${voter.name}의 찬반 선택을 저장했습니다.")
    }

    fun setPlayerAlive(token: String?, playerId: String, isAlive: Boolean): LaboratoryOperationResult = operate(token) { session ->
        if (session.phase == LaboratoryPhase.SETUP) return@operate failure("게임을 시작한 뒤 생존 상태를 조정할 수 있습니다.")
        val player = session.players.firstOrNull { it.id == playerId } ?: return@operate failure("플레이어를 찾을 수 없습니다.")
        player.isAlive = isAlive
        if (!isAlive) {
            session.actions.remove(player.id)
            session.votes.remove(player.id)
            session.prosConsVotes.remove(player.id)
        }
        record(session, "상태 수동 조정", "${player.name}: ${if (isAlive) "생존" else "사망"}")
        success("${player.name}의 상태를 변경했습니다.")
    }

    fun advance(token: String?): LaboratoryOperationResult = operate(token) { session ->
        when (session.phase) {
            LaboratoryPhase.SETUP -> failure("먼저 실험실 게임을 시작해 주세요.")
            LaboratoryPhase.NIGHT -> {
                resolveNight(session)
                session.phase = LaboratoryPhase.DAY
                clearPhaseInputs(session)
                record(session, "낮 시작", "${session.dayCount}일차 낮입니다.")
                success("${session.dayCount}일차 낮으로 진행했습니다.")
            }
            LaboratoryPhase.DAY -> {
                session.phase = LaboratoryPhase.MAIN_VOTE
                clearPhaseInputs(session)
                record(session, "본투표 시작", "생존 플레이어별 투표 대상을 지정하세요.")
                success("본투표 단계로 진행했습니다.")
            }
            LaboratoryPhase.MAIN_VOTE -> resolveMainVote(session)
            LaboratoryPhase.PROS_CONS -> resolveProsConsVote(session)
        }
    }

    fun reset(token: String?): LaboratoryOperationResult = operate(token) { session ->
        session.phase = LaboratoryPhase.SETUP
        session.dayCount = 0
        session.players.forEach { it.isAlive = true }
        session.events.clear()
        session.nextSequence = 1
        clearPhaseInputs(session)
        success("실험실을 설정 단계로 초기화했습니다.")
    }

    private fun resolveNight(session: LaboratorySession) {
        val playersById = session.players.associateBy { it.id }
        val protectedIds = session.actions.values
            .filter { it.abilityName == "치료" }
            .mapNotNull { it.targetId }
            .toSet()
        session.actions.values
            .filter { it.abilityName == "소생" }
            .mapNotNull { it.targetId }
            .mapNotNull(playersById::get)
            .forEach { target ->
                target.isAlive = true
                record(session, "밤 결과", "${target.name}님이 소생 대상으로 지정되어 생존 상태가 되었습니다.")
            }
        val executionTarget = session.actions.values.lastOrNull { it.abilityName == "처형" }
            ?.targetId
            ?.let(playersById::get)
        if (executionTarget != null) {
            if (executionTarget.id in protectedIds) {
                record(session, "밤 결과", "${executionTarget.name}님은 치료 대상으로 지정되어 처형을 피했습니다.")
            } else {
                executionTarget.isAlive = false
                record(session, "밤 결과", "${executionTarget.name}님이 처형 대상으로 지정되어 사망했습니다.")
            }
        }
        val otherActions = session.actions.values.count { it.abilityName !in CORE_RESOLVED_ABILITIES }
        if (otherActions > 0) {
            record(session, "능력 기록", "기타 능력 ${otherActions}건은 결정 기록으로 보존되었습니다. 결과는 생존 상태 버튼으로 직접 조정할 수 있습니다.")
        }
    }

    private fun resolveMainVote(session: LaboratorySession): LaboratoryOperationResult {
        val counts = session.votes.values.filterNotNull().groupingBy { it }.eachCount()
        val maxVotes = counts.values.maxOrNull() ?: 0
        val topIds = counts.filterValues { it == maxVotes }.keys
        val target = if (maxVotes > 0 && topIds.size == 1) {
            session.players.firstOrNull { it.id == topIds.first() && it.isAlive }
        } else null
        if (target == null) {
            record(session, "본투표 결과", if (maxVotes == 0) "유효표가 없습니다." else "동표로 최후 변론 대상이 없습니다.")
            beginNextNight(session)
            return success("처형 후보 없이 다음 날 밤으로 진행했습니다.")
        }
        session.defenseTargetId = target.id
        session.phase = LaboratoryPhase.PROS_CONS
        session.actions.clear()
        session.prosConsVotes.clear()
        record(session, "최후 변론", "${target.name}님이 찬반 투표 대상입니다.")
        return success("${target.name}님에 대한 찬반 투표를 시작했습니다.")
    }

    private fun resolveProsConsVote(session: LaboratorySession): LaboratoryOperationResult {
        val target = session.defenseTargetId?.let { id -> session.players.firstOrNull { it.id == id } }
            ?: return failure("찬반 투표 대상을 찾을 수 없습니다.")
        val pros = session.prosConsVotes.values.count { it }
        val cons = session.prosConsVotes.size - pros
        if (pros > cons) {
            target.isAlive = false
            record(session, "찬반 투표 결과", "찬성 $pros / 반대 $cons — ${target.name}님이 사망했습니다.")
        } else {
            record(session, "찬반 투표 결과", "찬성 $pros / 반대 $cons — ${target.name}님의 처형이 부결되었습니다.")
        }
        beginNextNight(session)
        return success("찬반 투표를 처리하고 다음 날 밤으로 진행했습니다.")
    }

    private fun beginNextNight(session: LaboratorySession) {
        session.dayCount += 1
        session.phase = LaboratoryPhase.NIGHT
        clearPhaseInputs(session)
        record(session, "밤 시작", "${session.dayCount}일차 밤입니다.")
    }

    private fun clearPhaseInputs(session: LaboratorySession) {
        session.actions.clear()
        session.votes.clear()
        session.prosConsVotes.clear()
        session.defenseTargetId = null
    }

    private fun record(session: LaboratorySession, title: String, body: String) {
        session.events += LaboratoryEvent(
            sequence = session.nextSequence++,
            dayCount = session.dayCount,
            phase = session.phase,
            title = title,
            body = body
        )
        if (session.events.size > MAX_EVENTS) session.events.removeAt(0)
    }

    private fun snapshot(session: LaboratorySession): LaboratorySnapshot {
        val jobs = catalogProvider().sortedBy(LaboratoryJobDefinition::name)
        val jobsByName = jobs.associateBy(LaboratoryJobDefinition::name)
        val playersById = session.players.associateBy { it.id }
        return LaboratorySnapshot(
            phase = session.phase,
            dayCount = session.dayCount,
            players = session.players.map { player ->
                val job = player.jobName?.let(jobsByName::get)
                LaboratoryPlayerSnapshot(
                    id = player.id,
                    name = player.name,
                    isHuman = player.isHuman,
                    isAlive = player.isAlive,
                    jobName = player.jobName,
                    jobImage = job?.image,
                    abilities = job?.abilities.orEmpty().filter { it.isUsableDuring(session.phase) }
                )
            },
            jobs = jobs,
            actions = session.actions.values.map { action ->
                val actor = playersById.getValue(action.actorId)
                val target = action.targetId?.let(playersById::get)
                LaboratoryActionSnapshot(
                    actorId = actor.id,
                    actorName = actor.name,
                    abilityName = action.abilityName,
                    targetId = target?.id,
                    targetName = target?.name,
                    selectedJobName = action.selectedJobName
                )
            },
            votes = session.votes.toMap(),
            prosConsVotes = session.prosConsVotes.toMap(),
            defenseTargetId = session.defenseTargetId,
            events = session.events.map { event ->
                LaboratoryEventSnapshot(event.sequence, event.dayCount, event.phase, event.title, event.body)
            }
        )
    }

    private fun livingPlayer(session: LaboratorySession, playerId: String): LaboratoryPlayer? =
        session.players.firstOrNull { it.id == playerId && it.isAlive }

    private fun LaboratoryAbilityDefinition.isUsableDuring(currentPhase: LaboratoryPhase): Boolean =
        phase == currentPhase || (phase == LaboratoryPhase.MAIN_VOTE && currentPhase == LaboratoryPhase.PROS_CONS)

    private fun session(token: String?): LaboratorySession? {
        val current = token?.takeIf(String::isNotBlank)?.let(sessions::get) ?: return null
        if (nowMillis() - current.lastAccessMillis > SESSION_LIFETIME_MILLIS) {
            sessions.remove(current.token)
            return null
        }
        current.lastAccessMillis = nowMillis()
        return current
    }

    private fun operate(
        token: String?,
        operation: (LaboratorySession) -> LaboratoryOperationResult
    ): LaboratoryOperationResult {
        val session = session(token) ?: return failure("실험실 세션이 없거나 만료되었습니다.")
        return synchronized(session) {
            operation(session).let { result ->
                if (result.snapshot == null) result.copy(snapshot = snapshot(session)) else result
            }
        }
    }

    private fun pruneExpiredSessions() {
        val threshold = nowMillis() - SESSION_LIFETIME_MILLIS
        sessions.entries.removeIf { it.value.lastAccessMillis < threshold }
        if (sessions.size <= MAX_SESSIONS) return
        sessions.values.sortedBy { it.lastAccessMillis }
            .take(sessions.size - MAX_SESSIONS)
            .forEach { sessions.remove(it.token) }
    }

    private fun success(message: String) = LaboratoryOperationResult(true, message)
    private fun failure(message: String) = LaboratoryOperationResult(false, message)

    companion object {
        private const val HUMAN_PLAYER_ID = "human"
        private const val MIN_PLAYERS = 4
        private const val MAX_PLAYERS = 16
        private const val MAX_PLAYER_NAME_LENGTH = 24
        private const val MAX_EVENTS = 500
        private const val MAX_SESSIONS = 100
        private const val SESSION_LIFETIME_MILLIS = 8 * 60 * 60 * 1000L
        private val CORE_RESOLVED_ABILITIES = setOf("처형", "치료", "소생")

        private fun defaultPlayers(): MutableList<LaboratoryPlayer> = MutableList(6) { index ->
            LaboratoryPlayer(
                id = if (index == 0) HUMAN_PLAYER_ID else "bot-$index",
                name = if (index == 0) "나" else "봇 $index",
                isHuman = index == 0,
                jobName = null
            )
        }
    }
}

internal object LaboratoryJobCatalog {
    @Synchronized
    fun definitions(): List<LaboratoryJobDefinition> {
        if (JobManager.getAll().isEmpty()) JobManager.registerAll()
        return JobManager.getAll().map { job ->
            LaboratoryJobDefinition(
                name = job.name,
                description = job.description,
                image = job.jobImage,
                isEvil = job is Evil,
                abilities = (job.abilities + job.extraAbilities)
                    .filterIsInstance<ActiveAbility>()
                    .map { ability ->
                        LaboratoryAbilityDefinition(
                            name = ability.name,
                            description = ability.description,
                            image = ability.image,
                            phase = ability.usablePhase.toLaboratoryPhase(),
                            requiresTarget = ability.name != "조회",
                            requiresJobSelection = ability.name in setOf("조회", "청부")
                        )
                    }
                    .distinctBy { it.name to it.phase }
            )
        }
    }

    private fun GamePhase.toLaboratoryPhase(): LaboratoryPhase = when (this) {
        GamePhase.NIGHT -> LaboratoryPhase.NIGHT
        GamePhase.DAY, GamePhase.DAWN -> LaboratoryPhase.DAY
        GamePhase.VOTE -> LaboratoryPhase.MAIN_VOTE
        GamePhase.END -> LaboratoryPhase.DAY
    }
}
