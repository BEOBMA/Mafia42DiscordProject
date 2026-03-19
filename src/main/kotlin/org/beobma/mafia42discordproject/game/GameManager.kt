package org.beobma.mafia42discordproject.game

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.createTextChannel
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.Member
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.VoiceChannel
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.channel.addMemberOverwrite
import dev.kord.rest.builder.channel.addRoleOverwrite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.beobma.mafia42discordproject.discord.DiscordMessageManager
import org.beobma.mafia42discordproject.game.player.JobPreferenceManager
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.JobManager
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.AbilityManager
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.evil.Evil
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
    private val abilitySelectionSessions: MutableMap<Snowflake, AbilitySelectionSession> = mutableMapOf()

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
        this.playerDatas = membersInSameVoice.map(::PlayerData).toMutableList()

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
        this.playerDatas = membersInSameVoice.map(::PlayerData).toMutableList()

        val assignmentPlayers = buildAssignmentPlayers(membersInSameVoice)
        assignJobs(assignmentPlayers)
        this.applyAssignedJobs(assignmentPlayers)
        initializeExtraAbilitySelectionForPlayers(assignmentPlayers)
        tryStartGameLoopWhenAbilitySelectionCompleted(guild)

        setupGameChannels(this)

        event.message.channel.createMessage(
            buildString {
                appendLine("현재 음성채널: ${voiceChannel.mention}")
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
        abilitySelectionSessions.clear()

        playerDatas.forEach { player ->
            val job = player.job ?: return@forEach
            val pool = AbilityManager.getAvailableExtraAbilitiesFor(job)
                .distinctBy(Ability::name)
                .shuffled()
                .toMutableList()

            val session = AbilitySelectionSession(availablePool = pool)
            session.currentOptions = drawAbilityOptions(session)
            abilitySelectionSessions[player.member.id] = session

            val guideMessage = buildAbilitySelectionGuideMessage(session, includeProgress = false)
            runCatching {
                player.member.getDmChannel().createMessage(
                    buildString {
                        appendLine("당신의 직업은 **${job.name}** 입니다.")
                        appendLine("이제 부가 능력을 선택해 주세요. (총 ${EXTRA_ABILITY_SELECTION_REPEAT_COUNT}회)")
                        appendLine()
                        append(guideMessage)
                    }
                )
            }.onFailure { error ->
                println("⚠️ ${player.member.effectiveName} DM 전송 실패: ${error.message}")
            }
        }

        assignVirtualPlayerExtraAbilities(players)
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
        val game = currentGame ?: return "진행 중인 게임이 없습니다."
        val player = game.getPlayer(userId) ?: return "현재 게임 참여자만 사용할 수 있는 명령어입니다."

        if (pickNumber !in 1..EXTRA_ABILITY_OPTIONS_PER_ROUND) {
            return "선택 번호는 1~${EXTRA_ABILITY_OPTIONS_PER_ROUND} 사이여야 합니다."
        }

        val session = abilitySelectionSessions[userId]
            ?: return "현재 부가 능력 선택 단계가 아니거나 이미 선택이 완료되었습니다."

        if (session.completedRounds >= EXTRA_ABILITY_SELECTION_REPEAT_COUNT || session.currentOptions.isEmpty()) {
            return "이미 부가 능력 선택이 완료되었습니다."
        }

        if (pickNumber > session.currentOptions.size) {
            return "현재 라운드에서 선택 가능한 번호가 아닙니다. 제시된 번호 중에서 선택해 주세요."
        }

        val pickedAbility = session.currentOptions[pickNumber - 1]
        val selectedUniqueAbility = pickedAbility.toJobUniqueAbility()
        if (player.job?.abilities?.none { it.name == selectedUniqueAbility.name } == true) {
            player.job?.abilities?.add(selectedUniqueAbility)
        }
        if (player.extraAbilities.none { it.name == pickedAbility.name }) {
            player.extraAbilities += pickedAbility
        }
        session.selected += pickedAbility
        session.completedRounds += 1

        if (session.completedRounds >= EXTRA_ABILITY_SELECTION_REPEAT_COUNT) {
            abilitySelectionSessions.remove(userId)
            currentGuild?.let { guild ->
                tryStartGameLoopWhenAbilitySelectionCompleted(guild)
            }
            return buildString {
                appendLine("✅ ${pickedAbility.name} 능력을 선택했습니다.")
                appendLine("부가 능력 선택이 모두 완료되었습니다.")
                append("최종 선택 능력: ${session.selected.joinToString(", ") { it.name }}")
            }
        }

        session.currentOptions = drawAbilityOptions(session)
        if (session.currentOptions.isEmpty()) {
            abilitySelectionSessions.remove(userId)
            currentGuild?.let { guild ->
                tryStartGameLoopWhenAbilitySelectionCompleted(guild)
            }
            return buildString {
                appendLine("✅ ${pickedAbility.name} 능력을 선택했습니다.")
                appendLine("추가로 제시할 수 있는 능력이 없어 선택 단계를 종료합니다.")
                append("현재 선택 능력: ${session.selected.joinToString(", ") { it.name }}")
            }
        }

        return buildString {
            appendLine("✅ ${pickedAbility.name} 능력을 선택했습니다.")
            appendLine()
            append(buildAbilitySelectionGuideMessage(session, includeProgress = true))
        }
    }

    private fun drawAbilityOptions(session: AbilitySelectionSession): List<Ability> {
        if (session.availablePool.isEmpty()) return emptyList()

        val count = minOf(EXTRA_ABILITY_OPTIONS_PER_ROUND, session.availablePool.size)
        val options = session.availablePool.shuffled().take(count)
        val optionNames = options.map(Ability::name).toSet()
        session.availablePool.removeAll { ability -> ability.name in optionNames }
        return options
    }

    private fun Ability.toJobUniqueAbility(): JobUniqueAbility =
        this as? JobUniqueAbility ?: object : JobUniqueAbility {
            override val name: String = this@toJobUniqueAbility.name
            override val description: String = this@toJobUniqueAbility.description
        }

    private fun buildAbilitySelectionGuideMessage(
        session: AbilitySelectionSession,
        includeProgress: Boolean
    ): String {
        return buildString {
            if (includeProgress) {
                appendLine(
                    "진행도: ${session.completedRounds + 1}/${EXTRA_ABILITY_SELECTION_REPEAT_COUNT}회 선택 완료"
                )
            }

            appendLine("아래 능력 중 하나를 선택해 주세요.")
            session.currentOptions.forEachIndexed { index, ability ->
                appendLine("${index + 1}. ${ability.name} - ${ability.description}")
            }
            append("선택 명령어: `/abilitypick 번호:<1~${session.currentOptions.size}>`")
        }
    }

    fun isInCurrentGame(userId: Snowflake): Boolean =
        currentGame?.playerDatas?.any { it.member.id == userId } == true

    private suspend fun tryStartGameLoopWhenAbilitySelectionCompleted(guild: GuildBehavior) {
        val game = currentGame ?: return
        if (abilitySelectionSessions.isNotEmpty()) return
        if (game.isRunning) return
        if (gameLoopJob?.isActive == true) return

        game.isRunning = true
        publishMessageToAllTextChannels(guild, "✅ 모든 플레이어의 부가 능력 선택이 완료되어 게임을 시작합니다.")
        gameLoopJob = gameLoopScope.launch {
            GameLoopManager.runGameLoop(game)
        }
    }

    private suspend fun publishMessageToAllTextChannels(guild: GuildBehavior, message: String) {
        val textChannels = guild.channels
            .filter { it is TextChannel }
            .toList()
            .map { it as TextChannel }

        textChannels.forEach { channel ->
            runCatching {
                channel.createMessage(message)
            }
        }
    }

    suspend fun stop(event: GuildChatInputCommandInteractionCreateEvent) {
        val gameToStop = currentGame

        if (gameToStop == null) {
            DiscordMessageManager.respondEphemeral(event, "진행 중인 게임이 없습니다.")
            return
        }
        gameToStop.mainChannel?.delete("게임 강제 종료로 인한 채널 삭제")
        gameToStop.mafiaChannel?.delete("게임 강제 종료로 인한 채널 삭제")

        currentGame = null
        currentGuild = null
        abilitySelectionSessions.clear()
        gameLoopJob?.cancel()
        gameLoopJob = null

        val mention = DiscordMessageManager.mention(event.interaction.user)
        DiscordMessageManager.respondPublic(event, "${mention}이(가) 게임을 종료했습니다.")
    }

    suspend fun stop(event: MessageCreateEvent) {
        val gameToStop = currentGame

        if (gameToStop == null) {
            event.message.channel.createMessage("진행 중인 게임이 없습니다.")
            return
        }
        gameToStop.mainChannel?.delete("게임 강제 종료로 인한 채널 삭제")
        gameToStop.mafiaChannel?.delete("게임 강제 종료로 인한 채널 삭제")

        currentGame = null
        currentGuild = null
        abilitySelectionSessions.clear()
        gameLoopJob?.cancel()
        gameLoopJob = null

        val mention = event.message.author?.mention.orEmpty()
        event.message.channel.createMessage("${mention}이(가) 게임을 종료했습니다.")
    }

    suspend fun setupGameChannels(game: Game) {
        val guild = game.guild

        // 1. 메인 채널 설정 (기본적으로 모두가 말할 수 있도록 초기화)
        val mainChat = guild.createTextChannel("메인채널") {}
        // (선택 사항: 카테고리를 만들어서 채널들을 묶어둘 수도 있습니다)

        // 2. 마피아 전용 비밀 채널 생성
        val evilPlayers = game.playerDatas.filter { it.job is Evil }
        val mafiaChat = guild.createTextChannel("마피아전용채널") {
            // 기본적으로 @everyone 역할은 이 채널을 아예 볼 수 없도록 설정 (ViewChannel 차단)
            addRoleOverwrite(guild.id) {
                denied = Permissions(Permission.ViewChannel)
            }

            // 2. 필터링해 둔 악인 팀원들에게만 보기 및 쓰기 권한 부여
            for (player in evilPlayers) {
                addMemberOverwrite(player.member.id) {
                    allowed = Permissions(Permission.ViewChannel, Permission.SendMessages)
                }
            }
        }

        game.mainChannel = mainChat
        game.mafiaChannel = mafiaChat

        mafiaChat.createMessage("마피아 전용채널")
    }
}
