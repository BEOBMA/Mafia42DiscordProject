package org.beobma.mafia42discordproject.game

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.Member
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.VoiceChannel
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import org.beobma.mafia42discordproject.discord.DiscordMessageManager
import org.beobma.mafia42discordproject.game.player.JobPreferenceManager
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.JobManager
import kotlin.random.Random

object GameManager {
    private var currentGame: Game? = null

    private const val MIN_TEST_PLAYER_COUNT = 8
    private const val REQUIRED_MAFIA_COUNT = 2
    private const val REQUIRED_DOCTOR_COUNT = 1

    private data class AssignmentPlayer(
        val name: String,
        val preferences: List<Job>,
        var assignedJob: Job? = null
    )

    suspend fun start(event: GuildChatInputCommandInteractionCreateEvent) {
        Game(mutableListOf()).start(event)
    }

    private suspend fun Game.start(event: GuildChatInputCommandInteractionCreateEvent) {
        val interaction = event.interaction
        if (currentGame != null) {
            DiscordMessageManager.respondEphemeral(event, "이미 게임이 진행 중입니다.")
            return
        }

        val guild = interaction.guild
        val commandSender = interaction.user
        val voiceChannelId = commandSender.getVoiceStateOrNull()?.channelId ?: run {
            DiscordMessageManager.respondEphemeral(event, "현재 음성채널에 들어가 있지 않습니다.")
            return
        }
        val voiceChannel = guild.getChannelOfOrNull<VoiceChannel>(voiceChannelId) ?: run {
            DiscordMessageManager.respondEphemeral(event, "음성채널 정보를 가져오지 못했습니다.")
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
            DiscordMessageManager.respondPublic(
                event,
                buildString {
                    appendLine("아래 플레이어가 선호 직업을 설정하지 않아 게임 시작이 취소되었습니다.")
                    appendLine("`/jobpreference` 명령어로 선호 직업 7개를 먼저 설정해 주세요.")
                    append(DiscordMessageManager.mentions(membersWithoutPreference))
                }
            )
            return
        }

        currentGame = this
        this.playerDatas = membersInSameVoice.map(::PlayerData).toMutableList()

        val assignmentPlayers = buildAssignmentPlayers(membersInSameVoice)
        assignJobs(assignmentPlayers)
        publishAssignmentsToAllTextChannels(event, assignmentPlayers)

        DiscordMessageManager.respondPublic(
            event,
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

        val availableForVirtual = JobManager.getAll().filterNot { it.name == "마피아" || it.name == "의사" }
        val neededVirtualCount = (MIN_TEST_PLAYER_COUNT - players.size).coerceAtLeast(0)

        repeat(neededVirtualCount) { index ->
            val randomPreferences = availableForVirtual.shuffled().take(7)
            players += AssignmentPlayer(
                name = "가상플레이어${index + 1}",
                preferences = randomPreferences
            )
        }

        return players
    }

    private fun assignJobs(players: MutableList<AssignmentPlayer>) {
        val mafia = JobManager.findByName("마피아") ?: return
        val doctor = JobManager.findByName("의사") ?: return

        val slotCountForNonFixed = players.size - REQUIRED_MAFIA_COUNT - REQUIRED_DOCTOR_COUNT
        if (slotCountForNonFixed <= 0) {
            assignRequiredJobsRandomly(players, mafia, doctor)
            return
        }

        val nonFixedJobs = selectNonFixedJobs(players, slotCountForNonFixed, setOf(mafia.name, doctor.name))
        assignByPreference(players, nonFixedJobs)
        assignRequiredJobsRandomly(players, mafia, doctor)
    }

    private fun selectNonFixedJobs(
        players: List<AssignmentPlayer>,
        slotCount: Int,
        excludedJobNames: Set<String>
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

        while (occupiedSlots < slotCount) {
            val eligibleJobs = allCandidates.filter(::isEligible)
            if (eligibleJobs.isEmpty()) break

            val weightedJobs = eligibleJobs.map { it to (preferenceWeightByJob[it] ?: 0) }
            val picked = pickByWeight(weightedJobs)
                ?: eligibleJobs.random()

            selectedJobs += picked
            pickedNames += picked.name
            occupiedSlots += slotsFor(picked)
        }

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

    private fun assignByPreference(players: MutableList<AssignmentPlayer>, selectedJobs: List<Job>) {
        val shuffledJobs = selectedJobs.shuffled().toMutableList()
        shuffledJobs.forEach { job ->
            val requiredCount = if (job.name == "연인" || job.name == "비밀결사") 2 else 1
            val candidates = players
                .filter { it.assignedJob == null && it.preferences.any { preferred -> preferred.name == job.name } }
                .shuffled()
                .take(requiredCount)

            if (candidates.size < requiredCount) {
                return@forEach
            }

            candidates.forEach { player ->
                player.assignedJob = job
            }
        }
    }

    private fun assignRequiredJobsRandomly(players: MutableList<AssignmentPlayer>, mafia: Job, doctor: Job) {
        val unassigned = players.filter { it.assignedJob == null }.shuffled().toMutableList()

        repeat(REQUIRED_MAFIA_COUNT) {
            if (unassigned.isEmpty()) return@repeat
            val player = unassigned.removeFirst()
            player.assignedJob = mafia
        }

        repeat(REQUIRED_DOCTOR_COUNT) {
            if (unassigned.isEmpty()) return@repeat
            val player = unassigned.removeFirst()
            player.assignedJob = doctor
        }

        val fallbackPool = JobManager.getAll().filterNot { it.name == mafia.name || it.name == doctor.name }
        unassigned.forEach { player ->
            player.assignedJob = fallbackPool.randomOrNull() ?: doctor
        }
    }

    private suspend fun publishAssignmentsToAllTextChannels(
        event: GuildChatInputCommandInteractionCreateEvent,
        players: List<AssignmentPlayer>
    ) {
        val message = buildString {
            appendLine("[테스트 공개] 플레이어 직업 배정 결과")
            players.forEach { player ->
                val assignedJobName = player.assignedJob?.name ?: "미배정"
                appendLine("• ${player.name}: $assignedJobName")
            }
        }

        val guild = event.interaction.guild
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
}
