package org.beobma.mafia42discordproject.game

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.createTextChannel
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.channel.edit
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.rest.builder.channel.addMemberOverwrite
import dev.kord.rest.builder.channel.addRoleOverwrite
import dev.kord.rest.builder.component.actionRow
import dev.kord.core.entity.Member
import dev.kord.core.entity.channel.DmChannel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.VoiceChannel
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.beobma.mafia42discordproject.discord.DiscordMessageManager
import org.beobma.mafia42discordproject.discord.InteractionErrorHandler
import org.beobma.mafia42discordproject.game.player.JobPreferenceManager
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.JobManager
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.AbilityManager
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.definition.list.Cabal
import org.beobma.mafia42discordproject.job.definition.list.CabalRole
import org.beobma.mafia42discordproject.job.definition.list.Couple
import org.beobma.mafia42discordproject.job.definition.list.CoupleRole
import org.beobma.mafia42discordproject.job.evil.Evil
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

object GameManager {
    private var currentGame: Game? = null
    private var currentGuild: GuildBehavior? = null
    private val gameLoopScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var gameLoopJob: kotlinx.coroutines.Job? = null

    private const val MIN_TEST_PLAYER_COUNT = 8
    private const val REQUIRED_MAFIA_COUNT = 2
    private const val REQUIRED_ASSISTANT_COUNT = 1
    private const val REQUIRED_DOCTOR_COUNT = 1
    private const val REQUIRED_POLICE_COUNT = 1
    private const val EXTRA_ABILITY_SELECTION_REPEAT_COUNT = 3
    private const val EXTRA_ABILITY_OPTIONS_PER_ROUND = 3

    private val policeJobNames = setOf("경찰", "요원")
    private val excludedVirtualPreferenceJobNames = setOf("시민", "악인")
    private val abilitySelectionSessions: MutableMap<Snowflake, AbilitySelectionSession> = ConcurrentHashMap()
    private val abilitySelectionSessionMutex = Mutex()
    private var abilitySelectionInitializationInProgress: Boolean = false
    private var abilitySelectionPendingUserIds: MutableSet<Snowflake> = mutableSetOf()

    private data class AssignmentPlayer(
        val memberId: Snowflake? = null,
        val name: String,
        val preferences: List<Job>,
        var assignedJob: Job? = null
    )

    private data class AssignmentTrace(
        val lines: MutableList<String> = mutableListOf()
    ) {
        fun add(message: String) {
            lines += message
        }
    }

    private data class AbilitySelectionSession(
        val playerJob: Job,
        val availablePool: MutableList<Ability>,
        val selected: MutableList<Ability> = mutableListOf(),
        var currentOptions: List<Ability> = emptyList(),
        var completedRounds: Int = 0
    )

    suspend fun start(event: GuildChatInputCommandInteractionCreateEvent) {
        val interaction = event.interaction
        val guild = interaction.getGuild()

        val game = Game(
            playerDatas = mutableListOf(),
            guild = guild,
        )

        game.start(event)
    }

    suspend fun start(event: MessageCreateEvent) {
        val guild = event.getGuildOrNull() ?: return

        val game = Game(
            playerDatas = mutableListOf(),
            guild = guild,
        )

        // 기존 로직 실행
        game.start(event)
    }

    private suspend fun Game.start(event: GuildChatInputCommandInteractionCreateEvent) {
        val interaction = event.interaction
        if (currentGame != null) {
            DiscordMessageManager.respondEphemeral(event, "이미 게임이 진행 중입니다.")
            return
        }

        val deferredResponse = interaction.deferPublicResponse()

        val guild = interaction.guild
        val commandSender = interaction.user
        val voiceChannelId = commandSender.getVoiceStateOrNull()?.channelId ?: run {
            deferredResponse.respond {
                content = "현재 음성채널에 들어가 있지 않습니다."
            }
            return
        }
        val voiceChannel = guild.getChannelOfOrNull<VoiceChannel>(voiceChannelId) ?: run {
            deferredResponse.respond {
                content = "음성채널 정보를 가져오지 못했습니다."
            }
            return
        }

        val membersInSameVoice = guild.members
            .filter { guildMember ->
                guildMember.getVoiceStateOrNull()?.channelId == voiceChannelId
            }
            .toList()

        val membersWithoutPreference = membersInSameVoice.filter { member ->
            JobPreferenceManager.get(member.id.value).isNullOrEmpty()
        }

        if (membersWithoutPreference.isNotEmpty()) {
            deferredResponse.respond {
                content = buildString {
                    appendLine("아래 플레이어가 선호 직업을 설정하지 않아 게임 시작이 취소되었습니다.")
                    appendLine("`/jobpreference` 명령어로 선호 직업 7개를 먼저 설정해 주세요.")
                    append(DiscordMessageManager.mentions(membersWithoutPreference))
                }
            }
            return
        }

        currentGame = this
        currentGuild = guild
        GameLoopManager.resetTimeThreadState()
        this.replacePlayers(membersInSameVoice.map(::PlayerData).toMutableList())

        val assignmentPlayers = buildAssignmentPlayers(membersInSameVoice)
        assignJobs(assignmentPlayers)
        this.applyAssignedJobs(assignmentPlayers)
        initializeExtraAbilitySelectionForPlayers(assignmentPlayers)
        tryStartGameLoopWhenAbilitySelectionCompleted(guild)

        setupGameChannels(this)

        deferredResponse.respond {
            content = buildString {
                appendLine("현재 음성채널: ${voiceChannel.mention}")
                appendLine("인원 수: ${membersInSameVoice.size}")
                appendLine()
                append(DiscordMessageManager.mentions(membersInSameVoice))
            }
        }
    }

    private suspend fun Game.start(event: MessageCreateEvent) {
        if (currentGame != null) {
            event.message.channel.createMessage("이미 게임이 진행 중입니다.")
            return
        }

        val guild = event.getGuildOrNull() ?: return
        val commandSender = event.member ?: return
        val voiceChannelId = commandSender.getVoiceStateOrNull()?.channelId ?: run {
            event.message.channel.createMessage("현재 음성채널에 들어가 있지 않습니다.")
            return
        }
        val voiceChannel = guild.getChannelOfOrNull<VoiceChannel>(voiceChannelId) ?: run {
            event.message.channel.createMessage("음성채널 정보를 가져오지 못했습니다.")
            return
        }

        val membersInSameVoice = guild.members
            .filter { guildMember ->
                guildMember.getVoiceStateOrNull()?.channelId == voiceChannelId
            }
            .toList()

        val membersWithoutPreference = membersInSameVoice.filter { member ->
            JobPreferenceManager.get(member.id.value).isNullOrEmpty()
        }

        if (membersWithoutPreference.isNotEmpty()) {
            event.message.channel.createMessage(
                buildString {
                    appendLine("아래 플레이어가 선호 직업을 설정하지 않아 게임 시작이 취소되었습니다.")
                    appendLine("`!jobpreference` 또는 `/jobpreference` 명령어로 선호 직업 7개를 먼저 설정해 주세요.")
                    append(DiscordMessageManager.mentions(membersWithoutPreference))
                }
            )
            return
        }

        currentGame = this
        currentGuild = guild
        GameLoopManager.resetTimeThreadState()
        this.replacePlayers(membersInSameVoice.map(::PlayerData).toMutableList())

        val assignmentPlayers = buildAssignmentPlayers(membersInSameVoice)
        assignJobs(assignmentPlayers)
        this.applyAssignedJobs(assignmentPlayers)
        initializeExtraAbilitySelectionForPlayers(assignmentPlayers)
        tryStartGameLoopWhenAbilitySelectionCompleted(guild)

        setupGameChannels(this)

        event.message.channel.createMessage(
            buildString {
                appendLine("인원 수: ${membersInSameVoice.size}")
                appendLine()
                append(DiscordMessageManager.mentions(membersInSameVoice))
            }
        )
    }

    private fun buildAssignmentPlayers(members: List<Member>): MutableList<AssignmentPlayer> {
        val players = members.map { member ->
            AssignmentPlayer(
                memberId = member.id,
                name = member.effectiveName,
                preferences = JobPreferenceManager.get(member.id.value).orEmpty()
            )
        }.toMutableList()

        val neededVirtualCount = (MIN_TEST_PLAYER_COUNT - players.size).coerceAtLeast(0)

        repeat(neededVirtualCount) { index ->
            players += AssignmentPlayer(
                name = "가상플레이어${index + 1}",
                preferences = generateVirtualPreferences()
            )
        }

        return players
    }

    private fun Game.applyAssignedJobs(players: List<AssignmentPlayer>) {
        val assignmentByMemberId = players
            .filter { it.memberId != null }
            .associateBy { it.memberId }

        playerDatas.forEach { playerData ->
            val assignment = assignmentByMemberId[playerData.member.id]
            val assignedJobName = assignment?.assignedJob?.name
            playerData.job = assignedJobName?.let(JobManager::createByName)
        }
        assignCabalSunMoonRoles()
        assignCoupleRoles()
    }

    private fun Game.assignCabalSunMoonRoles() {
        val cabalPlayers = playerDatas
            .filter { it.job is Cabal }
            .shuffled()

        if (cabalPlayers.size != 2) return

        val sunPlayer = cabalPlayers[0]
        val moonPlayer = cabalPlayers[1]
        val sunCabal = sunPlayer.job as? Cabal ?: return
        val moonCabal = moonPlayer.job as? Cabal ?: return

        sunCabal.role = CabalRole.SUN
        moonCabal.role = CabalRole.MOON

        sunCabal.pairedPlayerId = moonPlayer.member.id
        moonCabal.pairedPlayerId = sunPlayer.member.id
    }

    private fun Game.assignCoupleRoles() {
        val couplePlayers = playerDatas
            .filter { it.job is Couple }
            .shuffled()

        if (couplePlayers.size != 2) return

        val malePlayer = couplePlayers[0]
        val femalePlayer = couplePlayers[1]
        val maleCouple = malePlayer.job as? Couple ?: return
        val femaleCouple = femalePlayer.job as? Couple ?: return

        maleCouple.role = CoupleRole.MALE
        femaleCouple.role = CoupleRole.FEMALE

        maleCouple.pairedPlayerId = femalePlayer.member.id
        femaleCouple.pairedPlayerId = malePlayer.member.id
    }

    private fun generateVirtualPreferences(): List<Job> {
        val allJobs = JobManager.getAll()
        val policePool = allJobs.filter { it.name in policeJobNames }
        val assistantPool = allJobs.filter { it is Evil && it.name != "마피아" && it.name != "악인" }
        val specialPool = allJobs.filter {
            it.name !in policeJobNames &&
                it.name != "의사" &&
                it !is Evil &&
                it.name !in excludedVirtualPreferenceJobNames
        }

        val police = requireNotNull(policePool.randomOrNull()) {
            "가상 플레이어 선호 직업 구성을 위한 경찰 계열 직업이 없습니다."
        }
        val assistant = requireNotNull(assistantPool.randomOrNull()) {
            "가상 플레이어 선호 직업 구성을 위한 보조 계열 직업이 없습니다."
        }
        val specials = specialPool.shuffled().take(5)

        val fallbackPool = allJobs.filterNot {
            it.name in excludedVirtualPreferenceJobNames ||
                it.name in policeJobNames ||
                it is Evil
        }.shuffled()
        return buildList {
            add(assistant)
            add(police)
            addAll(specials)
            fallbackPool.forEach { job ->
                if (size >= 7) return@forEach
                if (any { it.name == job.name }) return@forEach
                add(job)
            }
        }.take(7)
    }

    private fun assignJobs(players: MutableList<AssignmentPlayer>): AssignmentTrace {
        val trace = AssignmentTrace()
        val mafia = JobManager.findByName("마피아") ?: run {
            trace.add("[오류] 마피아 직업 정의를 찾지 못했습니다.")
            return trace
        }
        val assistantPool = JobManager.getAll().filter { it is Evil && it.name != mafia.name }
        if (assistantPool.isEmpty()) {
            trace.add("[오류] 보조 계열 직업 정의를 찾지 못했습니다.")
            return trace
        }
        val doctor = JobManager.findByName("의사") ?: run {
            trace.add("[오류] 의사 직업 정의를 찾지 못했습니다.")
            return trace
        }
        val policePool = JobManager.getAll().filter { it.name in policeJobNames }
        if (policePool.isEmpty()) {
            trace.add("[오류] 경찰 계열 직업 정의를 찾지 못했습니다.")
            return trace
        }
        val selectedPoliceJob = pickPoliceJobByPreference(players, policePool, trace)
        val selectedAssistantJob = pickAssistantJobByPreference(players, assistantPool, trace)

        trace.add("[1단계] 참여 인원: ${players.size}명")
        trace.add(
            "[1단계] 고정 배정 직업: 마피아 ${REQUIRED_MAFIA_COUNT}명, 보조계열 ${REQUIRED_ASSISTANT_COUNT}명, 의사 ${REQUIRED_DOCTOR_COUNT}명, 경찰계열 ${REQUIRED_POLICE_COUNT}명"
        )

        val requiredFixedCount = REQUIRED_MAFIA_COUNT + REQUIRED_ASSISTANT_COUNT + REQUIRED_DOCTOR_COUNT + REQUIRED_POLICE_COUNT
        val slotCountForNonFixed = players.size - requiredFixedCount
        if (slotCountForNonFixed <= 0) {
            trace.add("[2단계] 고정 직업만 배정 가능한 인원 수라서 바로 랜덤 배정 진행")
            assignRequiredJobs(players, mafia, selectedAssistantJob, doctor, selectedPoliceJob, trace)
            return trace
        }

        assignNonFixedJobsByPreference(
            players = players,
            slotCount = slotCountForNonFixed,
            trace = trace
        )
        assignRequiredJobs(players, mafia, selectedAssistantJob, doctor, selectedPoliceJob, trace)
        return trace
    }

    private fun pickPoliceJobByPreference(
        players: List<AssignmentPlayer>,
        policePool: List<Job>,
        trace: AssignmentTrace
    ): Job {
        val weightedPoliceJobs = policePool.map { policeJob ->
            val weight = players.sumOf { player ->
                player.preferences.count { preferred -> preferred.name == policeJob.name }
            }
            policeJob to weight
        }
        val weightSummary = weightedPoliceJobs.joinToString(", ") { (job, weight) -> "${job.name}($weight)" }
        trace.add("[1단계] 경찰계열 후보 가중치: $weightSummary")

        val picked = pickByWeight(weightedPoliceJobs) ?: policePool.random()
        trace.add("[1단계] 경찰계열 고정 직업 선택: ${picked.name}")
        return picked
    }

    private fun pickAssistantJobByPreference(
        players: List<AssignmentPlayer>,
        assistantPool: List<Job>,
        trace: AssignmentTrace
    ): Job {
        val weightedAssistantJobs = assistantPool.map { assistantJob ->
            val weight = players.sumOf { player ->
                player.preferences.count { preferred -> preferred.name == assistantJob.name }
            }
            assistantJob to weight
        }
        val weightSummary = weightedAssistantJobs.joinToString(", ") { (job, weight) -> "${job.name}($weight)" }
        trace.add("[1단계] 보조계열 후보 가중치: $weightSummary")

        val picked = pickByWeight(weightedAssistantJobs) ?: assistantPool.random()
        trace.add("[1단계] 보조계열 고정 직업 선택: ${picked.name}")
        return picked
    }

    private fun assignNonFixedJobsByPreference(
        players: MutableList<AssignmentPlayer>,
        slotCount: Int,
        trace: AssignmentTrace
    ) {
        val excludedJobNames = setOf("마피아", "의사") + policeJobNames
        val allCandidates = JobManager.getAll().filter { candidate ->
            candidate !is Evil && candidate.name !in excludedJobNames
        }
        val preferenceWeightByJob = mutableMapOf<Job, Int>()

        players.flatMap { it.preferences }.forEach { job ->
            if (job !is Evil && job.name !in excludedJobNames) {
                preferenceWeightByJob[job] = (preferenceWeightByJob[job] ?: 0) + 1
            }
        }

        val pickedNames = mutableSetOf<String>()
        var assignedSlots = 0

        fun slotsFor(job: Job): Int = if (job.name == "연인" || job.name == "비밀결사") 2 else 1

        fun isEligible(job: Job): Boolean {
            if (pickedNames.contains(job.name)) return false

            val requiredSlots = slotsFor(job)
            if (assignedSlots + requiredSlots > slotCount) return false

            val preferredUnassignedCount = players.count { player ->
                player.assignedJob == null && player.preferences.any { it.name == job.name }
            }
            return preferredUnassignedCount >= requiredSlots
        }

        trace.add("[2단계] 고정 직업 제외 슬롯 수: $slotCount")
        trace.add("[2단계] 후보 직업 수: ${allCandidates.size}개")
        val sortedWeightSummary = preferenceWeightByJob.entries
            .sortedWith(compareByDescending<Map.Entry<Job, Int>> { it.value }.thenBy { it.key.name })
            .joinToString(", ") { (job, weight) -> "${job.name}($weight)" }
        trace.add("[2단계] 전체 선호 직업 가중치: ${sortedWeightSummary.ifEmpty { "없음" }}")
        players.forEach { player ->
            val playerWeightSummary = player.preferences
                .joinToString(", ") { job -> "${job.name}(${preferenceWeightByJob[job] ?: 0})" }
            trace.add("[2단계] ${player.name} 선호 직업 가중치: $playerWeightSummary")
        }

        while (assignedSlots < slotCount) {
            val eligibleJobs = allCandidates.filter(::isEligible)
            if (eligibleJobs.isEmpty()) break

            val weightedJobs = eligibleJobs.map { it to (preferenceWeightByJob[it] ?: 0) }
            val picked = pickByWeight(weightedJobs)
                ?: eligibleJobs.random()
            val pickedWeight = preferenceWeightByJob[picked] ?: 0
            val requiredCount = slotsFor(picked)
            val candidates = players
                .filter { it.assignedJob == null && it.preferences.any { preferred -> preferred.name == picked.name } }
                .shuffled()
                .take(requiredCount)

            if (candidates.size < requiredCount) {
                trace.add("[2단계] ${picked.name}: 선호 인원 부족으로 건너뜀")
                pickedNames += picked.name
                continue
            }

            candidates.forEach { player ->
                player.assignedJob = picked
            }
            pickedNames += picked.name
            assignedSlots += requiredCount
            trace.add(
                "[2단계] 선택/배정 직업: ${picked.name} (가중치 $pickedWeight / 필요 슬롯 $requiredCount / 누적 $assignedSlots) -> ${candidates.joinToString { it.name }}"
            )
        }

        if (assignedSlots < slotCount) {
            trace.add("[2단계] 경고: 선호 기반 비고정 배정이 부족하여 보완 배정을 시도합니다. (현재 $assignedSlots/$slotCount)")
            fillRemainingNonFixedSlots(players, allCandidates, pickedNames, slotCount, assignedSlots, trace)
        }
    }

    private fun pickByWeight(weightedJobs: List<Pair<Job, Int>>): Job? {
        val totalWeight = weightedJobs.sumOf { (_, weight) -> weight.coerceAtLeast(0) }
        if (totalWeight <= 0) return null

        var point = Random.nextInt(totalWeight)
        weightedJobs.forEach { (job, rawWeight) ->
            val weight = rawWeight.coerceAtLeast(0)
            if (weight == 0) return@forEach
            if (point < weight) {
                return job
            }
            point -= weight
        }
        return null
    }

    private fun fillRemainingNonFixedSlots(
        players: MutableList<AssignmentPlayer>,
        allCandidates: List<Job>,
        pickedNames: MutableSet<String>,
        targetSlotCount: Int,
        initialAssignedSlots: Int,
        trace: AssignmentTrace
    ) {
        var assignedSlots = initialAssignedSlots
        fun slotsFor(job: Job): Int = if (job.name == "연인" || job.name == "비밀결사") 2 else 1

        while (assignedSlots < targetSlotCount) {
            val remainingSlots = targetSlotCount - assignedSlots
            val selectable = allCandidates.filter { job ->
                job.name !in pickedNames && slotsFor(job) <= remainingSlots
            }
            if (selectable.isEmpty()) break

            val picked = selectable.random()
            val requiredCount = slotsFor(picked)
            val preferCandidates = players
                .filter { it.assignedJob == null && it.preferences.any { preferred -> preferred.name == picked.name } }
                .shuffled()
                .take(requiredCount)

            val finalCandidates = if (preferCandidates.size == requiredCount) {
                preferCandidates
            } else {
                players.filter { it.assignedJob == null }.shuffled().take(requiredCount)
            }

            if (finalCandidates.size < requiredCount) break

            finalCandidates.forEach { player ->
                player.assignedJob = picked
            }
            pickedNames += picked.name
            assignedSlots += requiredCount
            trace.add(
                "[2단계-보완] ${picked.name}: ${finalCandidates.joinToString { it.name }} 배정 (누적 $assignedSlots/$targetSlotCount)"
            )
        }
    }

    private fun assignRequiredJobs(
        players: MutableList<AssignmentPlayer>,
        mafia: Job,
        assistant: Job,
        doctor: Job,
        policeJob: Job,
        trace: AssignmentTrace
    ) {
        trace.add("[3단계] 고정 직업/잔여 인원 배정 시작")
        val unassigned = players.filter { it.assignedJob == null }.shuffled().toMutableList()

        repeat(REQUIRED_MAFIA_COUNT) {
            if (unassigned.isEmpty()) return@repeat
            val player = unassigned.removeFirst()
            player.assignedJob = mafia
            trace.add("[3단계] 마피아 배정: ${player.name}")
        }

        repeat(REQUIRED_ASSISTANT_COUNT) {
            if (unassigned.isEmpty()) return@repeat
            val preferred = unassigned.firstOrNull { candidate ->
                candidate.preferences.any { it.name == assistant.name }
            }
            val player = preferred ?: unassigned.first()
            unassigned.remove(player)
            player.assignedJob = assistant
            trace.add("[3단계] 보조계열 배정: ${player.name} -> ${assistant.name}")
        }

        repeat(REQUIRED_DOCTOR_COUNT) {
            if (unassigned.isEmpty()) return@repeat
            val player = unassigned.removeFirst()
            player.assignedJob = doctor
            trace.add("[3단계] 의사 배정: ${player.name}")
        }

        repeat(REQUIRED_POLICE_COUNT) {
            if (unassigned.isEmpty()) return@repeat
            val preferred = unassigned.firstOrNull { candidate ->
                candidate.preferences.any { it.name == policeJob.name }
            }
            val player = preferred ?: unassigned.first()
            unassigned.remove(player)
            player.assignedJob = policeJob
            trace.add("[3단계] 경찰계열 배정: ${player.name} -> ${policeJob.name}")
        }

        unassigned.forEach { player ->
            player.assignedJob = JobManager.findByName("시민") ?: doctor
            trace.add("[3단계] 안전 배정: ${player.name} -> ${player.assignedJob?.name}")
        }
        trace.add("[3단계] 배정 완료")
    }

    private suspend fun Game.initializeExtraAbilitySelectionForPlayers(players: List<AssignmentPlayer>) {
        val preparedSessions = mutableMapOf<Snowflake, AbilitySelectionSession>()

        playerDatas.forEach { player ->
            val job = player.job ?: return@forEach
            val pool = AbilityManager.getAvailableExtraAbilitiesFor(job)
                .distinctBy(Ability::name)
                .shuffled()
                .toMutableList()

            val session = AbilitySelectionSession(
                playerJob = job,
                availablePool = pool
            )
            session.currentOptions = drawAbilityOptions(session)
            if (session.currentOptions.isNotEmpty()) {
                preparedSessions[player.member.id] = session
            }
        }

        abilitySelectionSessionMutex.withLock {
            abilitySelectionInitializationInProgress = true
            abilitySelectionSessions.clear()
            abilitySelectionSessions.putAll(preparedSessions)
            abilitySelectionPendingUserIds = preparedSessions.keys.toMutableSet()
        }

        coroutineScope {
            playerDatas.forEach { player ->
                launch {
                    val job = player.job ?: return@launch
                    val session = preparedSessions[player.member.id]
                    runCatching {
                        val dmChannel = player.member.getDmChannel()
                        val ownedAbilityMessage = buildString {
                            job.jobImage
                                ?.takeIf { it.isNotBlank() }
                                ?.let { appendLine(it) }
                            appendAbilityImages(this, job.abilities)
                        }.trim()
                        if (ownedAbilityMessage.isNotBlank()) {
                            dmChannel.createMessage(ownedAbilityMessage)
                        }

                        if (session != null) {
                            sendAbilitySelectionPrompt(dmChannel, player.member.id, session)
                        } else {
                            dmChannel.createMessage("ℹ️ 선택 가능한 부가 능력이 없어 능력 선택 단계를 건너뜁니다.")
                        }
                    }.onFailure { error ->
                        println("⚠️ ${player.member.effectiveName} DM 전송 실패: ${error.message}")
                    }
                }
            }
        }

        abilitySelectionSessionMutex.withLock {
            abilitySelectionInitializationInProgress = false
        }

        assignVirtualPlayerExtraAbilities(players)
    }

    private fun appendAbilityImages(
        builder: StringBuilder,
        abilities: List<Ability>
    ): Int {
        var count = 0
        abilities
            .map(Ability::image)
            .filter { it.isNotBlank() }
            .forEach { imageUrl ->
                builder.appendLine(imageUrl)
                count += 1
            }
        return count
    }

    private fun assignVirtualPlayerExtraAbilities(players: List<AssignmentPlayer>) {
        val virtualPlayers = players.filter { it.memberId == null }
        virtualPlayers.forEach { virtualPlayer ->
            val assignedJob = virtualPlayer.assignedJob ?: return@forEach
            val pool = AbilityManager.getAvailableExtraAbilitiesFor(assignedJob)
                .distinctBy(Ability::name)
                .shuffled()
                .toMutableList()

            val selected = mutableListOf<Ability>()
            repeat(EXTRA_ABILITY_SELECTION_REPEAT_COUNT) {
                if (pool.isEmpty()) return@repeat
                selected += pool.removeAt(0)
            }

            if (selected.isNotEmpty()) {
                println(
                    "🎲 ${virtualPlayer.name}(${assignedJob.name}) 부가 능력 랜덤 선택 완료: ${
                        selected.joinToString(", ") { it.name }
                    }"
                )
            }
        }
    }

    suspend fun selectExtraAbility(userId: Snowflake, pickNumber: Int): String {
        var shouldTryStartGameLoop = false
        val resultMessage = abilitySelectionSessionMutex.withLock {
            val session = abilitySelectionSessions[userId]
                ?: restoreAbilitySelectionSessionLocked(userId)
                ?: return@withLock "현재 부가 능력 선택 단계가 아니거나 이미 선택이 완료되었습니다."
            val playerJob = session.playerJob

            if (pickNumber !in 1..EXTRA_ABILITY_OPTIONS_PER_ROUND) {
                return@withLock "선택 번호는 1~${EXTRA_ABILITY_OPTIONS_PER_ROUND} 사이여야 합니다."
            }

            if (session.completedRounds >= EXTRA_ABILITY_SELECTION_REPEAT_COUNT || session.currentOptions.isEmpty()) {
                return@withLock "이미 부가 능력 선택이 완료되었습니다."
            }

            if (pickNumber > session.currentOptions.size) {
                return@withLock "현재 라운드에서 선택 가능한 번호가 아닙니다. 제시된 번호 중에서 선택해 주세요."
            }

            val pickedAbility = session.currentOptions[pickNumber - 1]
            val selectedUniqueAbility = pickedAbility as? JobUniqueAbility
            if (selectedUniqueAbility != null && playerJob.abilities.none { it.name == selectedUniqueAbility.name }) {
                playerJob.abilities.add(selectedUniqueAbility)
            }
            if (playerJob.extraAbilities.none { it.name == pickedAbility.name }) {
                playerJob.extraAbilities.add(pickedAbility)
            }
            session.selected += pickedAbility
            session.completedRounds += 1

            if (session.completedRounds >= EXTRA_ABILITY_SELECTION_REPEAT_COUNT) {
                abilitySelectionSessions.remove(userId)
                abilitySelectionPendingUserIds.remove(userId)
                shouldTryStartGameLoop = true
                return@withLock buildString {
                    appendLine("✅ ${pickedAbility.name} 능력을 선택했습니다.")
                    appendLine("부가 능력 선택이 모두 완료되었습니다.")
                    append("최종 선택 능력: ${session.selected.joinToString(", ") { it.name }}")
                }
            }

            session.currentOptions = drawAbilityOptions(session)
            if (session.currentOptions.isEmpty()) {
                abilitySelectionSessions.remove(userId)
                abilitySelectionPendingUserIds.remove(userId)
                shouldTryStartGameLoop = true
                return@withLock buildString {
                    appendLine("**${pickedAbility.name}** 능력을 선택했습니다.")
                    appendLine("추가로 제시할 수 있는 능력이 없어 선택 단계를 종료합니다.")
                    append("현재 선택 능력: ${session.selected.joinToString(", ") { it.name }}")
                }
            }

            "**${pickedAbility.name}** 능력을 선택했습니다. 다음 능력을 선택해 주세요."
        }

        if (shouldTryStartGameLoop) {
            currentGuild?.let { guild ->
                runCatching {
                    tryStartGameLoopWhenAbilitySelectionCompleted(guild)
                }.onFailure { error ->
                    println("⚠️ 능력 선택 종료 후 게임 루프 시작 실패: ${error.message}")
                }
            }
        }

        return resultMessage
    }

    private fun restoreAbilitySelectionSessionLocked(userId: Snowflake): AbilitySelectionSession? {
        val game = currentGame ?: return null
        if (game.isRunning) return null

        val player = game.getPlayer(userId) ?: return null
        val playerJob = player.job ?: return null
        val alreadySelectedNames = playerJob.extraAbilities.map(Ability::name).toSet()
        val completedRounds = minOf(playerJob.extraAbilities.size, EXTRA_ABILITY_SELECTION_REPEAT_COUNT)
        if (completedRounds >= EXTRA_ABILITY_SELECTION_REPEAT_COUNT) return null

        val availablePool = AbilityManager.getAvailableExtraAbilitiesFor(playerJob)
            .distinctBy(Ability::name)
            .filterNot { ability -> ability.name in alreadySelectedNames }
            .toMutableList()
        if (availablePool.isEmpty()) return null

        val restoredSession = AbilitySelectionSession(
            playerJob = playerJob,
            availablePool = availablePool,
            selected = playerJob.extraAbilities.toMutableList(),
            completedRounds = completedRounds
        )
        restoredSession.currentOptions = drawAbilityOptions(restoredSession)
        if (restoredSession.currentOptions.isEmpty()) return null

        abilitySelectionSessions[userId] = restoredSession
        abilitySelectionPendingUserIds.add(userId)
        println("ℹ️ ${player.member.effectiveName}의 부가 능력 선택 세션을 복구했습니다.")
        return restoredSession
    }

    private fun drawAbilityOptions(session: AbilitySelectionSession): List<Ability> {
        if (session.availablePool.isEmpty()) return emptyList()

        val count = minOf(EXTRA_ABILITY_OPTIONS_PER_ROUND, session.availablePool.size)
        val options = session.availablePool.shuffled().take(count)
        val optionNames = options.map(Ability::name).toSet()
        session.availablePool.removeAll { ability -> ability.name in optionNames }
        return options
    }

    private fun buildAbilitySelectionGuideMessage(session1: AbilitySelectionSession, includeProgress: Boolean): String {
        return buildString {
            append("능력 중 하나를 선택하세요.")
        }
    }

    data class AbilityPickButtonPayload(
        val ownerUserId: Snowflake,
        val pickNumber: Int
    )

    fun parseAbilityPickButtonId(componentId: String): AbilityPickButtonPayload? {
        val prefix = "ability_pick_"
        if (!componentId.startsWith(prefix)) return null
        val payload = componentId.removePrefix(prefix)
        val parts = payload.split("_")
        if (parts.size != 2) return null

        val ownerId = parts[0].toULongOrNull()?.let(::Snowflake) ?: return null
        val pickNumber = parts[1].toIntOrNull()?.takeIf { it in 1..EXTRA_ABILITY_OPTIONS_PER_ROUND } ?: return null
        return AbilityPickButtonPayload(ownerUserId = ownerId, pickNumber = pickNumber)
    }

    fun abilityPickButtonId(userId: Snowflake, pickNumber: Int): String = "ability_pick_${userId.value}_$pickNumber"

    suspend fun getAbilitySelectionSession(userId: Snowflake): AbilitySelectionSnapshot? {
        val session = abilitySelectionSessionMutex.withLock {
            abilitySelectionSessions[userId]
        } ?: return null
        return AbilitySelectionSnapshot(
            guideMessage = buildAbilitySelectionGuideMessage(session, true),
            optionCount = session.currentOptions.size
        )
    }

    suspend fun sendCurrentAbilityOptionImages(userId: Snowflake): Boolean {
        return sendCurrentAbilitySelectionPrompt(userId)
    }

    suspend fun sendCurrentAbilityPickButtons(userId: Snowflake): Boolean {
        return sendCurrentAbilitySelectionPrompt(userId)
    }

    suspend fun sendCurrentAbilitySelectionPrompt(userId: Snowflake): Boolean {
        val game = currentGame ?: return false
        val player = game.getPlayer(userId) ?: return false
        val session = abilitySelectionSessionMutex.withLock {
            abilitySelectionSessions[userId]
                ?: restoreAbilitySelectionSessionLocked(userId)
        } ?: return false
        if (session.currentOptions.isEmpty()) return false

        return runCatching {
            val dmChannel = player.member.getDmChannel()
            sendAbilitySelectionPrompt(dmChannel, userId, session)
            true
        }.getOrElse { error ->
            println("⚠️ 현재 능력 선택 안내 DM 전송 실패(${player.member.effectiveName}): ${error.message}")
            false
        }
    }

    private suspend fun sendAbilitySelectionPrompt(
        dmChannel: DmChannel,
        userId: Snowflake,
        session: AbilitySelectionSession
    ) {
        val content = buildString {
            appendAbilityImages(this, session.currentOptions)
            if (isNotEmpty()) {
                appendLine()
            }
            append(buildAbilitySelectionGuideMessage(session, includeProgress = true))
        }.trim()

        dmChannel.createMessage {
            this.content = content
            actionRow {
                session.currentOptions.forEachIndexed { index, _ ->
                    interactionButton(ButtonStyle.Primary, abilityPickButtonId(userId, index + 1)) {
                        label = "${index + 1}번 선택"
                    }
                }
            }
        }
    }

    fun getCurrentGameFor(userId: Snowflake): Game? =
        currentGame?.takeIf { game -> game.getPlayer(userId) != null }

    data class AbilitySelectionSnapshot(
        val guideMessage: String,
        val optionCount: Int
    )

    fun isInCurrentGame(userId: Snowflake): Boolean =
        currentGame?.playerDatas?.any { it.member.id == userId } == true

    private suspend fun tryStartGameLoopWhenAbilitySelectionCompleted(guild: GuildBehavior) {
        val game = currentGame ?: return
        val canStart = abilitySelectionSessionMutex.withLock {
            !abilitySelectionInitializationInProgress && abilitySelectionPendingUserIds.isEmpty()
        }
        if (!canStart) return
        if (game.isRunning) return
        if (gameLoopJob?.isActive == true) return

        game.isRunning = true
        gameLoopJob = gameLoopScope.launch {
            GameLoopManager.runGameLoop(game)
        }
    }

    suspend fun stop(event: GuildChatInputCommandInteractionCreateEvent) {
        val gameToStop = currentGame

        if (gameToStop == null) {
            InteractionErrorHandler.runSafely("gamestop:no-game") {
                val deferred = event.interaction.deferEphemeralResponse()
                deferred.respond {
                    content = "진행 중인 게임이 없습니다."
                }
            }
            return
        }

        InteractionErrorHandler.runSafely("gamestop:announce-stop") {
            val deferred = event.interaction.deferPublicResponse()
            val mention = DiscordMessageManager.mention(event.interaction.user)
            deferred.respond {
                content = "${mention}이(가) 게임을 종료했습니다."
            }
        }

        stopGameState(gameToStop)
    }

    suspend fun stop(event: MessageCreateEvent) {
        val gameToStop = currentGame

        if (gameToStop == null) {
            event.message.channel.createMessage("진행 중인 게임이 없습니다.")
            return
        }

        val mention = event.message.author?.mention.orEmpty()
        event.message.channel.createMessage("${mention}이(가) 게임을 종료했습니다.")

        stopGameState(gameToStop)
    }

    private suspend fun stopGameState(gameToStop: Game) {
        safelyDeleteGameChannels(gameToStop)

        currentGame = null
        currentGuild = null
        GameLoopManager.resetTimeThreadState()
        abilitySelectionSessions.clear()
        gameLoopJob?.cancel()
        gameLoopJob = null
    }

    private suspend fun safelyDeleteGameChannels(game: Game) {
        game.deadChannel?.let { deadChannel ->
            runCatching {
                deadChannel.delete("게임 강제 종료로 인한 죽은자 채널 삭제")
            }.onFailure { exception ->
                println("[GameManager] 죽은자 채널 삭제 실패(이미 삭제되었거나 접근 불가): ${exception.message}")
            }
        }

        game.mafiaChannel?.let { mafiaChannel ->
            runCatching {
                mafiaChannel.delete("게임 강제 종료로 인한 마피아 채널 삭제")
            }.onFailure { exception ->
                println("[GameManager] 마피아 채널 삭제 실패(이미 삭제되었거나 접근 불가): ${exception.message}")
            }
        }

        game.coupleChannel?.let { coupleChannel ->
            runCatching {
                coupleChannel.delete("게임 강제 종료로 인한 연인 채널 삭제")
            }.onFailure { exception ->
                println("[GameManager] 연인 채널 삭제 실패(이미 삭제되었거나 접근 불가): ${exception.message}")
            }
        }

        game.mainChannel?.let { mainChannel ->
            runCatching {
                mainChannel.delete("게임 강제 종료로 인한 채널 삭제")
            }.onFailure { exception ->
                println("[GameManager] 메인 채널 삭제 실패(이미 삭제되었거나 접근 불가): ${exception.message}")
            }
        }

        game.mainChannel = null
        game.mafiaChannel = null
        game.coupleChannel = null
        game.deadChannel = null
    }

    suspend fun setupGameChannels(game: Game) {
        val guild = game.guild

        // 1. 메인 채널 설정 (기본적으로 모두가 말할 수 있도록 초기화)
        val mainChat = guild.createTextChannel("메인채널") {}

        // 2. 마피아 전용 비밀 채널 생성
        val evilPlayers = game.playerDatas.filter { it.job is Evil }
        val mafiaChat = guild.createTextChannel("마피아전용채팅") {
            addRoleOverwrite(guild.id) {
                denied = Permissions(
                    Permission.ViewChannel,
                    Permission.ReadMessageHistory,
                    Permission.SendMessages
                )
            }

            evilPlayers.forEach { player ->
                addMemberOverwrite(player.member.id) {
                    allowed = Permissions(Permission.ViewChannel)
                    denied = Permissions(
                        Permission.ReadMessageHistory,
                        Permission.SendMessages
                    )
                }
            }
        }

        val deadChat = guild.createTextChannel("죽은자들의채팅") {
            addRoleOverwrite(guild.id) {
                denied = Permissions(
                    Permission.ViewChannel,
                    Permission.ReadMessageHistory,
                    Permission.SendMessages
                )
            }
        }

        val couplePlayers = game.playerDatas.filter { it.job is Couple }
        val coupleChat = guild.createTextChannel("연인전용채팅") {
            addRoleOverwrite(guild.id) {
                denied = Permissions(
                    Permission.ViewChannel,
                    Permission.ReadMessageHistory,
                    Permission.SendMessages
                )
            }

            couplePlayers.forEach { player ->
                addMemberOverwrite(player.member.id) {
                    allowed = Permissions(Permission.ViewChannel)
                    denied = Permissions(
                        Permission.ReadMessageHistory,
                        Permission.SendMessages
                    )
                }
            }
        }

        game.mainChannel = mainChat
        game.mafiaChannel = mafiaChat
        game.coupleChannel = coupleChat
        game.deadChannel = deadChat
    }

    suspend fun enforceDeadPlayerChatRestriction(event: MessageCreateEvent): Boolean {
        val game = currentGame ?: return false
        val member = event.member ?: return false
        val player = game.getPlayer(member.id) ?: return false
        if (!player.state.isDead) return false

        val deadChannelId = game.deadChannel?.id
        val isDeadChannel = event.message.channelId == deadChannelId
        val canSendInDeadChannel = !player.state.isShamaned
        val textChannel = event.message.channel as? TextChannel

        if (isDeadChannel && canSendInDeadChannel) {
            textChannel?.let { grantDeadPlayerChatPermission(it, player) }
            val deceasedChatEvent = GameEvent.DeceasedChat(
                dayCount = game.dayCount,
                chatSender = player,
                chat = event.message.content
            )
            dispatchDeceasedChatEvent(game, deceasedChatEvent)
            return false
        }

        textChannel?.let { denyDeadPlayerChatPermission(it, player) }

        if (isDeadChannel && !canSendInDeadChannel) {
            runCatching {
                textChannel?.createMessage("성불 상태에서는 죽은 자들의 채팅에 메시지를 보낼 수 없습니다.")
            }
        }

        return true
    }

    private suspend fun denyDeadPlayerChatPermission(channel: TextChannel, player: PlayerData) {
        runCatching {
            channel.edit {
                addMemberOverwrite(player.member.id) {
                    denied = Permissions(Permission.SendMessages)
                }
            }
        }
    }

    private suspend fun grantDeadPlayerChatPermission(channel: TextChannel, player: PlayerData) {
        runCatching {
            channel.edit {
                addMemberOverwrite(player.member.id) {
                    denied = Permissions()
                }
            }
        }
    }

    private fun dispatchDeceasedChatEvent(game: Game, event: GameEvent.DeceasedChat) {
        val observers = game.playerDatas
            .filter { !it.state.isDead }
            .mapNotNull { player ->
                val passives = player.allAbilities
                    .filterIsInstance<PassiveAbility>()
                    .sortedByDescending(PassiveAbility::priority)
                if (passives.isEmpty()) null else player to passives
            }

        observers.forEach { (player, passives) ->
            passives.forEach { passive ->
                passive.onEventObserved(game, player, event)
                passive.onDeceasedChat(game, player, event)
            }
        }
    }

    // 지목투표 데이터 저장
    fun receiveMainVote(voterId: Snowflake, targetIdString: String): Boolean {
        val game = currentGame ?: return false

        // 현재 페이즈가 투표(VOTE 단계일 때만 표를 받습니다.
        if (game.currentPhase != GamePhase.VOTE) return false
        val voter = game.getPlayer(voterId) ?: return false
        if (voter.state.isDead) return false

        game.currentMainVotes[voterId] = targetIdString
        return true
    }

    // 찬반 투표 데이터 저장
    fun receiveProsConsVote(voterId: Snowflake, isPros: Boolean): Boolean {
        val game = currentGame ?: return false

        if (game.currentPhase != GamePhase.VOTE) return false
        val voter = game.getPlayer(voterId) ?: return false
        if (voter.state.isDead) return false
        if (game.currentProsConsVotes.containsKey(voterId)) return false

        game.currentProsConsVotes[voterId] = isPros
        return true
    }
}
