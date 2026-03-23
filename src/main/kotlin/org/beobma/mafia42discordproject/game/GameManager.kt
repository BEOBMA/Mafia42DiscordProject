package org.beobma.mafia42discordproject.game

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
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
import kotlinx.coroutines.cancelAndJoin
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
import org.beobma.mafia42discordproject.job.ability.general.definition.list.shaman.Manifesto
import org.beobma.mafia42discordproject.job.definition.list.Cabal
import org.beobma.mafia42discordproject.job.definition.list.CabalRole
import org.beobma.mafia42discordproject.job.definition.list.Couple
import org.beobma.mafia42discordproject.job.definition.list.CoupleRole
import org.beobma.mafia42discordproject.job.definition.list.Detective
import org.beobma.mafia42discordproject.job.definition.list.Doctor
import org.beobma.mafia42discordproject.job.definition.list.Hacker
import org.beobma.mafia42discordproject.job.definition.list.Mercenary
import org.beobma.mafia42discordproject.job.definition.list.Nurse
import org.beobma.mafia42discordproject.job.definition.list.Police
import org.beobma.mafia42discordproject.job.definition.list.Politician
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import org.beobma.mafia42discordproject.job.ability.general.definition.list.nurse.Oath
import org.beobma.mafia42discordproject.job.ability.general.definition.list.other.Eavesdropping
import org.beobma.mafia42discordproject.job.ability.general.list.Megaphone
import org.beobma.mafia42discordproject.job.ability.general.list.Perjury
import org.beobma.mafia42discordproject.job.ability.general.list.SecretLetter
import org.beobma.mafia42discordproject.job.ability.general.list.Will
import org.beobma.mafia42discordproject.job.ability.general.evil.list.Password
import org.beobma.mafia42discordproject.job.ability.general.evil.list.godfather.GodfatherContactPolicy
import org.beobma.mafia42discordproject.job.evil.Evil
import org.beobma.mafia42discordproject.job.evil.list.Beastman
import org.beobma.mafia42discordproject.job.evil.list.Godfather
import org.beobma.mafia42discordproject.job.evil.list.HitMan
import org.beobma.mafia42discordproject.job.evil.list.Hostess
import org.beobma.mafia42discordproject.job.evil.list.MadScientist
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import org.beobma.mafia42discordproject.job.evil.list.Spy
import org.beobma.mafia42discordproject.job.evil.list.Swindler
import org.beobma.mafia42discordproject.job.evil.list.Thief
import org.beobma.mafia42discordproject.job.evil.list.Villain
import org.beobma.mafia42discordproject.job.evil.list.Witch
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

object GameManager {
    private var currentGame: Game? = null
    private var currentGuild: GuildBehavior? = null
    private val gameLoopScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var gameLoopJob: kotlinx.coroutines.Job? = null

    private const val FULL_GAME_PLAYER_COUNT = 8
    private const val EXTENDED_ROLE_RULE_START_COUNT = 9
    private const val MAX_GAME_PLAYER_COUNT = 20

    private data class RequiredRoleCounts(
        val mafiaCount: Int,
        val assistantCount: Int,
        val doctorCount: Int,
        val policeCount: Int,
        val citizenCount: Int = 0
    )
    private const val EXTRA_ABILITY_SELECTION_REPEAT_COUNT = 3
    private const val EXTRA_ABILITY_OPTIONS_PER_ROUND = 3

    private const val GAME_MAIN_CHANNEL_ID = 1485008595727679690L
    private const val GAME_MAFIA_CHANNEL_ID = 1485008648886423622L
    private const val GAME_COUPLE_CHANNEL_ID = 1485008669279125745L
    private const val GAME_DEAD_CHANNEL_ID = 1485008691961790484L
    private const val SHAMAN_RELAY_COMMAND = "접신"
    private const val SHAMANED_RELAY_COMMAND = "강령"
    private const val MEGAPHONE_COMMAND = "확성기"
    private const val SECRET_LETTER_COMMAND = "밀서"
    private const val WILL_COMMAND = "유언"
    private const val PERJURY_COMMAND = "위증"
    private const val PASSWORD_COMMAND = "암구호"
    private const val GAME_CHANNEL_SPACER_LINES = 180

    data class SpiritRelayResult(
        val isSuccess: Boolean,
        val message: String
    )

    private val policeJobNames = setOf("경찰", "요원", "자경단원")
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
        if (membersInSameVoice.size > MAX_GAME_PLAYER_COUNT) {
            deferredResponse.respond {
                content = "최대 ${MAX_GAME_PLAYER_COUNT}명까지만 게임을 시작할 수 있습니다. 현재 인원: ${membersInSameVoice.size}명"
            }
            return
        }

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
        this.initialPlayerCount = this.playerDatas.size

        val assignmentPlayers = buildAssignmentPlayers(membersInSameVoice)
        assignJobs(assignmentPlayers)
        this.applyAssignedJobs(assignmentPlayers)
        initializeExtraAbilitySelectionForPlayers(assignmentPlayers)
        tryStartGameLoopWhenAbilitySelectionCompleted(guild)

        setupGameChannels(this)
        sendGameChannelSpacer(this)

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
        if (membersInSameVoice.size > MAX_GAME_PLAYER_COUNT) {
            event.message.channel.createMessage(
                "최대 ${MAX_GAME_PLAYER_COUNT}명까지만 게임을 시작할 수 있습니다. 현재 인원: ${membersInSameVoice.size}명"
            )
            return
        }

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
        this.initialPlayerCount = this.playerDatas.size

        val assignmentPlayers = buildAssignmentPlayers(membersInSameVoice)
        assignJobs(assignmentPlayers)
        this.applyAssignedJobs(assignmentPlayers)
        initializeExtraAbilitySelectionForPlayers(assignmentPlayers)
        tryStartGameLoopWhenAbilitySelectionCompleted(guild)

        setupGameChannels(this)
        sendGameChannelSpacer(this)

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
        assignMercenaryClient()
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

    private fun Game.assignMercenaryClient() {
        val mercenaries = playerDatas.filter { it.job is Mercenary }
        if (mercenaries.isEmpty()) return

        mercenaries.forEach { mercenaryPlayer ->
            val mercenaryJob = mercenaryPlayer.job as? Mercenary ?: return@forEach
            val candidates = playerDatas.filter { candidate ->
                candidate.member.id != mercenaryPlayer.member.id &&
                    candidate.job != null &&
                    candidate.job !is Evil
            }
            mercenaryJob.clientPlayerId = candidates.randomOrNull()?.member?.id
        }
    }

    private fun notifyNurseOath(game: Game) {
        val hasNurseWithOath = game.playerDatas.any { player ->
            player.job is Nurse && player.allAbilities.any { it is Oath }
        }
        if (!hasNurseWithOath) return

        gameLoopScope.launch {
            game.playerDatas
                .filter { it.job is Doctor }
                .forEach { doctorPlayer ->
                    runCatching {
                        doctorPlayer.member.getDmChannel().createMessage("간호사의 선서를 받았습니다")
                    }
                }
        }
    }

    private fun assignJobs(players: MutableList<AssignmentPlayer>): AssignmentTrace {
        val trace = AssignmentTrace()
        val requiredCounts = resolveRequiredRoleCounts(players.size)

        val mafia = JobManager.findByName("마피아") ?: run {
            trace.add("[오류] 마피아 직업 정의를 찾지 못했습니다.")
            return trace
        }
        val doctor = JobManager.findByName("의사") ?: run {
            trace.add("[오류] 의사 직업 정의를 찾지 못했습니다.")
            return trace
        }

        val policePool = JobManager.getAll().filter { it.name in policeJobNames }
        if (requiredCounts.policeCount > 0 && policePool.isEmpty()) {
            trace.add("[오류] 경찰 계열 직업 정의를 찾지 못했습니다.")
            return trace
        }

        val assistantPool = JobManager.getAll().filter { it is Evil && it.name != mafia.name }
        if (requiredCounts.assistantCount > 0 && assistantPool.isEmpty()) {
            trace.add("[오류] 보조 계열 직업 정의를 찾지 못했습니다.")
            return trace
        }

        val selectedPoliceJob = if (requiredCounts.policeCount > 0) {
            pickPoliceJobByPreference(players, policePool, trace)
        } else {
            null
        }

        val selectedAssistantJob = if (requiredCounts.assistantCount > 0) {
            pickAssistantJobByPreference(players, assistantPool, trace)
        } else {
            null
        }

        trace.add("[1단계] 참여 인원: ${players.size}명")
        trace.add(
            "[1단계] 고정 배정 직업: 마피아 ${requiredCounts.mafiaCount}명, 보조계열 ${requiredCounts.assistantCount}명, 의사 ${requiredCounts.doctorCount}명, 경찰계열 ${requiredCounts.policeCount}명, 시민 ${requiredCounts.citizenCount}명"
        )

        val requiredFixedCount =
            requiredCounts.mafiaCount +
                requiredCounts.assistantCount +
                requiredCounts.doctorCount +
                requiredCounts.policeCount +
                requiredCounts.citizenCount

        val slotCountForNonFixed = players.size - requiredFixedCount
        if (slotCountForNonFixed <= 0) {
            trace.add("[2단계] 고정 직업만 배정 가능한 인원 수라서 바로 랜덤 배정 진행")
            assignRequiredJobs(players, mafia, selectedAssistantJob, doctor, selectedPoliceJob, requiredCounts, trace)
            return trace
        }

        assignNonFixedJobsByPreference(
            players = players,
            slotCount = slotCountForNonFixed,
            trace = trace
        )
        assignRequiredJobs(players, mafia, selectedAssistantJob, doctor, selectedPoliceJob, requiredCounts, trace)
        return trace
    }


    private fun resolveRequiredRoleCounts(playerCount: Int): RequiredRoleCounts {
        return if (playerCount >= EXTENDED_ROLE_RULE_START_COUNT) {
            val mafiaCount = 2 + ((playerCount - EXTENDED_ROLE_RULE_START_COUNT) / 2)
            val citizenCount = if (playerCount % 2 == 0) 1 else 0
            RequiredRoleCounts(
                mafiaCount = mafiaCount,
                assistantCount = 1,
                doctorCount = 1,
                policeCount = 1,
                citizenCount = citizenCount
            )
        } else if (playerCount >= FULL_GAME_PLAYER_COUNT) {
            RequiredRoleCounts(mafiaCount = 2, assistantCount = 1, doctorCount = 1, policeCount = 1)
        } else {
            when (playerCount) {
                7, 6 -> RequiredRoleCounts(mafiaCount = 1, assistantCount = 1, doctorCount = 1, policeCount = 1)
                5, 4 -> RequiredRoleCounts(mafiaCount = 1, assistantCount = 0, doctorCount = 1, policeCount = 1)
                else -> {
                    val mafia = if (playerCount >= 2) 1 else 0
                    val doctor = if (playerCount >= 1) 1 else 0
                    val police = if (playerCount >= 3) 1 else 0
                    RequiredRoleCounts(mafiaCount = mafia, assistantCount = 0, doctorCount = doctor, policeCount = police)
                }
            }
        }
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
        assistant: Job?,
        doctor: Job,
        policeJob: Job?,
        requiredCounts: RequiredRoleCounts,
        trace: AssignmentTrace
    ) {
        trace.add("[3단계] 고정 직업/잔여 인원 배정 시작")
        val unassigned = players.filter { it.assignedJob == null }.shuffled().toMutableList()

        repeat(requiredCounts.mafiaCount) {
            if (unassigned.isEmpty()) return@repeat
            val player = unassigned.removeFirst()
            player.assignedJob = mafia
            trace.add("[3단계] 마피아 배정: ${player.name}")
        }

        repeat(requiredCounts.assistantCount) {
            if (unassigned.isEmpty()) return@repeat
            val assistantJob = requireNotNull(assistant) { "보조계열 고정 직업이 필요하지만 선택되지 않았습니다." }
            val preferred = unassigned.firstOrNull { candidate ->
                candidate.preferences.any { it.name == assistantJob.name }
            }
            val player = preferred ?: unassigned.first()
            unassigned.remove(player)
            player.assignedJob = assistantJob
            trace.add("[3단계] 보조계열 배정: ${player.name} -> ${assistantJob.name}")
        }

        repeat(requiredCounts.doctorCount) {
            if (unassigned.isEmpty()) return@repeat
            val player = unassigned.removeFirst()
            player.assignedJob = doctor
            trace.add("[3단계] 의사 배정: ${player.name}")
        }

        repeat(requiredCounts.policeCount) {
            if (unassigned.isEmpty()) return@repeat
            val selectedPoliceJob = requireNotNull(policeJob) { "경찰계열 고정 직업이 필요하지만 선택되지 않았습니다." }
            val preferred = unassigned.firstOrNull { candidate ->
                candidate.preferences.any { it.name == selectedPoliceJob.name }
            }
            val player = preferred ?: unassigned.first()
            unassigned.remove(player)
            player.assignedJob = selectedPoliceJob
            trace.add("[3단계] 경찰계열 배정: ${player.name} -> ${selectedPoliceJob.name}")
        }

        repeat(requiredCounts.citizenCount) {
            if (unassigned.isEmpty()) return@repeat
            val player = unassigned.removeFirst()
            player.assignedJob = JobManager.findByName("시민") ?: doctor
            trace.add("[3단계] 시민 배정: ${player.name} -> ${player.assignedJob?.name}")
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

                        val cabalJob = job as? Cabal
                        if (cabalJob != null) {
                            val cabalRoleMessage = when (cabalJob.role) {
                                CabalRole.SUN -> "당신은 해 비밀결사입니다."
                                CabalRole.MOON -> "당신은 달 비밀결사입니다."
                                null -> null
                            }
                            if (cabalRoleMessage != null) {
                                dmChannel.createMessage(cabalRoleMessage)
                            }
                        }

                        buildMafiaTeammateMessage(this@initializeExtraAbilitySelectionForPlayers, player)
                            ?.let { dmChannel.createMessage(it) }
                        buildCouplePartnerMessage(this@initializeExtraAbilitySelectionForPlayers, player)
                            ?.let { dmChannel.createMessage(it) }

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

    private fun buildMafiaTeammateMessage(game: Game, player: PlayerData): String? {
        if (player.job !is Mafia) return null

        val teammateNames = game.playerDatas
            .filter { candidate ->
                candidate.member.id != player.member.id &&
                    candidate.job is Mafia
            }
            .map { it.member.effectiveName }

        if (teammateNames.isEmpty()) return null
        return "당신과 함께하는 다른 마피아: ${teammateNames.joinToString(", ")}"
    }

    private fun buildCouplePartnerMessage(game: Game, player: PlayerData): String? {
        val coupleJob = player.job as? Couple ?: return null
        val partnerId = coupleJob.pairedPlayerId ?: return null
        if (partnerId == player.member.id) return null

        val partnerName = game.playerDatas
            .firstOrNull { it.member.id == partnerId }
            ?.member
            ?.effectiveName
            ?: return null

        return "당신의 짝 연인은 ${partnerName}입니다."
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

        notifyNurseOath(game)
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
        currentGame = null
        currentGuild = null
        gameLoopJob?.cancelAndJoin()
        gameLoopJob = null
        GameLoopManager.clearTimeThread()
        abilitySelectionSessions.clear()
        releaseAllPlayerMutes(gameToStop)

        gameToStop.mainChannel = null
        gameToStop.mafiaChannel = null
        gameToStop.coupleChannel = null
        gameToStop.deadChannel = null
    }

    suspend fun releaseAllPlayerMutes(game: Game) {
        game.playerDatas.forEach { player ->
            runCatching {
                player.member.edit {
                    muted = false
                }
            }
        }
    }

    private suspend fun sendGameChannelSpacer(game: Game) {
        val spacerMessage = buildString {
            repeat(GAME_CHANNEL_SPACER_LINES) {
                appendLine(".")
            }
        }

        sendChannelSpacer(game.mainChannel, "메인", spacerMessage)
        sendChannelSpacer(game.mafiaChannel, "마피아", spacerMessage)
        sendChannelSpacer(game.coupleChannel, "연인", spacerMessage)
        sendChannelSpacer(game.deadChannel, "죽은자", spacerMessage)
    }

    private suspend fun sendChannelSpacer(channel: TextChannel?, channelName: String, spacerMessage: String) {
        if (channel == null) return

        runCatching {
            channel.createMessage(spacerMessage)
        }.onFailure { exception ->
            println("[GameManager] ${channelName} 채널 줄넘김 메시지 전송 실패: ${exception.message}")
        }
    }

    suspend fun setupGameChannels(game: Game) {
        val guild = game.guild

        val mainChat = guild.getChannelOfOrNull<TextChannel>(Snowflake(GAME_MAIN_CHANNEL_ID))
        val mafiaChat = guild.getChannelOfOrNull<TextChannel>(Snowflake(GAME_MAFIA_CHANNEL_ID))
        val coupleChat = guild.getChannelOfOrNull<TextChannel>(Snowflake(GAME_COUPLE_CHANNEL_ID))
        val deadChat = guild.getChannelOfOrNull<TextChannel>(Snowflake(GAME_DEAD_CHANNEL_ID))

        if (mainChat == null || mafiaChat == null || coupleChat == null || deadChat == null) {
            error("게임 채널을 찾을 수 없습니다. 채널 ID 설정을 확인해 주세요.")
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
        if (!GameLoopManager.isMadScientistDistortionHidden(player) && !player.state.isDead) return false

        val isDeadChannel = event.message.channelId == Snowflake(GAME_DEAD_CHANNEL_ID)

        if (isDeadChannel && player.state.isDead) {
            val deceasedChatEvent = GameEvent.DeceasedChat(
                dayCount = game.dayCount,
                chatSender = player,
                chat = event.message.content
            )
            dispatchDeceasedChatEvent(game, deceasedChatEvent)
            return false
        }

        if (player.state.isDead) {
            runCatching { event.message.delete("사망 플레이어 채팅 채널 제한") }
            return true
        }

        runCatching { event.message.delete("왜곡 상태 채팅 차단") }
        return true
    }

    suspend fun handleSpiritCommands(event: MessageCreateEvent, commandName: String, args: List<String>): Boolean {
        val message = args.joinToString(" ").trim()

        return when (commandName) {
            SHAMAN_RELAY_COMMAND -> {
                val memberId = event.member?.id ?: return false
                val result = relayShamanMessage(memberId, message)
                result.isSuccess
            }
            SHAMANED_RELAY_COMMAND -> {
                val memberId = event.member?.id ?: return false
                val result = relayShamanedMessage(memberId, event.message.channelId, message)
                if (result.isSuccess) {
                    runCatching { event.message.delete("성불 플레이어 강령 전달 처리") }
                }
                result.isSuccess
            }
            else -> false
        }
    }

    suspend fun useMegaphone(memberId: Snowflake, message: String): SpiritRelayResult {
        val game = currentGame ?: return SpiritRelayResult(false, "진행 중인 게임이 없습니다.")
        val sender = game.getPlayer(memberId) ?: return SpiritRelayResult(false, "게임 참가자만 사용할 수 있습니다.")
        return sendMegaphoneMessage(game, sender, message)
    }

    fun sendSecretLetter(memberId: Snowflake, targetId: Snowflake, message: String): SpiritRelayResult {
        val game = currentGame ?: return SpiritRelayResult(false, "진행 중인 게임이 없습니다.")
        val sender = game.getPlayer(memberId) ?: return SpiritRelayResult(false, "게임 참가자만 사용할 수 있습니다.")
        val target = game.getPlayer(targetId) ?: return SpiritRelayResult(false, "밀서 대상을 찾을 수 없습니다.")
        return sendSecretLetter(game, sender, target, message)
    }

    fun writeWill(memberId: Snowflake, message: String): SpiritRelayResult {
        val game = currentGame ?: return SpiritRelayResult(false, "진행 중인 게임이 없습니다.")
        val sender = game.getPlayer(memberId) ?: return SpiritRelayResult(false, "게임 참가자만 사용할 수 있습니다.")
        return writeWill(game, sender, message)
    }

    fun castPerjuryVote(memberId: Snowflake, targetId: Snowflake): SpiritRelayResult {
        val game = currentGame ?: return SpiritRelayResult(false, "진행 중인 게임이 없습니다.")
        val sender = game.getPlayer(memberId) ?: return SpiritRelayResult(false, "게임 참가자만 사용할 수 있습니다.")
        val target = game.getPlayer(targetId) ?: return SpiritRelayResult(false, "위증 대상을 찾을 수 없습니다.")
        return castPerjuryVote(game, sender, target)
    }

    suspend fun sendPasswordChat(memberId: Snowflake, message: String): SpiritRelayResult {
        val game = currentGame ?: return SpiritRelayResult(false, "진행 중인 게임이 없습니다.")
        val sender = game.getPlayer(memberId) ?: return SpiritRelayResult(false, "게임 참가자만 사용할 수 있습니다.")
        return sendPasswordChat(game, sender, message)
    }

    suspend fun handleNightUtilityCommands(event: MessageCreateEvent, commandName: String, args: List<String>): Boolean {
        val game = currentGame ?: return false
        val memberId = event.member?.id ?: return false
        val sender = game.getPlayer(memberId) ?: return false

        return when (commandName) {
            MEGAPHONE_COMMAND -> {
                val message = args.joinToString(" ").trim()
                val result = sendMegaphoneMessage(game, sender, message)
                event.message.channel.createMessage(result.message)
                true
            }

            SECRET_LETTER_COMMAND -> {
                if (args.size < 2) {
                    event.message.channel.createMessage("사용법: !밀서 @대상 내용")
                    return true
                }
                val target = parseTargetPlayer(game, args.first())
                val result = if (target == null) {
                    SpiritRelayResult(false, "밀서 대상을 찾을 수 없습니다.")
                } else {
                    sendSecretLetter(game, sender, target, args.drop(1).joinToString(" ").trim())
                }
                event.message.channel.createMessage(result.message)
                true
            }

            WILL_COMMAND -> {
                val result = writeWill(game, sender, args.joinToString(" ").trim())
                event.message.channel.createMessage(result.message)
                true
            }

            PERJURY_COMMAND -> {
                if (args.isEmpty()) {
                    event.message.channel.createMessage("사용법: !위증 @대상")
                    return true
                }
                val target = parseTargetPlayer(game, args.first())
                val result = if (target == null) {
                    SpiritRelayResult(false, "위증 대상을 찾을 수 없습니다.")
                } else {
                    castPerjuryVote(game, sender, target)
                }
                event.message.channel.createMessage(result.message)
                true
            }

            PASSWORD_COMMAND -> {
                val result = sendPasswordChat(game, sender, args.joinToString(" ").trim())
                event.message.channel.createMessage(result.message)
                true
            }

            else -> false
        }
    }

    private suspend fun sendMegaphoneMessage(game: Game, sender: PlayerData, message: String): SpiritRelayResult {
        if (game.currentPhase != GamePhase.NIGHT) return SpiritRelayResult(false, "확성기는 밤에만 사용할 수 있습니다.")
        if (sender.state.isDead) return SpiritRelayResult(false, "사망한 플레이어는 확성기를 사용할 수 없습니다.")
        if (sender.state.isSilenced) return SpiritRelayResult(false, "유혹 상태에서는 확성기가 정상 출력되지 않습니다.")
        if (sender.allAbilities.none { it is Megaphone }) return SpiritRelayResult(false, "확성기 능력이 없습니다.")
        if (message.isBlank()) return SpiritRelayResult(false, "확성기 메시지를 입력해 주세요.")
        if (sender.member.id in game.usedMegaphonePlayerIds) return SpiritRelayResult(false, "확성기는 게임 중 1회만 사용할 수 있습니다.")
        if (game.megaphoneUsedTonight) return SpiritRelayResult(false, "이번 밤에는 이미 다른 플레이어가 확성기를 사용했습니다.")

        game.mainChannel?.createMessage("[확성기] ${sender.member.effectiveName}: $message")
        game.usedMegaphonePlayerIds += sender.member.id
        game.megaphoneUsedTonight = true
        return SpiritRelayResult(true, "확성기 메시지를 전송했습니다.")
    }

    private fun sendSecretLetter(game: Game, sender: PlayerData, target: PlayerData, message: String): SpiritRelayResult {
        if (game.currentPhase != GamePhase.NIGHT) return SpiritRelayResult(false, "밀서는 밤에만 보낼 수 있습니다.")
        if (sender.state.isDead) return SpiritRelayResult(false, "사망한 플레이어는 밀서를 보낼 수 없습니다.")
        if (sender.state.isSilenced) return SpiritRelayResult(false, "유혹 상태에서는 능력을 사용할 수 없습니다.")
        if (sender.allAbilities.none { it is SecretLetter }) return SpiritRelayResult(false, "밀서 능력이 없습니다.")
        if (sender.member.id in game.usedSecretLetterPlayerIds) return SpiritRelayResult(false, "밀서는 게임 중 1회만 보낼 수 있습니다.")
        if (target.state.isDead) return SpiritRelayResult(false, "사망한 플레이어에게는 밀서를 보낼 수 없습니다.")
        if (target.member.id == sender.member.id) return SpiritRelayResult(false, "자기 자신에게는 밀서를 보낼 수 없습니다.")
        if (message.isBlank()) return SpiritRelayResult(false, "밀서 내용을 입력해 주세요.")

        val formatted = "${sender.member.effectiveName} To ${target.member.effectiveName}\n$message"
        game.pendingLettersByRecipient.getOrPut(target.member.id) { mutableListOf() } += SecretLetterDelivery(
            title = "[밀서 도착]",
            content = formatted
        )
        game.pendingLettersByRecipient.getOrPut(sender.member.id) { mutableListOf() } += SecretLetterDelivery(
            title = "[밀서 전성]",
            content = formatted
        )
        game.usedSecretLetterPlayerIds += sender.member.id
        return SpiritRelayResult(true, "${target.member.effectiveName}님에게 밀서를 보냈습니다. 낮 시작 시 전달됩니다.")
    }

    private fun writeWill(game: Game, sender: PlayerData, message: String): SpiritRelayResult {
        if (game.currentPhase != GamePhase.NIGHT) return SpiritRelayResult(false, "유언은 밤에만 작성할 수 있습니다.")
        if (sender.state.isDead) return SpiritRelayResult(false, "사망한 플레이어는 유언을 작성할 수 없습니다.")
        if (sender.state.isSilenced) return SpiritRelayResult(false, "유혹 상태에서는 유언이 정상 출력되지 않습니다.")
        if (sender.allAbilities.none { it is Will }) return SpiritRelayResult(false, "유언 능력이 없습니다.")
        if (message.isBlank()) return SpiritRelayResult(false, "유언 내용을 입력해 주세요.")

        game.willByPlayerId[sender.member.id] = message
        return SpiritRelayResult(true, "유언을 작성했습니다. 이번 밤에 사망하면 공개됩니다.")
    }

    private fun castPerjuryVote(game: Game, sender: PlayerData, target: PlayerData): SpiritRelayResult {
        if (game.currentPhase != GamePhase.VOTE || game.defenseTargetId != null) {
            return SpiritRelayResult(false, "위증은 본투표 시간에만 사용할 수 있습니다.")
        }
        if (sender.state.isDead) return SpiritRelayResult(false, "사망한 플레이어는 위증을 사용할 수 없습니다.")
        if (sender.state.isSilenced) return SpiritRelayResult(false, "유혹 상태에서는 능력을 사용할 수 없습니다.")
        if (sender.allAbilities.none { it is Perjury }) return SpiritRelayResult(false, "위증 능력이 없습니다.")
        if (target.state.isDead) return SpiritRelayResult(false, "사망한 플레이어는 위증 대상으로 지정할 수 없습니다.")
        game.currentFakeVotes[sender.member.id] = target.member.id
        return SpiritRelayResult(true, "${target.member.effectiveName}님에게 가짜 투표를 행사했습니다. (집계에만 반영)")
    }

    private suspend fun sendPasswordChat(game: Game, sender: PlayerData, message: String): SpiritRelayResult {
        if (sender.state.isDead) return SpiritRelayResult(false, "사망한 플레이어는 암구호를 사용할 수 없습니다.")
        if (sender.state.isSilenced) return SpiritRelayResult(false, "유혹 상태에서는 능력을 사용할 수 없습니다.")
        if (sender.job !is Evil || sender.job is Villain) return SpiritRelayResult(false, "마피아 팀만 암구호를 사용할 수 있습니다.")
        if (sender.job !is Mafia && !hasContactedMafiaTeam(game, sender)) {
            return SpiritRelayResult(false, "보조 직업은 접선 후에만 암구호를 사용할 수 있습니다.")
        }
        if (sender.allAbilities.none { it is Password }) return SpiritRelayResult(false, "암구호 능력이 없습니다.")
        if (message.isBlank()) return SpiritRelayResult(false, "암구호 메시지를 입력해 주세요.")

        val mafiaChannel = game.mafiaChannel ?: return SpiritRelayResult(false, "마피아 채널을 찾을 수 없습니다.")
        mafiaChannel.createMessage("[암구호] ${sender.member.effectiveName}: $message")
        return SpiritRelayResult(true, "암구호 메시지를 전송했습니다.")
    }

    private fun hasContactedMafiaTeam(game: Game, player: PlayerData): Boolean {
        if (!isMafiaSupportJob(player)) return false
        if (player.state.hasContactedMafiaByInformant) return true

        return when (val job = player.job) {
            is Beastman -> player.state.isTamed
            is Godfather -> GodfatherContactPolicy.canContactMafia(game)
            is HitMan -> job.hasContactedMafia
            is Hostess -> job.hasContactedMafia
            is MadScientist -> player.state.hasContactedMafiaOnDeath
            is Spy -> job.hasContactedMafia
            is Swindler -> job.hasContactedMafia
            is Thief -> job.hasContactedMafia
            is Witch -> job.hasContactedMafia
            else -> false
        }
    }

    private fun isMafiaSupportJob(player: PlayerData): Boolean {
        val job = player.job
        return job is Evil && job !is Mafia && job !is Villain
    }

    private fun parseTargetPlayer(game: Game, raw: String): PlayerData? {
        val targetId = raw
            .replace("<@", "")
            .replace(">", "")
            .replace("!", "")
            .toULongOrNull()
            ?.let(::Snowflake)
        if (targetId != null) {
            return game.getPlayer(targetId)
        }

        return game.playerDatas.firstOrNull { it.member.effectiveName.equals(raw, ignoreCase = true) }
    }

    suspend fun relayShamanMessage(memberId: Snowflake, message: String): SpiritRelayResult {
        val game = currentGame ?: return SpiritRelayResult(false, "진행 중인 게임이 없습니다.")
        val sender = game.getPlayer(memberId) ?: return SpiritRelayResult(false, "게임 참가자만 사용할 수 있습니다.")
        if (message.isBlank()) return SpiritRelayResult(false, "메시지를 입력해 주세요.")
        if (sender.job !is Shaman || sender.state.isDead) return SpiritRelayResult(false, "생존한 영매만 사용할 수 있습니다.")
        if (game.currentPhase != GamePhase.NIGHT) return SpiritRelayResult(false, "접신 메시지는 밤에만 보낼 수 있습니다.")
        val deadChannel = game.deadChannel ?: return SpiritRelayResult(false, "죽은 자들의 채널을 찾을 수 없습니다.")

        runCatching {
            deadChannel.createMessage("[접신] ${sender.member.effectiveName}: $message")
        }
        return SpiritRelayResult(true, "죽은 자들의 채널에 접신 메시지를 보냈습니다.")
    }

    suspend fun relayShamanedMessage(
        memberId: Snowflake,
        channelId: Snowflake,
        message: String
    ): SpiritRelayResult {
        val game = currentGame ?: return SpiritRelayResult(false, "진행 중인 게임이 없습니다.")
        val sender = game.getPlayer(memberId) ?: return SpiritRelayResult(false, "게임 참가자만 사용할 수 있습니다.")
        if (message.isBlank()) return SpiritRelayResult(false, "메시지를 입력해 주세요.")
        if (channelId != Snowflake(GAME_DEAD_CHANNEL_ID)) return SpiritRelayResult(false, "죽은 자들의 채널에서만 사용할 수 있습니다.")
        if (!sender.state.isDead || !sender.state.isShamaned) return SpiritRelayResult(false, "성불된 사망자만 사용할 수 있습니다.")

        relayShamanedPlayerMessage(game, sender, message)
        return SpiritRelayResult(true, "강령 메시지를 전달했습니다.")
    }

    suspend fun relayNightPrivateChat(event: MessageCreateEvent) {
        val game = currentGame ?: return
        if (game.currentPhase != GamePhase.NIGHT) return

        val sender = event.member?.let { game.getPlayer(it.id) } ?: return
        if (sender.state.isDead) return
        if (event.message.content.isBlank()) return

        val channelId = event.message.channelId
        val mafiaChannelId = game.mafiaChannel?.id ?: Snowflake(GAME_MAFIA_CHANNEL_ID)
        val coupleChannelId = game.coupleChannel?.id ?: Snowflake(GAME_COUPLE_CHANNEL_ID)
        val parentChannelId = runCatching { event.message.getChannel().data.parentId }
            .getOrNull()

        val isNightPrivateChannel = channelId == mafiaChannelId ||
            channelId == coupleChannelId ||
            parentChannelId == mafiaChannelId ||
            parentChannelId == coupleChannelId
        if (!isNightPrivateChannel) return

        val watchers = game.playerDatas
            .asSequence()
            .filter { !it.state.isDead }
            .filter { it.member.id != sender.member.id }
            .filter { observer -> observer.allAbilities.any { it is Eavesdropping } }
            .filter { observer ->
                when (val observerJob = observer.job) {
                    is Police -> observerJob.eavesdroppingTargetId == sender.member.id
                    is Detective -> observerJob.fixedReasoningTargetId == sender.member.id
                    is Hacker -> observerJob.hackedTargetId == sender.member.id
                    else -> false
                }
            }
            .toList()

        if (watchers.isEmpty()) return

        watchers.forEach { watcher ->
            runCatching {
                watcher.member.getDmChannel().createMessage(
                    "[도청] ${sender.member.effectiveName}: ${event.message.content}"
                )
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

        val autopsyEavesdroppers = game.playerDatas
            .asSequence()
            .filter { !it.state.isDead }
            .filter { it.member.id != event.chatSender.member.id }
            .filter { player -> player.allAbilities.any { it is Eavesdropping } }
            .filter { player ->
                val policeJob = player.job as? Police ?: return@filter false
                policeJob.eavesdroppingTargetId == event.chatSender.member.id
            }
            .toList()

        autopsyEavesdroppers.forEach { watcher ->
            gameLoopScope.launch {
                runCatching {
                    watcher.member.getDmChannel().createMessage(
                        "[도청] ${event.chatSender.member.effectiveName}: ${event.chat}"
                    )
                }
            }
        }
    }

    private fun relayShamanedPlayerMessage(game: Game, sender: PlayerData, message: String) {
        val manifestShamans = game.playerDatas
            .asSequence()
            .filter { !it.state.isDead }
            .filter { it.job is Shaman }
            .filter { player -> player.allAbilities.any { it is Manifesto } }
            .toList()
        if (manifestShamans.isEmpty()) return

        manifestShamans.forEach { shaman ->
            gameLoopScope.launch {
                runCatching {
                    shaman.member.getDmChannel().createMessage("[강령] ${sender.member.effectiveName}: $message")
                }
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
        if (voterId in game.permanentlyDisenfranchisedVoters) return false
        if (game.activeThreatenedVoters.containsKey(voterId)) return false
        val targetId = runCatching { Snowflake(targetIdString) }.getOrNull() ?: return false
        val target = game.getPlayer(targetId) ?: return false
        if (target.state.isDead) return false
        if (
            GameLoopManager.isMadScientistDistortionHidden(voter) &&
            voter.member.id == target.member.id
        ) return false
        val dictatorshipPolitician = game.playerDatas
            .filter { !it.state.isDead && it.job !is Evil }
            .singleOrNull()
            ?.takeIf { it.job is Politician }
        if (dictatorshipPolitician != null && dictatorshipPolitician.member.id != voterId) return false

        game.currentMainVotes[voterId] = target.member.id.toString()
        if (game.dayCount == 1 && voter.job is Hostess && !game.hostessFirstVoteTargetByDay.containsKey(voterId)) {
            game.hostessFirstVoteTargetByDay[voterId] = target.member.id
        }
        return true
    }

    fun receivePerjuryVote(voterId: Snowflake, targetIdString: String): Boolean {
        val game = currentGame ?: return false
        if (game.currentPhase != GamePhase.VOTE || game.defenseTargetId != null) return false
        val voter = game.getPlayer(voterId) ?: return false
        if (voter.state.isDead) return false
        if (voter.state.isSilenced) return false
        if (voter.allAbilities.none { it is Perjury }) return false
        val targetId = runCatching { Snowflake(targetIdString) }.getOrNull() ?: return false
        val target = game.getPlayer(targetId) ?: return false
        if (target.state.isDead) return false

        game.currentFakeVotes[voterId] = target.member.id
        return true
    }

    // 찬반 투표 데이터 저장
    fun receiveProsConsVote(voterId: Snowflake, isPros: Boolean): Boolean {
        val game = currentGame ?: return false

        if (game.currentPhase != GamePhase.VOTE) return false
        val voter = game.getPlayer(voterId) ?: return false
        if (voter.state.isDead) return false
        if (voterId in game.permanentlyDisenfranchisedVoters) return false
        if (game.activeThreatenedVoters.containsKey(voterId)) return false
        val dictatorshipPolitician = game.playerDatas
            .filter { !it.state.isDead && it.job !is Evil }
            .singleOrNull()
            ?.takeIf { it.job is Politician }
        if (dictatorshipPolitician != null && dictatorshipPolitician.member.id != voterId) return false
        if (game.currentProsConsVotes.containsKey(voterId)) return false

        game.currentProsConsVotes[voterId] = isPros
        return true
    }
}
