package org.beobma.mafia42discordproject.game

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.Member
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.VoiceChannel
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import org.beobma.mafia42discordproject.discord.DiscordMessageManager
import org.beobma.mafia42discordproject.game.GameManager.publishAssignmentsToAllTextChannels
import org.beobma.mafia42discordproject.game.player.JobPreferenceManager
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.JobManager
import org.beobma.mafia42discordproject.job.evil.Evil
import kotlin.random.Random

object GameManager {
    private var currentGame: Game? = null

    private const val MIN_TEST_PLAYER_COUNT = 8
    private const val REQUIRED_MAFIA_COUNT = 2
    private const val REQUIRED_DOCTOR_COUNT = 1
    private const val REQUIRED_POLICE_COUNT = 1

    private val policeJobNames = setOf("경찰", "요원")
    private val excludedVirtualPreferenceJobNames = setOf("시민", "악인")

    private data class AssignmentPlayer(
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

    suspend fun start(event: GuildChatInputCommandInteractionCreateEvent) {
        Game(mutableListOf()).start(event)
    }

    suspend fun start(event: MessageCreateEvent) {
        Game(mutableListOf()).start(event)
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
        this.playerDatas = membersInSameVoice.map(::PlayerData).toMutableList()

        val assignmentPlayers = buildAssignmentPlayers(membersInSameVoice)
        val trace = assignJobs(assignmentPlayers)
        publishAssignmentsToAllTextChannels(event, assignmentPlayers, trace)

        deferredResponse.respond {
            content = buildString {
                appendLine("현재 음성채널: ${voiceChannel.mention}")
                appendLine("실제 인원 수: ${membersInSameVoice.size}")
                appendLine("테스트 인원 수(가상 포함): ${assignmentPlayers.size}")
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
        this.playerDatas = membersInSameVoice.map(::PlayerData).toMutableList()

        val assignmentPlayers = buildAssignmentPlayers(membersInSameVoice)
        val trace = assignJobs(assignmentPlayers)
        publishAssignmentsToAllTextChannelsGuild(guild, assignmentPlayers, trace)

        event.message.channel.createMessage(
            buildString {
                appendLine("현재 음성채널: ${voiceChannel.mention}")
                appendLine("실제 인원 수: ${membersInSameVoice.size}")
                appendLine("테스트 인원 수(가상 포함): ${assignmentPlayers.size}")
                appendLine()
                append(DiscordMessageManager.mentions(membersInSameVoice))
            }
        )
    }

    private fun buildAssignmentPlayers(members: List<Member>): MutableList<AssignmentPlayer> {
        val players = members.map { member ->
            AssignmentPlayer(
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
        val doctor = JobManager.findByName("의사") ?: run {
            trace.add("[오류] 의사 직업 정의를 찾지 못했습니다.")
            return trace
        }
        val policePool = JobManager.getAll().filter { it.name in policeJobNames }
        if (policePool.isEmpty()) {
            trace.add("[오류] 경찰 계열 직업 정의를 찾지 못했습니다.")
            return trace
        }

        trace.add("[1단계] 참여 인원: ${players.size}명")
        trace.add(
            "[1단계] 고정 배정 직업: 마피아 ${REQUIRED_MAFIA_COUNT}명, 의사 ${REQUIRED_DOCTOR_COUNT}명, 경찰계열 ${REQUIRED_POLICE_COUNT}명"
        )

        val slotCountForNonFixed = players.size - REQUIRED_MAFIA_COUNT - REQUIRED_DOCTOR_COUNT - REQUIRED_POLICE_COUNT
        if (slotCountForNonFixed <= 0) {
            trace.add("[2단계] 고정 직업만 배정 가능한 인원 수라서 바로 랜덤 배정 진행")
            assignRequiredJobsRandomly(players, mafia, doctor, policePool, trace)
            return trace
        }

        val nonFixedJobs = selectNonFixedJobs(
            players = players,
            slotCount = slotCountForNonFixed,
            excludedJobNames = setOf(mafia.name, doctor.name) + policeJobNames,
            trace = trace
        )
        assignByPreference(players, nonFixedJobs, trace)
        assignRequiredJobsRandomly(players, mafia, doctor, policePool, trace)
        return trace
    }

    private fun selectNonFixedJobs(
        players: List<AssignmentPlayer>,
        slotCount: Int,
        excludedJobNames: Set<String>,
        trace: AssignmentTrace
    ): List<Job> {
        val allCandidates = JobManager.getAll().filterNot { excludedJobNames.contains(it.name) }
        val preferenceWeightByJob = mutableMapOf<Job, Int>()

        players.flatMap { it.preferences }.forEach { job ->
            if (!excludedJobNames.contains(job.name)) {
                preferenceWeightByJob[job] = (preferenceWeightByJob[job] ?: 0) + 1
            }
        }

        val selectedJobs = mutableListOf<Job>()
        val pickedNames = mutableSetOf<String>()
        var occupiedSlots = 0

        fun slotsFor(job: Job): Int = if (job.name == "연인" || job.name == "비밀결사") 2 else 1

        fun isEligible(job: Job): Boolean {
            if (pickedNames.contains(job.name)) return false

            val requiredSlots = slotsFor(job)
            if (occupiedSlots + requiredSlots > slotCount) return false

            if (job.name == "연인" || job.name == "비밀결사") {
                val preferredPlayerCount = players.count { player -> player.preferences.any { it.name == job.name } }
                if (preferredPlayerCount < 2) return false
            }
            return true
        }

        trace.add("[2단계] 고정 직업 제외 슬롯 수: $slotCount")
        trace.add("[2단계] 후보 직업 수: ${allCandidates.size}개")
        val sortedWeightSummary = preferenceWeightByJob.entries
            .sortedWith(compareByDescending<Map.Entry<Job, Int>> { it.value }.thenBy { it.key.name })
            .joinToString(", ") { (job, weight) -> "${job.name}($weight)" }
        trace.add("[2단계] 전체 선호 직업 가중치: ${if (sortedWeightSummary.isEmpty()) "없음" else sortedWeightSummary}")
        players.forEach { player ->
            val playerWeightSummary = player.preferences
                .joinToString(", ") { job -> "${job.name}(${preferenceWeightByJob[job] ?: 0})" }
            trace.add("[2단계] ${player.name} 선호 직업 가중치: $playerWeightSummary")
        }

        while (occupiedSlots < slotCount) {
            val eligibleJobs = allCandidates.filter(::isEligible)
            if (eligibleJobs.isEmpty()) break

            val weightedJobs = eligibleJobs.map { it to (preferenceWeightByJob[it] ?: 0) }
            val picked = pickByWeight(weightedJobs)
                ?: eligibleJobs.random()
            val pickedWeight = preferenceWeightByJob[picked] ?: 0

            selectedJobs += picked
            pickedNames += picked.name
            occupiedSlots += slotsFor(picked)
            trace.add(
                "[2단계] 선택 직업: ${picked.name} (가중치 $pickedWeight / 필요 슬롯 ${slotsFor(picked)} / 누적 $occupiedSlots)"
            )
        }

        trace.add("[2단계] 최종 선택된 비고정 직업: ${selectedJobs.joinToString { it.name }}")
        return selectedJobs
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

    private fun assignByPreference(
        players: MutableList<AssignmentPlayer>,
        selectedJobs: List<Job>,
        trace: AssignmentTrace
    ) {
        trace.add("[3단계] 선호 기반 배정 시작")
        val shuffledJobs = selectedJobs.shuffled().toMutableList()
        shuffledJobs.forEach { job ->
            val requiredCount = if (job.name == "연인" || job.name == "비밀결사") 2 else 1
            val candidates = players
                .filter { it.assignedJob == null && it.preferences.any { preferred -> preferred.name == job.name } }
                .shuffled()
                .take(requiredCount)

            if (candidates.size < requiredCount) {
                trace.add("[3단계] ${job.name}: 선호 인원 부족으로 배정 건너뜀")
                return@forEach
            }

            candidates.forEach { player ->
                player.assignedJob = job
            }
            trace.add("[3단계] ${job.name}: ${candidates.joinToString { it.name }} 배정")
        }
    }

    private fun assignRequiredJobsRandomly(
        players: MutableList<AssignmentPlayer>,
        mafia: Job,
        doctor: Job,
        policePool: List<Job>,
        trace: AssignmentTrace
    ) {
        trace.add("[4단계] 고정 직업/잔여 인원 배정 시작")
        val unassigned = players.filter { it.assignedJob == null }.shuffled().toMutableList()

        repeat(REQUIRED_MAFIA_COUNT) {
            if (unassigned.isEmpty()) return@repeat
            val player = unassigned.removeFirst()
            player.assignedJob = mafia
            trace.add("[4단계] 마피아 배정: ${player.name}")
        }

        repeat(REQUIRED_DOCTOR_COUNT) {
            if (unassigned.isEmpty()) return@repeat
            val player = unassigned.removeFirst()
            player.assignedJob = doctor
            trace.add("[4단계] 의사 배정: ${player.name}")
        }

        repeat(REQUIRED_POLICE_COUNT) {
            if (unassigned.isEmpty()) return@repeat
            val player = unassigned.removeFirst()
            val policeJob = policePool.random()
            player.assignedJob = policeJob
            trace.add("[4단계] 경찰계열 배정: ${player.name} -> ${policeJob.name}")
        }

        val fallbackPool = JobManager.getAll().filterNot {
            it.name == mafia.name || it.name == doctor.name || it.name in policeJobNames
        }
        unassigned.forEach { player ->
            player.assignedJob = fallbackPool.randomOrNull() ?: doctor
            trace.add("[4단계] 잔여 랜덤 배정: ${player.name} -> ${player.assignedJob?.name}")
        }
        trace.add("[4단계] 배정 완료")
    }

    private suspend fun publishAssignmentsToAllTextChannels(
        event: GuildChatInputCommandInteractionCreateEvent,
        players: List<AssignmentPlayer>,
        trace: AssignmentTrace
    ) {
        publishAssignmentsToAllTextChannelsGuild(event.interaction.guild, players, trace)
    }

    private suspend fun publishAssignmentsToAllTextChannelsGuild(
        guild: GuildBehavior,
        players: List<AssignmentPlayer>,
        trace: AssignmentTrace
    ) {
        val message = buildString {
            appendLine("[테스트 공개] 플레이어 직업 배정 결과")
            appendLine()
            appendLine("[배정 과정 상세 로그]")
            trace.lines.forEach { line ->
                appendLine("• $line")
            }
            appendLine()
            appendLine("[최종 직업 결과]")
            players.forEach { player ->
                val assignedJobName = player.assignedJob?.name ?: "미배정"
                appendLine("• ${player.name}: $assignedJobName")
            }
        }

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

    fun isInCurrentGame(userId: Snowflake): Boolean =
        currentGame?.playerDatas?.any { it.member.id == userId } == true

    suspend fun stop(event: GuildChatInputCommandInteractionCreateEvent) {
        if (currentGame == null) {
            DiscordMessageManager.respondEphemeral(event, "진행 중인 게임이 없습니다.")
            return
        }
        currentGame = null

        val mention = DiscordMessageManager.mention(event.interaction.user)
        DiscordMessageManager.respondPublic(event, "${mention}이(가) 게임을 종료했습니다.")
    }

    suspend fun stop(event: MessageCreateEvent) {
        if (currentGame == null) {
            event.message.channel.createMessage("진행 중인 게임이 없습니다.")
            return
        }
        currentGame = null

        val mention = event.message.author?.mention.orEmpty()
        event.message.channel.createMessage("${mention}이(가) 게임을 종료했습니다.")
    }
}
