package org.beobma.mafia42discordproject.game

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Member
import dev.kord.core.entity.channel.DmChannel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.VoiceChannel
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.component.actionRow
import dev.kord.rest.builder.message.embed
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.beobma.mafia42discordproject.discord.DiscordMessageManager
import org.beobma.mafia42discordproject.discord.InteractionErrorHandler
import org.beobma.mafia42discordproject.game.abilityselection.AbilityCommandGuide
import org.beobma.mafia42discordproject.game.abilityselection.AbilityPickButtonPayload
import org.beobma.mafia42discordproject.game.abilityselection.AbilitySelectionSession
import org.beobma.mafia42discordproject.game.abilityselection.AbilitySelectionSnapshot
import org.beobma.mafia42discordproject.game.abilityselection.selectAbilityRefreshGrants
import org.beobma.mafia42discordproject.game.annihilation.AnnihilationModeManager
import org.beobma.mafia42discordproject.game.annihilation.Capo
import org.beobma.mafia42discordproject.game.annihilation.Soldato
import org.beobma.mafia42discordproject.game.assignment.AssignmentPlayer
import org.beobma.mafia42discordproject.game.assignment.AssignmentTrace
import org.beobma.mafia42discordproject.game.assignment.AdministratorInspectionPolicy
import org.beobma.mafia42discordproject.game.assignment.JobAssignmentSimulationResult
import org.beobma.mafia42discordproject.game.assignment.RequiredRoleCounts
import org.beobma.mafia42discordproject.game.assignment.buildJobSelectionWeightByName
import org.beobma.mafia42discordproject.game.assignment.selectUniformMafiaPlayerIndices
import org.beobma.mafia42discordproject.game.communication.SpiritRelayResult
import org.beobma.mafia42discordproject.game.lobby.LobbyParticipation
import org.beobma.mafia42discordproject.game.lobby.LobbyParticipationResult
import org.beobma.mafia42discordproject.game.lobby.LobbyRefreshResult
import org.beobma.mafia42discordproject.game.lobby.LobbySelectionManager
import org.beobma.mafia42discordproject.game.mode.GameStartMode
import org.beobma.mafia42discordproject.game.player.BestJobPreferenceManager
import org.beobma.mafia42discordproject.game.player.JobPreferenceManager
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.replay.*
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.game.system.FrogCurseManager
import org.beobma.mafia42discordproject.game.system.MercenaryClientManager
import org.beobma.mafia42discordproject.game.system.SystemImage
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.JobManager
import org.beobma.mafia42discordproject.job.ability.*
import org.beobma.mafia42discordproject.job.ability.general.definition.list.administrator.Inspection
import org.beobma.mafia42discordproject.job.ability.general.definition.list.couple.CoupleAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.magician.Trick
import org.beobma.mafia42discordproject.job.ability.general.definition.list.nurse.Oath
import org.beobma.mafia42discordproject.job.ability.general.definition.list.other.Eavesdropping
import org.beobma.mafia42discordproject.job.ability.general.definition.list.shaman.Manifesto
import org.beobma.mafia42discordproject.job.ability.general.definition.list.shaman.ShamanAbilityOne
import org.beobma.mafia42discordproject.job.ability.general.evil.list.Password
import org.beobma.mafia42discordproject.job.ability.general.evil.list.godfather.GodfatherContactPolicy
import org.beobma.mafia42discordproject.job.ability.general.list.Megaphone
import org.beobma.mafia42discordproject.job.ability.general.list.Perjury
import org.beobma.mafia42discordproject.job.ability.general.list.SecretLetter
import org.beobma.mafia42discordproject.job.ability.general.list.Will
import org.beobma.mafia42discordproject.job.definition.Definition
import org.beobma.mafia42discordproject.job.definition.list.*
import org.beobma.mafia42discordproject.job.evil.Evil
import org.beobma.mafia42discordproject.job.evil.list.*
import org.beobma.mafia42discordproject.lavalink.LavalinkManager
import org.beobma.mafia42discordproject.web.WebNotepadServer
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

object GameManager {
    enum class VoteSubmissionResult {
        SUCCESS,
        THREATENED,
        FAILURE
    }

    @Volatile
    private var currentGame: Game? = null
    private var currentGuild: GuildBehavior? = null
    private val gameLoopScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var gameLoopJob: kotlinx.coroutines.Job? = null
    private val lobbySelectionManager = LobbySelectionManager()

    private const val FULL_GAME_PLAYER_COUNT = 8
    private const val EXTENDED_ROLE_RULE_START_COUNT = 9
    private const val MAX_GAME_PLAYER_COUNT = 20

    private const val EXTRA_ABILITY_SELECTION_REPEAT_COUNT = 3
    private const val EXTRA_ABILITY_OPTIONS_PER_ROUND = 3
    private const val GAME_MAIN_CHANNEL_ID = 1524098920576319518L
    private const val GAME_MAFIA_CHANNEL_ID = 1524098952499036320L
    private const val GAME_COUPLE_CHANNEL_ID = 1524098966139043860L
    private const val GAME_DEAD_CHANNEL_ID = 1524098974154227773L
    private const val SHAMAN_RELAY_COMMAND = "접신"
    private const val SHAMANED_RELAY_COMMAND = "강령"
    private const val GAME_CHANNEL_SPACER_LINES = 180
    private const val SEVEN_PLAYER_COUNT = 7
    private const val MAFIA_JOB_NAME = "마피아"
    private const val DOCTOR_JOB_NAME = "의사"
    private const val POLICE_JOB_NAME = "경찰"
    private const val INSPECTOR_JOB_NAME = "형사"
    private const val AGENT_JOB_NAME = "요원"
    private const val NURSE_JOB_NAME = "간호사"
    private const val CITIZEN_JOB_NAME = "시민"
    private const val COUPLE_JOB_NAME = "연인"
    private const val CABAL_JOB_NAME = "비밀결사"
    private const val MENTAL_PATIENT_JOB_NAME = MentalPatient.JOB_NAME
    private const val GAME_END_VOICE_DISCONNECT_DELAY_MS = 10_000L

    private val policeJobNames = setOf(POLICE_JOB_NAME, INSPECTOR_JOB_NAME, AGENT_JOB_NAME, "자경단원")
    private val pairAssignmentJobNames = setOf(COUPLE_JOB_NAME, CABAL_JOB_NAME)
    private val abilitySelectionSessions: MutableMap<Snowflake, AbilitySelectionSession> = ConcurrentHashMap()
    private val abilitySelectionSessionMutex = Mutex()
    private var abilitySelectionInitializationInProgress: Boolean = false
    private var abilitySelectionPendingUserIds: MutableSet<Snowflake> = mutableSetOf()

    suspend fun start(
        event: GuildChatInputCommandInteractionCreateEvent,
        mode: GameStartMode = GameStartMode.NORMAL
    ) {
        val interaction = event.interaction
        val guild = interaction.getGuild()

        val game = Game(
            playerDatas = mutableListOf(),
            guild = guild,
        )
        game.mode = mode

        game.start(event, mode)
    }

    suspend fun start(
        event: MessageCreateEvent,
        mode: GameStartMode = GameStartMode.NORMAL
    ) {
        val guild = event.getGuildOrNull() ?: return

        val game = Game(
            playerDatas = mutableListOf(),
            guild = guild,
        )
        game.mode = mode

        // 기존 로직 실행
        game.start(event, mode)
    }

    suspend fun markReady(event: GuildChatInputCommandInteractionCreateEvent): LobbyParticipationResult {
        val guild = event.interaction.getGuild()
        val member = event.interaction.user.asMember(guild.id)
        return markLobbyParticipation(guild, member, LobbyParticipation.READY)
    }

    suspend fun markReady(event: MessageCreateEvent): LobbyParticipationResult {
        val guild = event.getGuildOrNull()
            ?: return LobbyParticipationResult(false, "서버에서만 사용할 수 있는 명령어입니다.")
        val member = event.member
            ?: return LobbyParticipationResult(false, "서버 멤버 정보를 가져오지 못했습니다.")
        return markLobbyParticipation(guild, member, LobbyParticipation.READY)
    }

    suspend fun markSpectator(event: GuildChatInputCommandInteractionCreateEvent): LobbyParticipationResult {
        val guild = event.interaction.getGuild()
        val member = event.interaction.user.asMember(guild.id)
        return markLobbyParticipation(guild, member, LobbyParticipation.SPECTATOR)
    }

    suspend fun markSpectator(event: MessageCreateEvent): LobbyParticipationResult {
        val guild = event.getGuildOrNull()
            ?: return LobbyParticipationResult(false, "서버에서만 사용할 수 있는 명령어입니다.")
        val member = event.member
            ?: return LobbyParticipationResult(false, "서버 멤버 정보를 가져오지 못했습니다.")
        return markLobbyParticipation(guild, member, LobbyParticipation.SPECTATOR)
    }

    suspend fun refreshLobby(event: GuildChatInputCommandInteractionCreateEvent): LobbyParticipationResult {
        val guild = event.interaction.getGuild()
        val member = event.interaction.user.asMember(guild.id)
        return refreshLobby(guild, member)
    }

    suspend fun refreshLobby(event: MessageCreateEvent): LobbyParticipationResult {
        val guild = event.getGuildOrNull()
            ?: return LobbyParticipationResult(false, "서버에서만 사용할 수 있는 명령어입니다.")
        val member = event.member
            ?: return LobbyParticipationResult(false, "서버 멤버 정보를 가져오지 못했습니다.")
        return refreshLobby(guild, member)
    }

    private suspend fun refreshLobby(guild: Guild, member: Member): LobbyParticipationResult {
        if (currentGame != null) {
            return LobbyParticipationResult(false, "게임이 진행 중일 때는 대기 목록을 새로고침할 수 없습니다.")
        }

        val voiceChannelId = member.getVoiceStateOrNull()?.channelId
            ?: return LobbyParticipationResult(false, "먼저 음성채널에 접속한 뒤 사용해 주세요.")
        val voiceChannel = guild.getChannelOfOrNull<VoiceChannel>(voiceChannelId)
        val result = lobbySelectionManager.refreshParticipants(guild, voiceChannelId)

        return LobbyParticipationResult(
            true,
            buildLobbyRefreshMessage(voiceChannel?.mention, result)
        )
    }

    private suspend fun markLobbyParticipation(
        guild: Guild,
        member: Member,
        participation: LobbyParticipation
    ): LobbyParticipationResult {
        if (currentGame != null) {
            return LobbyParticipationResult(false, "게임이 진행 중일 때는 준비/관전 상태를 변경할 수 없습니다.")
        }

        return lobbySelectionManager.markParticipation(guild, member, participation)
    }

    private fun buildLobbyRefreshMessage(
        voiceChannelMention: String?,
        result: LobbyRefreshResult
    ): String {
        val members = result.members
        return buildString {
            appendLine("현재 음성채널 인원으로 대기 목록을 새로고침했습니다.")
            if (voiceChannelMention != null) {
                appendLine("음성채널: $voiceChannelMention")
            }
            appendLine("플레이어 ${members.readyMembers.size}명, 관전자 ${members.spectatorMembers.size}명")
            if (result.removedSelectionCount > 0) {
                appendLine("음성채널에 없는 ${result.removedSelectionCount}명의 선택을 제거했습니다.")
            }
            appendLine()
            appendLine("플레이어")
            appendLine(DiscordMessageManager.mentions(members.readyMembers).ifBlank { "없음" })
            if (members.spectatorMembers.isNotEmpty()) {
                appendLine()
                appendLine("관전자")
                append(DiscordMessageManager.mentions(members.spectatorMembers))
            }
        }
    }

    private suspend fun Game.start(
        event: GuildChatInputCommandInteractionCreateEvent,
        mode: GameStartMode
    ) {
        val interaction = event.interaction
        if (currentGame != null) {
            DiscordMessageManager.respondEphemeral(event, "이미 게임이 진행 중입니다.")
            return
        }

        val deferredResponse = interaction.deferPublicResponse()

        val guild = interaction.getGuild()
        val commandSender = interaction.user.asMember(guild.id)
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

        val lobbyMembers = lobbySelectionManager.collectMembers(guild, voiceChannelId)
        if (lobbyMembers.undecidedMembers.isNotEmpty()) {
            val failedMembers = sendReadyRequiredDm(lobbyMembers.undecidedMembers, "/준비", "/관전")
            deferredResponse.respond {
                content = buildReadyRequiredPublicMessage(lobbyMembers.undecidedMembers, failedMembers)
            }
            return
        }

        val playerMembers = lobbyMembers.readyMembers
        val spectatorMembers = lobbyMembers.spectatorMembers

        if (playerMembers.isEmpty()) {
            deferredResponse.respond {
                content = "플레이어로 준비한 사람이 없어 게임을 시작할 수 없습니다."
            }
            return
        }

        if (playerMembers.size > MAX_GAME_PLAYER_COUNT) {
            deferredResponse.respond {
                content = "최대 ${MAX_GAME_PLAYER_COUNT}명까지만 게임을 시작할 수 있습니다. 현재 플레이어: ${playerMembers.size}명"
            }
            return
        }

        if (mode == GameStartMode.ANNIHILATION && playerMembers.size < 4) {
            deferredResponse.respond {
                content = "말살 모드는 최소 4명(카포, 솔다토, 요원, 시민 1명)부터 시작할 수 있습니다."
            }
            return
        }

        val membersWithoutPreference = if (mode == GameStartMode.ANNIHILATION) {
            emptyList()
        } else {
            playerMembers.filter { member ->
                JobPreferenceManager.get(member.id.value).isNullOrEmpty()
            }
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
        this.replacePlayers(playerMembers.map(::PlayerData).toMutableList())
        this.replaceSpectators(spectatorMembers.toMutableList())
        this.initialPlayerCount = this.playerDatas.size
        this.voiceChannelId = voiceChannelId
        this.applyNextGameMafiaExecutionProtection()

        if (mode == GameStartMode.ANNIHILATION) {
            this.assignAnnihilationJobs()
            AnnihilationModeManager.initialize(this)
            GameReplayLogger.logGameStart(this, mode.displayName)
            setupGameChannels(this)
            sendGameChannelSpacer(this)
            AnnihilationModeManager.sendOpeningDms(this)
            this.isRunning = true
            gameLoopJob = gameLoopScope.launch {
                AnnihilationModeManager.runGameLoop(this@start)
            }
            lobbySelectionManager.clearSelections(guild.id, voiceChannelId)

            deferredResponse.respond {
                content = buildString {
                    appendLine("현재 음성채널: ${voiceChannel.mention}")
                    appendLine("플레이어 수: ${playerMembers.size}")
                    appendLine("관전자 수: ${spectatorMembers.size}")
                    appendLine("모드: ${mode.displayName}")
                    appendLine()
                    appendLine("플레이어")
                    appendLine(DiscordMessageManager.mentions(playerMembers))
                    if (spectatorMembers.isNotEmpty()) {
                        appendLine()
                        appendLine("관전자")
                        append(DiscordMessageManager.mentions(spectatorMembers))
                    }
                }
            }
            return
        }

        val assignmentPlayers = buildAssignmentPlayers(playerMembers)
        assignJobs(assignmentPlayers, mode)
        applyMadnessModeMentalPatientReplacements(assignmentPlayers, mode)
        this.applyAssignedJobs(assignmentPlayers)
        this.assignMentalPatientDisplayedJobs(assignmentPlayers)
        GameReplayLogger.logGameStart(this, mode.displayName)
        setupGameChannels(this)
        GameLoopManager.prepareGameChannels(this)
        sendGameChannelSpacer(this)
        initializeExtraAbilitySelectionForPlayers(assignmentPlayers)
        tryStartGameLoopWhenAbilitySelectionCompleted()
        lobbySelectionManager.clearSelections(guild.id, voiceChannelId)

        deferredResponse.respond {
            content = buildString {
                appendLine("현재 음성채널: ${voiceChannel.mention}")
                appendLine("플레이어 수: ${playerMembers.size}")
                appendLine("관전자 수: ${spectatorMembers.size}")
                appendLine("모드: ${mode.displayName}")
                appendLine()
                appendLine("플레이어")
                appendLine(DiscordMessageManager.mentions(playerMembers))
                if (spectatorMembers.isNotEmpty()) {
                    appendLine()
                    appendLine("관전자")
                    append(DiscordMessageManager.mentions(spectatorMembers))
                }
            }
        }
    }

    private suspend fun Game.start(event: MessageCreateEvent, mode: GameStartMode) {
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

        val lobbyMembers = lobbySelectionManager.collectMembers(guild, voiceChannelId)
        if (lobbyMembers.undecidedMembers.isNotEmpty()) {
            val failedMembers = sendReadyRequiredDm(lobbyMembers.undecidedMembers, "!준비", "!관전")
            event.message.channel.createMessage(
                buildReadyRequiredPublicMessage(lobbyMembers.undecidedMembers, failedMembers)
            )
            return
        }

        val playerMembers = lobbyMembers.readyMembers
        val spectatorMembers = lobbyMembers.spectatorMembers

        if (playerMembers.isEmpty()) {
            event.message.channel.createMessage("플레이어로 준비한 사람이 없어 게임을 시작할 수 없습니다.")
            return
        }

        if (playerMembers.size > MAX_GAME_PLAYER_COUNT) {
            event.message.channel.createMessage(
                "최대 ${MAX_GAME_PLAYER_COUNT}명까지만 게임을 시작할 수 있습니다. 현재 플레이어: ${playerMembers.size}명"
            )
            return
        }

        if (mode == GameStartMode.ANNIHILATION && playerMembers.size < 4) {
            event.message.channel.createMessage("말살 모드는 최소 4명(카포, 솔다토, 요원, 시민 1명)부터 시작할 수 있습니다.")
            return
        }

        val membersWithoutPreference = if (mode == GameStartMode.ANNIHILATION) {
            emptyList()
        } else {
            playerMembers.filter { member ->
                JobPreferenceManager.get(member.id.value).isNullOrEmpty()
            }
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
        this.replacePlayers(playerMembers.map(::PlayerData).toMutableList())
        this.replaceSpectators(spectatorMembers.toMutableList())
        this.initialPlayerCount = this.playerDatas.size
        this.voiceChannelId = voiceChannelId
        this.applyNextGameMafiaExecutionProtection()

        if (mode == GameStartMode.ANNIHILATION) {
            this.assignAnnihilationJobs()
            AnnihilationModeManager.initialize(this)
            GameReplayLogger.logGameStart(this, mode.displayName)
            setupGameChannels(this)
            sendGameChannelSpacer(this)
            AnnihilationModeManager.sendOpeningDms(this)
            this.isRunning = true
            gameLoopJob = gameLoopScope.launch {
                AnnihilationModeManager.runGameLoop(this@start)
            }
            lobbySelectionManager.clearSelections(guild.id, voiceChannelId)

            event.message.channel.createMessage(
                buildString {
                    appendLine("현재 음성채널: <#${voiceChannelId.value}>")
                    appendLine("플레이어 수: ${playerMembers.size}")
                    appendLine("관전자 수: ${spectatorMembers.size}")
                    appendLine("모드: ${mode.displayName}")
                    appendLine()
                    appendLine("플레이어")
                    appendLine(DiscordMessageManager.mentions(playerMembers))
                    if (spectatorMembers.isNotEmpty()) {
                        appendLine()
                        appendLine("관전자")
                        append(DiscordMessageManager.mentions(spectatorMembers))
                    }
                }
            )
            return
        }

        val assignmentPlayers = buildAssignmentPlayers(playerMembers)
        assignJobs(assignmentPlayers, mode)
        applyMadnessModeMentalPatientReplacements(assignmentPlayers, mode)
        this.applyAssignedJobs(assignmentPlayers)
        this.assignMentalPatientDisplayedJobs(assignmentPlayers)
        GameReplayLogger.logGameStart(this, mode.displayName)
        setupGameChannels(this)
        GameLoopManager.prepareGameChannels(this)
        sendGameChannelSpacer(this)
        initializeExtraAbilitySelectionForPlayers(assignmentPlayers)
        tryStartGameLoopWhenAbilitySelectionCompleted()
        lobbySelectionManager.clearSelections(guild.id, voiceChannelId)

        event.message.channel.createMessage(
            buildString {
                appendLine("플레이어 수: ${playerMembers.size}")
                appendLine("관전자 수: ${spectatorMembers.size}")
                appendLine("모드: ${mode.displayName}")
                appendLine()
                appendLine("플레이어")
                appendLine(DiscordMessageManager.mentions(playerMembers))
                if (spectatorMembers.isNotEmpty()) {
                    appendLine()
                    appendLine("관전자")
                    append(DiscordMessageManager.mentions(spectatorMembers))
                }
            }
        )
    }

    private suspend fun sendReadyRequiredDm(
        members: List<Member>,
        readyCommand: String,
        spectatorCommand: String
    ): List<Member> {
        val failedMembers = mutableListOf<Member>()

        members.forEach { member ->
            val message = "${member.mention} 게임에 참여하려면 `$readyCommand`, 관전하려면 `$spectatorCommand`을(를) 선택해 주세요."
            runCatching {
                member.getDmChannel().createMessage(message)
            }.onFailure { error ->
                failedMembers += member
                println("⚠️ ${member.effectiveName} 준비 요청 DM 전송 실패: ${error.message}")
            }
        }

        return failedMembers
    }

    private fun buildReadyRequiredPublicMessage(
        undecidedMembers: List<Member>,
        failedMembers: List<Member>
    ): String = buildString {
        appendLine("음성채널의 모든 사람이 준비 또는 관전 중 하나를 먼저 선택해야 합니다.")
        appendLine("아직 선택하지 않은 인원에게 준비 요청 DM을 보냈습니다.")
        appendLine()
        appendLine("아직 준비 또는 관전을 선택하지 않은 인원:")
        append(DiscordMessageManager.mentions(undecidedMembers))
        if (failedMembers.isNotEmpty()) {
            appendLine()
            appendLine()
            appendLine("DM을 보내지 못한 인원:")
            append(DiscordMessageManager.mentions(failedMembers))
        }
    }

    private fun buildAssignmentPlayers(members: List<Member>): MutableList<AssignmentPlayer> {
        val players = members.map { member ->
            val userId = member.id.value
            val savedBestJob = BestJobPreferenceManager.get(userId)
            val validatedBestJob = savedBestJob?.takeIf { bestJob ->
                BestJobPreferenceManager.isAllowedJob(userId, bestJob.name)
            }
            if (savedBestJob != null && validatedBestJob == null) {
                BestJobPreferenceManager.clear(userId)
            }
            AssignmentPlayer(
                memberId = member.id,
                name = member.effectiveName,
                preferences = JobPreferenceManager.get(userId).orEmpty(),
                bestJob = validatedBestJob
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

    private fun Game.assignAnnihilationJobs() {
        val shuffledPlayers = playerDatas.shuffled()
        shuffledPlayers.forEachIndexed { index, player ->
            player.job = when (index) {
                0 -> Capo()
                1 -> Soldato()
                2 -> Agent().also { it.abilities.clear(); it.extraAbilities.clear() }
                else -> Citizen()
            }
        }
        rebuildPlayerIndex()
    }

    private fun Game.applyNextGameMafiaExecutionProtection() {
        val protectedTargetId = MafiaExecutionProtectionManager.consume(guild.id.value)?.let(::Snowflake)
        mafiaExecutionProtectedTargetId = protectedTargetId
            ?.takeIf { targetId -> playerDatas.any { player -> player.member.id == targetId } }
    }

    private fun normalizeLowPlayerPreferences(
        players: MutableList<AssignmentPlayer>,
        trace: AssignmentTrace,
        mode: GameStartMode
    ) {
        if (players.size > SEVEN_PLAYER_COUNT) return
        val isSevenPlayerGame = players.size == SEVEN_PLAYER_COUNT

        val police = if (isSevenPlayerGame) {
            JobManager.findByName(POLICE_JOB_NAME) ?: run {
                trace.add("[7인 규칙] 경찰 직업 정의를 찾지 못해 요원 선호 변환을 건너뜁니다.")
                null
            }
        } else {
            null
        }
        val specialReplacementCandidates = getNonFixedJobCandidates(players.size)
            .filter { candidate -> candidate.name != CITIZEN_JOB_NAME }
        val usedSpecialReplacementJobNames = mutableSetOf<String>()

        fun addDistinct(target: MutableList<Job>, job: Job) {
            if (target.none { it.name == job.name }) {
                target += job
            }
        }

        fun pickSpecialReplacement(existingNames: Set<String>): Job? {
            val unusedCandidates = specialReplacementCandidates.filter { candidate ->
                candidate.name !in usedSpecialReplacementJobNames && candidate.name !in existingNames
            }
            val fallbackCandidates = specialReplacementCandidates.filter { candidate ->
                candidate.name !in existingNames
            }
            val picked = unusedCandidates.randomOrNull()
                ?: fallbackCandidates.randomOrNull()
                ?: specialReplacementCandidates.randomOrNull()
            if (picked != null) {
                usedSpecialReplacementJobNames += picked.name
            }
            return picked
        }

        if (isSevenPlayerGame) {
            val doctorRule = if (mode == GameStartMode.MADNESS) {
                "의사는 미치광이 모드에서 고정 배정하고"
            } else {
                "의사는 배정하지 않고"
            }
            trace.add("[7인 규칙] $doctorRule, 요원 선호는 경찰로, 간호사 선호는 중복 없는 특수 직업으로 변환합니다.")
        }
        trace.add("[7인 이하 규칙] 연인/비밀결사 선호는 무작위 특수 직업 선호로 변환합니다.")

        players.indices.forEach { index ->
            val player = players[index]
            val normalizedPreferences = mutableListOf<Job>()
            var nurseReplacementForPlayer: Job? = null
            val pairReplacementByName = mutableMapOf<String, Job>()

            player.preferences.forEach { preference ->
                when (preference.name) {
                    AGENT_JOB_NAME -> if (isSevenPlayerGame && police != null) {
                        addDistinct(normalizedPreferences, police)
                        trace.add("[7인 규칙] ${player.name}: 요원 선호 -> 경찰")
                    } else {
                        addDistinct(normalizedPreferences, preference)
                    }

                    NURSE_JOB_NAME -> if (isSevenPlayerGame) {
                        val replacement = nurseReplacementForPlayer
                            ?: pickSpecialReplacement(normalizedPreferences.map { it.name }.toSet())
                        if (replacement != null) {
                            nurseReplacementForPlayer = replacement
                            addDistinct(normalizedPreferences, replacement)
                            trace.add("[7인 규칙] ${player.name}: 간호사 선호 -> ${replacement.name}")
                        }
                    } else {
                        addDistinct(normalizedPreferences, preference)
                    }

                    in pairAssignmentJobNames -> {
                        val replacement = pairReplacementByName[preference.name]
                            ?: pickSpecialReplacement(normalizedPreferences.map { it.name }.toSet())
                        if (replacement != null) {
                            pairReplacementByName[preference.name] = replacement
                            addDistinct(normalizedPreferences, replacement)
                            trace.add("[7인 이하 규칙] ${player.name}: ${preference.name} 선호 -> ${replacement.name}")
                        }
                    }

                    else -> addDistinct(normalizedPreferences, preference)
                }
            }

            val normalizedBestJob = when (val bestJobName = player.bestJob?.name) {
                AGENT_JOB_NAME -> if (isSevenPlayerGame && police != null) police else player.bestJob
                NURSE_JOB_NAME -> if (isSevenPlayerGame) {
                    nurseReplacementForPlayer
                        ?: pickSpecialReplacement(normalizedPreferences.map { it.name }.toSet())
                        ?: player.bestJob
                } else {
                    player.bestJob
                }
                in pairAssignmentJobNames -> {
                    pairReplacementByName[bestJobName]
                        ?: pickSpecialReplacement(normalizedPreferences.map { it.name }.toSet())
                        ?: player.bestJob
                }
                else -> player.bestJob
            }

            players[index] = player.copy(
                preferences = normalizedPreferences,
                bestJob = normalizedBestJob
            )
        }
    }

    private fun buildNonFixedExcludedJobNames(playerCount: Int): Set<String> {
        val excluded = mutableSetOf(MAFIA_JOB_NAME, DOCTOR_JOB_NAME, MENTAL_PATIENT_JOB_NAME)
        excluded += policeJobNames
        if (playerCount == SEVEN_PLAYER_COUNT) {
            excluded += NURSE_JOB_NAME
        }
        if (playerCount <= SEVEN_PLAYER_COUNT) {
            excluded += pairAssignmentJobNames
        }
        return excluded
    }

    private fun getNonFixedJobCandidates(playerCount: Int): List<Job> {
        val excludedJobNames = buildNonFixedExcludedJobNames(playerCount)
        return JobManager.getAll().filter { candidate ->
            candidate !is Evil &&
                candidate.name !in excludedJobNames
        }
    }

    private fun applyMadnessModeMentalPatientReplacements(
        players: MutableList<AssignmentPlayer>,
        mode: GameStartMode
    ) {
        if (mode != GameStartMode.MADNESS) return

        val mentalPatientJob = JobManager.findByName(MENTAL_PATIENT_JOB_NAME) ?: return
        val replacementCount = mentalPatientCountFor(players.size)
        if (replacementCount <= 0) return

        val candidates = players
            .filter { player ->
                val assignedJob = player.assignedJob ?: return@filter false
                isMentalPatientReplacementCandidate(assignedJob, players.size)
            }
            .shuffled()
            .take(replacementCount)

        candidates.forEach { player ->
            player.assignedJob = mentalPatientJob
        }
    }

    private fun Game.assignMentalPatientDisplayedJobs(players: List<AssignmentPlayer>) {
        val assignmentByMemberId = players
            .mapNotNull { assignment -> assignment.memberId?.let { it to assignment } }
            .toMap()

        playerDatas.forEach { player ->
            val mentalPatient = player.job as? MentalPatient ?: return@forEach
            val assignment = assignmentByMemberId[player.member.id]
            mentalPatient.displayedJob = pickMentalPatientDisplayedJob(assignment, playerDatas.size)
        }
    }

    private fun pickMentalPatientDisplayedJob(
        assignment: AssignmentPlayer?,
        playerCount: Int
    ): Job? {
        val candidates = getMentalPatientDisplayJobCandidates(playerCount)
        if (candidates.isEmpty()) return null

        val preferredCandidates = assignment
            ?.preferences
            .orEmpty()
            .filter { preference -> candidates.any { candidate -> candidate.name == preference.name } }
            .distinctBy(Job::name)

        val picked = preferredCandidates.randomOrNull() ?: candidates.random()
        return JobManager.createByName(picked.name) ?: picked
    }

    private fun getMentalPatientDisplayJobCandidates(playerCount: Int): List<Job> {
        return getNonFixedJobCandidates(playerCount)
            .filter { candidate -> candidate.name != CITIZEN_JOB_NAME }
            .filterNot { candidate -> isPairAssignmentJob(candidate.name) }
    }

    private fun isMentalPatientReplacementCandidate(job: Job, playerCount: Int): Boolean {
        return job !is Evil &&
            job.name != CITIZEN_JOB_NAME &&
            job.name !in buildNonFixedExcludedJobNames(playerCount) &&
            !isPairAssignmentJob(job.name)
    }

    private fun mentalPatientCountFor(playerCount: Int): Int {
        return when {
            playerCount <= 6 -> 0
            playerCount in 7..9 -> 1
            else -> 2
        }
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
            MercenaryClientManager.assignRandomClient(this, mercenaryPlayer)
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
                        GameReplayLogger.logDirectMessage(game, doctorPlayer, "간호사의 선서를 받았습니다", "간호사 선서")
                        doctorPlayer.member.getDmChannel().createMessage("간호사의 선서를 받았습니다")
                    }
                }
        }
    }

    private suspend fun notifyAdministratorInspection(game: Game) {
        val requiredRoleCounts = resolveRequiredRoleCounts(game.playerDatas.size, game.mode)
        game.playerDatas
            .filter { player ->
                player.job is Administrator && player.allAbilities.any { it is Inspection }
            }
            .forEach { administratorPlayer ->
                val knownJobName = AdministratorInspectionPolicy.candidates(
                    jobs = game.playerDatas
                        .asSequence()
                        .filter { candidate -> candidate.member.id != administratorPlayer.member.id }
                        .mapNotNull(PlayerData::job),
                    requiredRoleCounts = requiredRoleCounts
                )
                    .randomOrNull()
                    ?.name

                val message = if (knownJobName != null) {
                    "${SystemImage.ADMINISTRATOR_NOTICE.imageUrl}\n감사 결과: 이번 게임에 $knownJobName 직업이 존재합니다."
                } else {
                    "${SystemImage.ADMINISTRATOR_NOTICE.imageUrl}\n감사 결과: 확인할 수 있는 다른 시민 팀 직업이 없습니다."
                }

                runCatching {
                    GameReplayMessenger.sendTrackedDm(game, administratorPlayer, message, "공무원 감사")
                }.onFailure { error ->
                    println("${administratorPlayer.member.effectiveName} 공무원 감사 DM 전송 실패: ${error.message}")
                }
            }
    }

    fun simulateJobAssignmentForVirtualPlayers(repeatCount: Int, playerCount: Int = FULL_GAME_PLAYER_COUNT): JobAssignmentSimulationResult {
        require(repeatCount > 0) { "repeatCount는 1 이상이어야 합니다." }
        require(playerCount >= 4) { "playerCount는 4 이상이어야 합니다." }

        val assignedJobCountByName = mutableMapOf<String, Int>()
        val outputLines = mutableListOf<String>()

        repeat(repeatCount) { runIndex ->
            val players = buildVirtualAssignmentPlayers(playerCount)
            val trace = assignJobs(players)

            outputLines += "## ${runIndex + 1}회차"
            outputLines += "[가상 선호/보석 설정]"
            players.forEach { player ->
                val assignedJobName = player.assignedJob?.name
                val highlightedPreferences = player.preferences.joinToString(", ") { preference ->
                    if (preference.name == assignedJobName) {
                        "🟩 **${preference.name}**(적중)"
                    } else {
                        preference.name
                    }
                }
                val bestJobName = player.bestJob?.name ?: "없음"
                val highlightedBestJob = if (bestJobName == assignedJobName) {
                    "🟪 **${bestJobName}**(적중)"
                } else {
                    bestJobName
                }
                outputLines += "- ${player.name}: 선호=[$highlightedPreferences], 보석=$highlightedBestJob"
            }

            outputLines += "[직업 배정 과정]"
            outputLines += trace.lines

            outputLines += "[직업 배정 결과]"
            players.forEach { player ->
                val assignedJobName = player.assignedJob?.name ?: "배정 실패"
                val preferenceHit = player.preferences.any { it.name == assignedJobName }
                val bestHit = player.bestJob?.name == assignedJobName
                val hitStatus = buildList {
                    if (preferenceHit) add("🟩 선호 적중")
                    if (bestHit) add("🟪 보석 적중")
                }.joinToString(" / ")
                val hitSuffix = if (hitStatus.isBlank()) "" else " [$hitStatus]"

                outputLines += "- ${player.name} -> $assignedJobName$hitSuffix"
                if (player.assignedJob != null) {
                    assignedJobCountByName[assignedJobName] = (assignedJobCountByName[assignedJobName] ?: 0) + 1
                }
            }
            outputLines += ""
        }

        return JobAssignmentSimulationResult(
            lines = outputLines,
            assignedJobCountByName = assignedJobCountByName.toSortedMap()
        )
    }

    private fun buildVirtualAssignmentPlayers(playerCount: Int): MutableList<AssignmentPlayer> {
        val allJobs = JobManager.getAll()
        val assistantPool = allJobs.filter { it is Evil && it.name != "마피아" && it.name != "악인" }
        val policePool = allJobs.filter { it.name in policeJobNames }
        val specialPool = allJobs.filter {
            it !is Evil && it.name != "의사" && it.name != "시민" && it.name !in policeJobNames
        }

        if (assistantPool.isEmpty() || policePool.isEmpty() || specialPool.size < 5) {
            throw IllegalStateException("가상 플레이어 선호 직업 풀을 구성할 수 없습니다.")
        }

        val doctor = JobManager.findByName("의사")

        return MutableList(playerCount) { index ->
            val assistant = assistantPool.random()
            val police = policePool.random()
            val specials = specialPool.shuffled().take(5)
            val preferences = buildList {
                add(assistant)
                add(police)
                addAll(specials)
            }

            val bestCandidates = (preferences + listOfNotNull(doctor)).distinctBy(Job::name)
            AssignmentPlayer(
                name = "가상플레이어${index + 1}",
                preferences = preferences,
                bestJob = bestCandidates.randomOrNull()
            )
        }
    }

    private fun assignJobs(
        players: MutableList<AssignmentPlayer>,
        mode: GameStartMode = GameStartMode.NORMAL
    ): AssignmentTrace {
        val trace = AssignmentTrace()
        val requiredCounts = resolveRequiredRoleCounts(players.size, mode)
        val allJobs = JobManager.getAll()

        val mafia = allJobs.firstOrNull { it.name == MAFIA_JOB_NAME } ?: run {
            trace.add("[오류] 마피아 직업 정의를 찾지 못했습니다.")
            return trace
        }
        val doctor = allJobs.firstOrNull { it.name == DOCTOR_JOB_NAME } ?: run {
            trace.add("[오류] 의사 직업 정의를 찾지 못했습니다.")
            return trace
        }
        normalizeLowPlayerPreferences(players, trace, mode)

        val policePool = allJobs.filter { job ->
            job.name in policeJobNames && !(players.size == SEVEN_PLAYER_COUNT && job.name == AGENT_JOB_NAME)
        }
        if (requiredCounts.policeCount > 0 && policePool.isEmpty()) {
            trace.add("[오류] 경찰 계열 직업 정의를 찾지 못했습니다.")
            return trace
        }

        val assistantPool = allJobs.filter { it is Evil && it.name != mafia.name }
        if (requiredCounts.assistantCount > 0 && assistantPool.isEmpty()) {
            trace.add("[오류] 보조 계열 직업 정의를 찾지 못했습니다.")
            return trace
        }

        trace.add("[1단계] 참여 인원: ${players.size}명")
        trace.add(
            "[1단계] 고정 배정 직업: 마피아 ${requiredCounts.mafiaCount}명, 보조계열 ${requiredCounts.assistantCount}명, 의사 ${requiredCounts.doctorCount}명, 경찰계열 ${requiredCounts.policeCount}명, 시민 ${requiredCounts.citizenCount}명"
        )

        val mafiaPlayerIndices = selectUniformMafiaPlayerIndices(
            playerCount = players.size,
            mafiaCount = requiredCounts.mafiaCount
        )
        val remainingPlayers = players
            .filterIndexed { index, _ -> index !in mafiaPlayerIndices }
            .toMutableList()

        trace.add(
            "[1단계] 마피아 균등 우선 배정: ${mafiaPlayerIndices.joinToString(", ") { index -> players[index].name }}"
        )

        val maxAttempts = 40
        repeat(maxAttempts) { attemptIndex ->
            players.forEach { it.assignedJob = null }
            mafiaPlayerIndices.forEach { index -> players[index].assignedJob = mafia }
            val attempt = attemptIndex + 1
            trace.add("[시도 $attempt/$maxAttempts] 마피아를 제외한 배정 조합 생성 시작")

            val selectedPoliceJob = if (requiredCounts.policeCount > 0) {
                pickPoliceJobByPreference(remainingPlayers, policePool, trace)
            } else {
                null
            }

            val selectedAssistantJob = if (requiredCounts.assistantCount > 0) {
                pickAssistantJobByPreference(remainingPlayers, assistantPool, trace)
            } else {
                null
            }

            val requiredFixedCount =
                requiredCounts.assistantCount +
                    requiredCounts.doctorCount +
                    requiredCounts.policeCount +
                    requiredCounts.citizenCount
            val slotCountForNonFixed = remainingPlayers.size - requiredFixedCount
            val nonFixedJobs = if (slotCountForNonFixed > 0) {
                selectNonFixedJobsByPreference(remainingPlayers, slotCountForNonFixed, trace)
            } else {
                trace.add("[2단계] 고정 직업만으로 슬롯이 구성됩니다.")
                emptyList()
            }

            val citizenJob = JobManager.findByName(CITIZEN_JOB_NAME) ?: doctor
            val fixedJobs = buildList {
                repeat(requiredCounts.assistantCount) {
                    add(requireNotNull(selectedAssistantJob) { "보조계열 고정 직업이 필요하지만 선택되지 않았습니다." })
                }
                repeat(requiredCounts.doctorCount) { add(doctor) }
                repeat(requiredCounts.policeCount) {
                    add(requireNotNull(selectedPoliceJob) { "경찰계열 고정 직업이 필요하지만 선택되지 않았습니다." })
                }
                repeat(requiredCounts.citizenCount) { add(citizenJob) }
            }

            val assigned = assignRemainingJobsInRandomPlayerOrder(
                players = remainingPlayers,
                fixedJobs = fixedJobs,
                nonFixedJobs = nonFixedJobs,
                trace = trace
            )
            if (assigned) {
                trace.add("[시도 $attempt/$maxAttempts] 배정 성공")
                return trace
            }
            trace.add("[시도 $attempt/$maxAttempts] 배정 실패, 재시도합니다.")
        }

        trace.add("[오류] 선호 직업 제약을 만족하는 배정을 찾지 못했습니다.")
        players.forEach { it.assignedJob = null }
        return trace
    }

    private fun selectNonFixedJobsByPreference(
        players: List<AssignmentPlayer>,
        slotCount: Int,
        trace: AssignmentTrace
    ): List<Job> {
        val excludedJobNames = buildNonFixedExcludedJobNames(players.size)
        val allCandidates = getNonFixedJobCandidates(players.size)
        val selectionWeightByName = buildJobSelectionWeightByName(players) { job ->
            job !is Evil && job.name !in excludedJobNames
        }

        val selected = mutableListOf<Job>()
        val selectedJobNames = mutableSetOf<String>()
        var assignedSlots = 0
        fun slotsFor(job: Job): Int = if (isPairAssignmentJob(job.name)) 2 else 1
        val preferredPlayerCountByName = players
            .flatMap { player -> player.preferences.map(Job::name).distinct() }
            .groupingBy { it }
            .eachCount()

        trace.add("[2단계] 비고정 슬롯 수: $slotCount")
        while (assignedSlots < slotCount) {
            val remaining = slotCount - assignedSlots
            val weightedEligible = allCandidates
                .filter { candidate ->
                    val requiredSlots = slotsFor(candidate)
                    if (candidate.name in selectedJobNames) return@filter false
                    if (requiredSlots > remaining) return@filter false
                    if (requiredSlots == 2 && (preferredPlayerCountByName[candidate.name] ?: 0) < 2) return@filter false
                    if ((selectionWeightByName[candidate.name] ?: 0) <= 0) return@filter false
                    true
                }
                .map { it to (selectionWeightByName[it.name] ?: 0) }

            if (weightedEligible.isEmpty()) break

            val picked = pickByWeight(weightedEligible) ?: weightedEligible.random().first
            val needed = slotsFor(picked)
            repeat(needed) { selected += picked }
            selectedJobNames += picked.name
            assignedSlots += needed
            trace.add("[2단계] 비고정 직업 선택: ${picked.name} (${assignedSlots}/$slotCount)")
        }

        if (assignedSlots < slotCount) {
            trace.add("[2단계] 경고: 선호 기반 비고정 슬롯을 모두 채우지 못했습니다. (${assignedSlots}/$slotCount)")
        }

        return selected
    }

    private fun assignRemainingJobsInRandomPlayerOrder(
        players: MutableList<AssignmentPlayer>,
        fixedJobs: List<Job>,
        nonFixedJobs: List<Job>,
        trace: AssignmentTrace
    ): Boolean {
        val slotCounter = (fixedJobs + nonFixedJobs)
            .groupingBy(Job::name)
            .eachCount()
            .toMutableMap()
        trace.add("[3단계] 마피아를 제외한 플레이어 랜덤 순회 배정 시작")

        players.forEach { it.assignedJob = null }
        val solved = solveAssignmentsWithBacktracking(players, slotCounter, trace)
        if (!solved) {
            players.forEach { it.assignedJob = null }
            trace.add("[3단계] 선호 제약으로 인해 유효 배정을 찾지 못했습니다.")
            return false
        }

        trace.add("[3단계] 마피아를 제외한 랜덤 순회 배정 완료")
        return true
    }

    private fun solveAssignmentsWithBacktracking(
        players: MutableList<AssignmentPlayer>,
        slotCounter: MutableMap<String, Int>,
        trace: AssignmentTrace
    ): Boolean {
        fun dfs(): Boolean {
            val unassigned = players.filter { it.assignedJob == null }
            if (unassigned.isEmpty()) return true

            val current = unassigned
                .shuffled()
                .minByOrNull { player ->
                    getAllowedJobNames(player).count { name -> (slotCounter[name] ?: 0) > 0 }
                } ?: return true

            val weightedCandidates = getAllowedJobNames(current)
                .filter { name -> (slotCounter[name] ?: 0) > 0 }
                .map { name ->
                    var weight = 3
                    if (name == current.bestJob?.name) {
                        weight *= 3
                    }
                    name to weight
                }
                .shuffled()
                .sortedByDescending { (_, weight) -> weight }

            for ((jobName, _) in weightedCandidates) {
                val job = JobManager.findByName(jobName) ?: continue

                if (!isPairAssignmentJob(jobName)) {
                    current.assignedJob = job
                    slotCounter[jobName] = (slotCounter[jobName] ?: 0) - 1
                    if (dfs()) return true
                    slotCounter[jobName] = (slotCounter[jobName] ?: 0) + 1
                    current.assignedJob = null
                    continue
                }

                if ((slotCounter[jobName] ?: 0) < 2) continue
                val partnerCandidates = unassigned
                    .filter { it != current && jobName in getAllowedJobNames(it) }
                    .shuffled()

                for (partner in partnerCandidates) {
                    current.assignedJob = job
                    partner.assignedJob = job
                    slotCounter[jobName] = (slotCounter[jobName] ?: 0) - 2
                    if (dfs()) return true
                    slotCounter[jobName] = (slotCounter[jobName] ?: 0) + 2
                    current.assignedJob = null
                    partner.assignedJob = null
                }
            }
            return false
        }

        val solved = dfs()
        if (solved) {
            players.forEach { player ->
                trace.add("[3단계] ${player.name} -> ${player.assignedJob?.name ?: "배정 실패"} (선호 제약 만족)")
            }
        }
        return solved
    }

    private fun isPairAssignmentJob(jobName: String): Boolean {
        return jobName in pairAssignmentJobNames
    }

    internal fun resolveRequiredRoleCounts(
        playerCount: Int,
        mode: GameStartMode = GameStartMode.NORMAL
    ): RequiredRoleCounts {
        return if (playerCount >= EXTENDED_ROLE_RULE_START_COUNT) {
            val mafiaCount = 2 + ((playerCount - EXTENDED_ROLE_RULE_START_COUNT) / 2)
            val citizenCount = 0
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
                7 -> RequiredRoleCounts(
                    mafiaCount = 1,
                    assistantCount = 1,
                    doctorCount = if (mode == GameStartMode.MADNESS) 1 else 0,
                    policeCount = 1
                )
                6 -> RequiredRoleCounts(
                    mafiaCount = 1,
                    assistantCount = 1,
                    doctorCount = if (mode == GameStartMode.MADNESS) 1 else 0,
                    policeCount = 1
                )
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
        val policePoolNames = policePool.map(Job::name).toSet()
        val selectionWeightByName = buildJobSelectionWeightByName(players) { job ->
            job.name in policePoolNames
        }
        val weightedPoliceJobs = policePool.map { policeJob ->
            val weight = selectionWeightByName[policeJob.name] ?: 0
            policeJob to weight
        }
        val weightSummary = weightedPoliceJobs.joinToString(", ") { (job, weight) -> "${job.name}($weight)" }
        trace.add("[1단계] 경찰계열 후보 가중치(선호+보석): $weightSummary")

        val picked = pickByWeight(weightedPoliceJobs) ?: policePool.random()
        trace.add("[1단계] 경찰계열 고정 직업 선택: ${picked.name}")
        return picked
    }

    private fun pickAssistantJobByPreference(
        players: List<AssignmentPlayer>,
        assistantPool: List<Job>,
        trace: AssignmentTrace
    ): Job {
        val assistantPoolNames = assistantPool.map(Job::name).toSet()
        val selectionWeightByName = buildJobSelectionWeightByName(players) { job ->
            job.name in assistantPoolNames
        }
        val weightedAssistantJobs = assistantPool.map { assistantJob ->
            val weight = selectionWeightByName[assistantJob.name] ?: 0
            assistantJob to weight
        }
        val weightSummary = weightedAssistantJobs.joinToString(", ") { (job, weight) -> "${job.name}($weight)" }
        trace.add("[1단계] 보조계열 후보 가중치(선호+보석): $weightSummary")

        val picked = pickByWeight(weightedAssistantJobs) ?: assistantPool.random()
        trace.add("[1단계] 보조계열 고정 직업 선택: ${picked.name}")
        return picked
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

    private fun getAllowedJobNames(player: AssignmentPlayer): List<String> {
        return (player.preferences.map(Job::name) + DOCTOR_JOB_NAME).distinct()
    }

    private suspend fun Game.initializeExtraAbilitySelectionForPlayers(players: List<AssignmentPlayer>) {
        val preparedSessions = mutableMapOf<Snowflake, AbilitySelectionSession>()
        val refreshGrantsByPlayerId = selectAbilityRefreshGrants(
            playerCount = playerDatas.size,
            mafiaPlayerIds = playerDatas
                .filter { player -> player.job is Mafia }
                .map { player -> player.member.id },
            previousPublicTargetId = mafiaExecutionProtectedTargetId
        )
        abilitySelectionRefreshLimitsByPlayerId.clear()
        abilitySelectionRefreshLimitsByPlayerId.putAll(
            refreshGrantsByPlayerId.mapValues { (_, grant) -> grant.refreshCount }
        )

        playerDatas.forEach { player ->
            val job = player.job ?: return@forEach
            val selectionJob = getAbilitySelectionDisplayJob(player) ?: job
            val pool = buildAvailableExtraAbilityPool(job, selectionJob)

            val session = AbilitySelectionSession(
                playerJob = job,
                availablePool = pool,
                maxRefreshes = abilitySelectionRefreshLimitsByPlayerId[player.member.id] ?: 0
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
                    val displayJob = getAbilitySelectionDisplayJob(player) ?: job
                    val session = preparedSessions[player.member.id]
                    runCatching {
                        val dmChannel = player.member.getDmChannel()
                        val ownedAbilityMessage = buildString {
                            displayJob.jobImage
                                ?.takeIf { it.isNotBlank() }
                                ?.let { appendLine(it) }
                            appendAbilityImages(this, displayJob.abilities)
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
                                GameReplayMessenger.sendTrackedDm(this@initializeExtraAbilitySelectionForPlayers, player, cabalRoleMessage, "비밀결사 역할")
                            }
                        }

                        buildMafiaTeammateMessage(this@initializeExtraAbilitySelectionForPlayers, player)
                            ?.let { GameReplayMessenger.sendTrackedDm(this@initializeExtraAbilitySelectionForPlayers, player, it, "마피아 팀 안내") }
                        buildCouplePartnerMessage(this@initializeExtraAbilitySelectionForPlayers, player)
                            ?.let { GameReplayMessenger.sendTrackedDm(this@initializeExtraAbilitySelectionForPlayers, player, it, "연인 안내") }

                        if (session != null) {
                            val refreshNotification = refreshGrantsByPlayerId[player.member.id]
                                ?.reasons
                                ?.sortedBy { reason -> reason.ordinal }
                                ?.mapNotNull { reason -> reason.notificationMessage }
                                ?.joinToString("\n")
                                .orEmpty()
                            if (refreshNotification.isNotBlank()) {
                                GameReplayMessenger.sendTrackedDm(
                                    this@initializeExtraAbilitySelectionForPlayers,
                                    player,
                                    refreshNotification,
                                    "능력 새로고침 기회 증가"
                                )
                            }
                            sendAbilitySelectionPrompt(dmChannel, player.member.id, session)
                        } else {
                            val message = "ℹ️ 선택 가능한 부가 능력이 없어 능력 선택 단계를 건너뜁니다."
                            GameReplayLogger.logDirectMessage(this@initializeExtraAbilitySelectionForPlayers, player, message, "능력 선택 안내")
                            dmChannel.createMessage(message)
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

    private fun getAbilitySelectionDisplayJob(player: PlayerData): Job? {
        return (player.job as? MentalPatient)?.displayedJob
    }

    private fun buildAvailableExtraAbilityPool(playerJob: Job, selectionJob: Job): MutableList<Ability> {
        return AbilityManager.getAvailableExtraAbilitiesFor(selectionJob)
            .asSequence()
            .filter { ability ->
                playerJob !is MentalPatient || MentalPatient.isActuallyAvailableUtilityAbility(ability)
            }
            .distinctBy(Ability::name)
            .shuffled()
            .toMutableList()
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

    private fun buildFinalJobAbilityReplayMessage(player: PlayerData): String {
        val job = player.job ?: return ""
        val displayJob = getAbilitySelectionDisplayJob(player) ?: job
        val builder = StringBuilder()
        val appendedImageUrls = linkedSetOf<String>()

        fun appendImage(imageUrl: String?) {
            val normalized = imageUrl?.trim().orEmpty()
            if (normalized.isBlank() || !appendedImageUrls.add(normalized)) return
            builder.appendLine(normalized)
        }

        appendImage(displayJob.jobImage)
        displayJob.abilities.forEach { ability -> appendImage(ability.image) }
        job.extraAbilities.forEach { ability -> appendImage(ability.image) }

        return builder.toString().trim()
    }

    private fun logFinalJobAbilityReplayMessages(game: Game) {
        val shouldLog = synchronized(game) {
            if (game.hasLoggedFinalJobAbilityReplay) {
                false
            } else {
                game.hasLoggedFinalJobAbilityReplay = true
                true
            }
        }
        if (!shouldLog) return

        game.playerDatas.forEach { player ->
            val message = buildFinalJobAbilityReplayMessage(player)
            if (message.isNotBlank()) {
                GameReplayLogger.logDirectMessage(game, player, message, "직업/능력 안내")
            }
        }
    }

    private suspend fun sendAbilityCommandGuides(game: Game) = coroutineScope {
        game.playerDatas.forEach { player ->
            launch {
                val message = buildAbilityCommandGuideMessage(player) ?: return@launch
                runCatching {
                    val dmChannel = player.member.getDmChannel()
                    splitDiscordMessage(message).forEach { chunk ->
                        dmChannel.createMessage(chunk)
                    }
                }.onFailure { error ->
                    println("⚠️ ${player.member.effectiveName} 능력 사용 안내 DM 전송 실패: ${error.message}")
                }
            }
        }
    }

    private fun buildAbilityCommandGuideMessage(player: PlayerData): String? {
        val job = player.job ?: return null
        val displayJob = getAbilitySelectionDisplayJob(player) ?: job
        val guides = commandGuideAbilities(player)
            .flatMap(::buildAbilityCommandGuides)
            .distinctBy { "${it.abilityName}|${it.timing}|${it.command.orEmpty()}" }
        if (guides.isEmpty()) return null

        return buildString {
            appendLine("능력 사용 안내")
            appendLine("직업: ${displayJob.name}")
            appendLine()
            guides.forEach { guide ->
                appendLine("[${guide.abilityName}]")
                appendLine("시점: ${guide.timing}")
                appendLine("명령어: ${guide.command ?: "없음 (자동/패시브 능력)"}")
                appendLine("요약: ${guide.summary}")
                appendLine()
            }
            append("슬래시 명령어 옵션 이름은 Discord 입력창의 자동완성을 따라 선택하면 됩니다.")
        }.trim()
    }

    private fun splitDiscordMessage(message: String, maxLength: Int = 1900): List<String> {
        if (message.length <= maxLength) return listOf(message)

        val chunks = mutableListOf<String>()
        var current = StringBuilder()
        message.lineSequence().forEach { line ->
            val nextLength = current.length + line.length + 1
            if (current.isNotEmpty() && nextLength > maxLength) {
                chunks += current.toString().trimEnd()
                current = StringBuilder()
            }

            if (line.length > maxLength) {
                line.chunked(maxLength).forEach { part ->
                    if (current.isNotEmpty()) {
                        chunks += current.toString().trimEnd()
                        current = StringBuilder()
                    }
                    chunks += part
                }
            } else {
                current.appendLine(line)
            }
        }
        if (current.isNotBlank()) {
            chunks += current.toString().trimEnd()
        }
        return chunks
    }

    private fun commandGuideAbilities(player: PlayerData): List<Ability> {
        return (player.job as? MentalPatient)?.activeAbilitySourceAbilities()
            ?: player.allAbilities
    }

    private fun buildAbilityCommandGuides(ability: Ability): List<AbilityCommandGuide> {
        return when (ability) {
            is Megaphone -> listOf(
                AbilityCommandGuide(
                    abilityName = ability.name,
                    timing = "밤",
                    command = "/확성기 {내용}",
                    summary = briefAbilitySummary(ability)
                )
            )
            is SecretLetter -> listOf(
                AbilityCommandGuide(
                    abilityName = ability.name,
                    timing = "밤",
                    command = "/밀서 {대상} {내용}",
                    summary = briefAbilitySummary(ability)
                )
            )
            is Will -> listOf(
                AbilityCommandGuide(
                    abilityName = ability.name,
                    timing = "밤",
                    command = "/유언 {내용}",
                    summary = briefAbilitySummary(ability)
                )
            )
            is Perjury -> listOf(
                AbilityCommandGuide(
                    abilityName = ability.name,
                    timing = "본투표",
                    command = "/위증 {대상}",
                    summary = briefAbilitySummary(ability)
                )
            )
            is Password -> listOf(
                AbilityCommandGuide(
                    abilityName = ability.name,
                    timing = "상시 (마피아팀 전용, 보조 직업은 접선 후)",
                    command = "/암구호 {내용}",
                    summary = briefAbilitySummary(ability)
                )
            )
            is ActiveAbility -> listOf(
                AbilityCommandGuide(
                    abilityName = ability.name,
                    timing = if (ability is Trick) "낮/투표" else phaseLabel(ability.usablePhase),
                    command = activeAbilityCommandUsage(ability),
                    summary = briefAbilitySummary(ability)
                )
            )
            else -> listOf(
                AbilityCommandGuide(
                    abilityName = ability.name,
                    timing = "자동 발동",
                    command = null,
                    summary = briefAbilitySummary(ability)
                )
            )
        }
    }

    private fun activeAbilityCommandUsage(ability: ActiveAbility): String {
        return when (ability.name) {
            "조회" -> "/사용 ${ability.name} {직업}"
            "청부" -> "/사용 ${ability.name} {대상} {직업}"
            "최면 해제" -> "/사용 ${ability.name}"
            else -> "/사용 ${ability.name} {대상}"
        }
    }

    private fun phaseLabel(phase: GamePhase): String = when (phase) {
        GamePhase.DAY -> "낮"
        GamePhase.NIGHT -> "밤"
        GamePhase.DAWN -> "새벽"
        GamePhase.VOTE -> "투표/최후 변론"
        GamePhase.END -> "게임 종료"
    }

    private fun briefAbilitySummary(ability: Ability): String {
        val normalized = ability.description
            .replace("\r", " ")
            .replace("\n", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (normalized.length <= 120) return normalized
        return normalized.take(117).trimEnd() + "..."
    }

    private fun assignVirtualPlayerExtraAbilities(players: List<AssignmentPlayer>) {
        val virtualPlayers = players.filter { it.memberId == null }
        virtualPlayers.forEach { virtualPlayer ->
            val assignedJob = virtualPlayer.assignedJob ?: return@forEach
            val pool = buildAvailableExtraAbilityPool(assignedJob, assignedJob)

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
            if (
                playerJob !is MentalPatient &&
                selectedUniqueAbility != null &&
                playerJob.abilities.none { it.name == selectedUniqueAbility.name }
            ) {
                playerJob.abilities.add(selectedUniqueAbility)
            }
            if (playerJob.extraAbilities.none { it.name == pickedAbility.name }) {
                playerJob.extraAbilities.add(pickedAbility)
            }
            session.selected += pickedAbility
            session.availablePool.removeAll { ability -> ability.name == pickedAbility.name }
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
                    tryStartGameLoopWhenAbilitySelectionCompleted()
                }.onFailure { error ->
                    println("⚠️ 능력 선택 종료 후 게임 루프 시작 실패: ${error.message}")
                }
            }
        }

        return resultMessage
    }

    suspend fun refreshExtraAbilityOptions(userId: Snowflake): String {
        var shouldTryStartGameLoop = false
        val resultMessage = abilitySelectionSessionMutex.withLock {
            val session = abilitySelectionSessions[userId]
                ?: restoreAbilitySelectionSessionLocked(userId)
                ?: return@withLock "현재 부가 능력 선택 단계가 아니거나 이미 선택이 완료되었습니다."

            if (session.completedRounds >= EXTRA_ABILITY_SELECTION_REPEAT_COUNT || session.currentOptions.isEmpty()) {
                return@withLock "이미 부가 능력 선택이 완료되었습니다."
            }

            if (session.maxRefreshes <= 0) {
                return@withLock "현재 직업은 능력 선택지를 새로고침할 수 없습니다."
            }

            if (session.refreshesUsed >= session.maxRefreshes) {
                return@withLock "능력 선택지 새로고침 기회를 모두 사용했습니다."
            }

            if (!canRefreshAbilityOptions(session)) {
                return@withLock "새로고침으로 다시 뽑을 수 있는 능력이 없습니다."
            }

            val removedOptionNames = session.currentOptions.map(Ability::name).toSet()
            session.availablePool.removeAll { ability -> ability.name in removedOptionNames }
            session.refreshesUsed += 1
            session.currentOptions = drawAbilityOptions(session)

            if (session.currentOptions.isEmpty()) {
                abilitySelectionSessions.remove(userId)
                abilitySelectionPendingUserIds.remove(userId)
                shouldTryStartGameLoop = true
                return@withLock buildString {
                    appendLine("능력 선택지를 새로고침했지만 추가로 제시할 수 있는 능력이 없습니다.")
                    append("현재 선택 능력: ${session.selected.joinToString(", ") { it.name }}")
                }
            }

            val remainingRefreshes = session.maxRefreshes - session.refreshesUsed
            "능력 선택지를 새로고침했습니다. 남은 새로고침: ${remainingRefreshes}회"
        }

        if (shouldTryStartGameLoop) {
            currentGuild?.let {
                runCatching {
                    tryStartGameLoopWhenAbilitySelectionCompleted()
                }.onFailure { error ->
                    println("⚠️ 능력 선택 새로고침 종료 후 게임 루프 시작 실패: ${error.message}")
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
        val selectionJob = getAbilitySelectionDisplayJob(player) ?: playerJob
        val alreadySelectedNames = playerJob.extraAbilities.map(Ability::name).toSet()
        val completedRounds = minOf(playerJob.extraAbilities.size, EXTRA_ABILITY_SELECTION_REPEAT_COUNT)
        if (completedRounds >= EXTRA_ABILITY_SELECTION_REPEAT_COUNT) return null

        val availablePool = buildAvailableExtraAbilityPool(playerJob, selectionJob)
            .filterNot { ability -> ability.name in alreadySelectedNames }
            .toMutableList()
        if (availablePool.isEmpty()) return null

        val restoredSession = AbilitySelectionSession(
            playerJob = playerJob,
            availablePool = availablePool,
            maxRefreshes = game.abilitySelectionRefreshLimitsByPlayerId[player.member.id] ?: 0,
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
        val selectedNames = session.selected.map(Ability::name).toSet()
        session.availablePool.removeAll { ability -> ability.name in selectedNames }
        if (session.availablePool.isEmpty()) return emptyList()

        val count = minOf(EXTRA_ABILITY_OPTIONS_PER_ROUND, session.availablePool.size)
        return session.availablePool.shuffled().take(count)
    }

    private fun canRefreshAbilityOptions(session: AbilitySelectionSession): Boolean {
        if (session.maxRefreshes <= 0 || session.refreshesUsed >= session.maxRefreshes) return false
        val selectedNames = session.selected.map(Ability::name).toSet()
        val currentOptionNames = session.currentOptions.map(Ability::name).toSet()
        return session.availablePool.any { ability ->
            ability.name !in selectedNames && ability.name !in currentOptionNames
        }
    }

    private fun buildAbilitySelectionGuideMessage(): String {
        return buildString {
            append("능력 중 하나를 선택하세요.")
        }
    }

    fun parseAbilityPickButtonId(componentId: String): AbilityPickButtonPayload? {
        val prefix = "ability_pick_"
        val refreshPrefix = "ability_refresh_"
        if (componentId.startsWith(refreshPrefix)) {
            val ownerId = componentId.removePrefix(refreshPrefix).toULongOrNull()?.let(::Snowflake) ?: return null
            return AbilityPickButtonPayload(ownerUserId = ownerId, pickNumber = 0, isRefresh = true)
        }
        if (!componentId.startsWith(prefix)) return null
        val payload = componentId.removePrefix(prefix)
        val parts = payload.split("_")
        if (parts.size != 2) return null

        val ownerId = parts[0].toULongOrNull()?.let(::Snowflake) ?: return null
        val pickNumber = parts[1].toIntOrNull()?.takeIf { it in 1..EXTRA_ABILITY_OPTIONS_PER_ROUND } ?: return null
        return AbilityPickButtonPayload(ownerUserId = ownerId, pickNumber = pickNumber)
    }

    fun abilityPickButtonId(userId: Snowflake, pickNumber: Int): String = "ability_pick_${userId.value}_$pickNumber"

    fun abilityRefreshButtonId(userId: Snowflake): String = "ability_refresh_${userId.value}"

    suspend fun getAbilitySelectionSession(userId: Snowflake): AbilitySelectionSnapshot? {
        val session = abilitySelectionSessionMutex.withLock {
            abilitySelectionSessions[userId]
        } ?: return null
        return AbilitySelectionSnapshot(
            guideMessage = buildAbilitySelectionGuideMessage(),
            optionCount = session.currentOptions.size
        )
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
            append(buildAbilitySelectionGuideMessage())
        }.trim()

        dmChannel.createMessage {
            this.content = content
            actionRow {
                session.currentOptions.forEachIndexed { index, _ ->
                    interactionButton(ButtonStyle.Primary, abilityPickButtonId(userId, index + 1)) {
                        label = "${index + 1}번 선택"
                    }
                }
                if (canRefreshAbilityOptions(session)) {
                    val remainingRefreshes = session.maxRefreshes - session.refreshesUsed
                    interactionButton(ButtonStyle.Secondary, abilityRefreshButtonId(userId)) {
                        label = "새로고침 (${remainingRefreshes}회)"
                    }
                }
            }
        }
    }

    fun getCurrentGameFor(userId: Snowflake): Game? =
        currentGame?.takeIf { game -> game.getPlayer(userId) != null }

    fun isInCurrentGame(userId: Snowflake): Boolean =
        currentGame?.playerDatas?.any { it.member.id == userId } == true

    suspend fun handleAnnihilationCommand(
        userId: Snowflake,
        action: String?,
        secret: String?,
        note: String?,
        location: String?,
        location2: String?,
        location3: String?,
        anonymousMessage: String?,
        anonymousAbility: String?,
        targetId: Snowflake?,
        target2Id: Snowflake?
    ): String {
        val game = currentGame ?: return "진행 중인 게임이 없습니다."
        if (game.mode != GameStartMode.ANNIHILATION) return "현재 게임은 말살 모드가 아닙니다."
        return AnnihilationModeManager.handleCommand(
            game = game,
            userId = userId,
            action = action,
            secret = secret,
            note = note,
            location = location,
            location2 = location2,
            location3 = location3,
            anonymousMessage = anonymousMessage,
            anonymousAbility = anonymousAbility,
            targetId = targetId,
            target2Id = target2Id
        )
    }

    suspend fun receiveAnnihilationMoveSelection(userId: Snowflake, componentId: String): String {
        val game = currentGame ?: return "진행 중인 게임이 없습니다."
        if (game.mode != GameStartMode.ANNIHILATION) return "현재 게임은 말살 모드가 아닙니다."
        return AnnihilationModeManager.receiveMoveSelection(game, userId, componentId)
    }

    suspend fun receiveAnnihilationVote(voterId: Snowflake, rawValue: String?): String {
        val game = currentGame ?: return "진행 중인 게임이 없습니다."
        if (game.mode != GameStartMode.ANNIHILATION) return "현재 게임은 말살 모드가 아닙니다."
        return AnnihilationModeManager.receiveVote(game, voterId, rawValue)
    }

    fun receiveAnnihilationAgentInvestigation(
        userId: Snowflake,
        componentId: String,
        rawTargetId: String?
    ): String {
        val game = currentGame ?: return "진행 중인 게임이 없습니다."
        if (game.mode != GameStartMode.ANNIHILATION) return "현재 게임은 말살 모드가 아닙니다."
        return AnnihilationModeManager.receiveAgentInvestigation(game, userId, componentId, rawTargetId)
    }

    private suspend fun tryStartGameLoopWhenAbilitySelectionCompleted() {
        val game = currentGame ?: return
        val canStart = abilitySelectionSessionMutex.withLock {
            !abilitySelectionInitializationInProgress && abilitySelectionPendingUserIds.isEmpty()
        }
        if (!canStart) return
        if (game.isRunning) return
        if (gameLoopJob?.isActive == true) return

        logFinalJobAbilityReplayMessages(game)
        sendAbilityCommandGuides(game)
        notifyAdministratorInspection(game)
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

        finalizeGameState(gameToStop, endReason = "FORCED_STOP", winningTeamName = null)
    }

    suspend fun stop(event: MessageCreateEvent) {
        val gameToStop = currentGame

        if (gameToStop == null) {
            event.message.channel.createMessage("진행 중인 게임이 없습니다.")
            return
        }

        val mention = event.message.author?.mention.orEmpty()
        event.message.channel.createMessage("${mention}이(가) 게임을 종료했습니다.")

        finalizeGameState(gameToStop, endReason = "FORCED_STOP", winningTeamName = null)
    }

    suspend fun finalizeGameState(
        gameToStop: Game,
        endReason: String,
        winningTeamName: String?,
        cancelLoopJob: Boolean = true
    ) {
        registerNextGameMafiaExecutionProtection(gameToStop)
        scheduleDelayedVoiceDisconnect(gameToStop)
        gameToStop.isRunning = false
        if (gameToStop.replayLogs.none { it.type == ReplayLogType.GAME_END }) {
            GameReplayLogger.logGameEnd(gameToStop, endReason = endReason, winningTeamName = winningTeamName)
        }
        releaseAllPlayerMutes(gameToStop)

        if (cancelLoopJob) {
            gameLoopJob?.cancelAndJoin()
        }
        gameLoopJob = null

        GameLoopManager.clearTimeThread()
        abilitySelectionSessions.clear()

        val replayRenderData = GameReplayRenderDataStore.snapshot(
            game = gameToStop,
            endReason = endReason,
            winningTeamName = winningTeamName
        )
        GameArchiveManager.archive(gameToStop, endReason = endReason, winningTeamName = winningTeamName)
        GameReplayRenderDataStore.save(replayRenderData)
        GameReplaySender.sendReplay(gameToStop, replayRenderData)
        WebNotepadServer.invalidateGame(gameToStop)
        currentGame = null
        currentGuild = null

        gameToStop.mainChannel = null
        gameToStop.mafiaChannel = null
        gameToStop.coupleChannel = null
        gameToStop.deadChannel = null
        gameToStop.replaceSpectators(mutableListOf())
    }

    private fun registerNextGameMafiaExecutionProtection(game: Game) {
        val firstMafiaTargetId = game.firstMafiaTargetId ?: return
        MafiaExecutionProtectionManager.record(game.guild.id.value, firstMafiaTargetId.value)
    }

    private fun scheduleDelayedVoiceDisconnect(game: Game) {
        val kord = game.guild.kord
        val guildId = game.guild.id

        gameLoopScope.launch {
            delay(GAME_END_VOICE_DISCONNECT_DELAY_MS.milliseconds)
            if (currentGame?.guild?.id == guildId) return@launch
            LavalinkManager.stop(kord = kord, guildId = guildId)
        }
    }

    suspend fun releaseAllPlayerMutes(game: Game) {
        game.playerDatas.forEach { player ->
            runCatching {
                player.member.edit {
                    muted = false
                }
            }
        }
        game.spectatorMembers.forEach { spectator ->
            runCatching {
                spectator.edit {
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
            println("[GameManager] $channelName 채널 줄넘김 메시지 전송 실패: ${exception.message}")
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
        if (game.isSpectator(member.id)) {
            if (isGameTextChannelOrThread(game, event)) {
                runCatching { event.message.delete("관전자 채팅 차단") }
                return true
            }
            return false
        }

        val player = game.getPlayer(member.id) ?: return false
        if (!GameLoopManager.isMadScientistDistortionHidden(player) && !player.state.isDead) return false

        val isDeadChannel = event.message.channelId == Snowflake(GAME_DEAD_CHANNEL_ID)
        val replayContent = replayMessageContent(event)

        if (isDeadChannel && player.state.isDead) {
            GameReplayLogger.logChat(
                game = game,
                actor = player,
                body = replayContent,
                visibility = ReplayVisibility.DEAD_CHANNEL,
                recipients = replayRecipientsFor(game, ReplayVisibility.DEAD_CHANNEL),
                recipientDescription = replayRecipientDescription(game, ReplayVisibility.DEAD_CHANNEL)
            )
            val deceasedChatEvent = GameEvent.DeceasedChat(
                dayCount = game.dayCount,
                chatSender = player,
                chat = replayContent
            )
            dispatchDeceasedChatEvent(game, deceasedChatEvent)
            return true
        }

        if (player.state.isDead) {
            runCatching { event.message.delete("사망 플레이어 채팅 채널 제한") }
            return true
        }

        runCatching { event.message.delete("왜곡 상태 채팅 차단") }
        return true
    }

    private suspend fun isGameTextChannelOrThread(game: Game, event: MessageCreateEvent): Boolean {
        val channelId = event.message.channelId
        val parentChannelId = runCatching { event.message.getChannel().data.parentId }
            .getOrNull()
        val gameChannelIds: Set<Snowflake> = setOf(
            game.mainChannel?.id ?: Snowflake(GAME_MAIN_CHANNEL_ID),
            game.mafiaChannel?.id ?: Snowflake(GAME_MAFIA_CHANNEL_ID),
            game.coupleChannel?.id ?: Snowflake(GAME_COUPLE_CHANNEL_ID),
            game.deadChannel?.id ?: Snowflake(GAME_DEAD_CHANNEL_ID)
        )

        return channelId in gameChannelIds || gameChannelIds.any { channel -> channel == parentChannelId }
    }

    suspend fun recordReplayChat(event: MessageCreateEvent) {
        val game = currentGame ?: return
        val member = event.member ?: return
        val player = game.getPlayer(member.id) ?: return
        val content = replayMessageContent(event)
        if (content.isBlank()) return

        val channelId = event.message.channelId
        val mafiaChannelId = game.mafiaChannel?.id ?: Snowflake(GAME_MAFIA_CHANNEL_ID)
        val coupleChannelId = game.coupleChannel?.id ?: Snowflake(GAME_COUPLE_CHANNEL_ID)
        val deadChannelId = game.deadChannel?.id ?: Snowflake(GAME_DEAD_CHANNEL_ID)
        val parentChannelId = runCatching { event.message.getChannel().data.parentId }
            .getOrNull()

        val visibility = when {
            channelId == mafiaChannelId || parentChannelId == mafiaChannelId -> ReplayVisibility.MAFIA_CHANNEL
            channelId == coupleChannelId || parentChannelId == coupleChannelId -> ReplayVisibility.COUPLE_CHANNEL
            channelId == deadChannelId || parentChannelId == deadChannelId -> ReplayVisibility.DEAD_CHANNEL
            else -> ReplayVisibility.PUBLIC
        }

        GameReplayLogger.logChat(
            game = game,
            actor = player,
            body = content,
            visibility = visibility,
            recipients = replayRecipientsFor(game, visibility),
            recipientDescription = replayRecipientDescription(game, visibility)
        )
    }

    private fun replayMessageContent(event: MessageCreateEvent): String {
        val text = event.message.content.trim()
        val attachments = event.message.attachments.map { attachment ->
            val filename = attachment.filename.takeIf { it.isNotBlank() } ?: "첨부파일"
            val url = attachment.url.takeIf { it.isNotBlank() }
            if (url == null) filename else "$filename $url"
        }

        return (listOf(text).filter(String::isNotBlank) + attachments)
            .joinToString("\n")
            .trim()
    }

    private fun replayRecipientsFor(game: Game, visibility: ReplayVisibility): List<ReplayRecipient> {
        val players = when (visibility) {
            ReplayVisibility.PUBLIC -> game.playerDatas
            ReplayVisibility.MAFIA_CHANNEL -> game.playerDatas
                .filter { !it.state.isDead }
                .filter { it.job is Mafia || hasContactedMafiaTeam(game, it) }
            ReplayVisibility.COUPLE_CHANNEL -> game.playerDatas
                .filter {
                    !it.state.isDead &&
                        it.allAbilities.any { ability -> ability is CoupleAbility } &&
                        !FrogCurseManager.shouldSuppressPassive(it)
                }
            ReplayVisibility.DEAD_CHANNEL -> game.playerDatas
                .filter {
                    it.state.isDead ||
                        (
                            it.allAbilities.any { ability -> ability is ShamanAbilityOne } &&
                                !FrogCurseManager.shouldSuppressPassive(it)
                        )
                }
            else -> emptyList()
        }

        return players.map { player -> GameReplayLogger.recipient(player, visibility) }
    }

    private fun replayRecipientDescription(game: Game, visibility: ReplayVisibility): String {
        val label = when (visibility) {
            ReplayVisibility.PUBLIC -> "공개 채널"
            ReplayVisibility.MAFIA_CHANNEL -> "마피아 채널"
            ReplayVisibility.COUPLE_CHANNEL -> "연인 채널"
            ReplayVisibility.DEAD_CHANNEL -> "사망자 채널"
            ReplayVisibility.DIRECT_MESSAGE -> "DM"
            ReplayVisibility.EPHEMERAL -> "개인 응답"
            ReplayVisibility.SYSTEM_INTERNAL -> "시스템"
        }
        val names = replayRecipientsFor(game, visibility).joinToString(", ") { it.name }
        return if (names.isBlank()) label else "$label ($names)"
    }

    private fun replayCommunicationBody(sender: PlayerData, recipientDescription: String, message: String): String {
        val content = message.trim().ifBlank { "(내용 없음)" }
        return "보낸 사람: ${sender.member.effectiveName}\n받은 사람/곳: $recipientDescription\n내용: $content"
    }

    private fun replayCommunicationBody(sender: PlayerData, recipients: List<PlayerData>, message: String): String {
        val recipientDescription = recipients.joinToString(", ") { it.member.effectiveName }
            .ifBlank { "없음" }
        return replayCommunicationBody(sender, recipientDescription, message)
    }

    suspend fun handleSpiritCommands(event: MessageCreateEvent, commandName: String, args: List<String>): Boolean {
        val message = args.joinToString(" ").trim()

        return when (commandName) {
            SHAMAN_RELAY_COMMAND, "shaman-relay" -> {
                val memberId = event.member?.id ?: return false
                val result = relayShamanMessage(memberId, message)
                if (result.isSuccess) {
                    runCatching { event.message.delete("영매 접신 전달 처리") }
                } else {
                    event.message.channel.createMessage(result.message)
                }
                true
            }
            SHAMANED_RELAY_COMMAND, "spirit-relay" -> {
                val memberId = event.member?.id ?: return false
                val result = relayShamanedMessage(memberId, message)
                if (result.isSuccess) {
                    runCatching { event.message.delete("성불 플레이어 강령 전달 처리") }
                } else {
                    event.message.channel.createMessage(result.message)
                }
                true
            }
            else -> false
        }
    }

    suspend fun useMegaphone(memberId: Snowflake, message: String): SpiritRelayResult {
        val game = currentGame ?: return SpiritRelayResult(false, "진행 중인 게임이 없습니다.")
        val sender = game.getPlayer(memberId) ?: return SpiritRelayResult(false, "게임 참가자만 사용할 수 있습니다.")
        return game.megaphoneUseGate.exclusive {
            sendMegaphoneMessage(game, sender, message)
        }
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

    private suspend fun sendMegaphoneMessage(game: Game, sender: PlayerData, message: String): SpiritRelayResult {
        if (game.currentPhase != GamePhase.NIGHT) return SpiritRelayResult(false, "확성기는 밤에만 사용할 수 있습니다.")
        if (sender.state.isDead) return SpiritRelayResult(false, "사망한 플레이어는 확성기를 사용할 수 없습니다.")
        if (sender.state.isSilenced) return SpiritRelayResult(false, "유혹 상태에서는 확성기가 정상 출력되지 않습니다.")
        if (FrogCurseManager.shouldSuppressPassive(sender)) {
            return SpiritRelayResult(false, FrogCurseManager.abilityBlockedMessage(sender))
        }
        if (sender.allAbilities.none { it is Megaphone }) return SpiritRelayResult(false, "확성기 능력이 없습니다.")
        if (message.isBlank()) return SpiritRelayResult(false, "확성기 메시지를 입력해 주세요.")
        if (sender.member.id in game.usedMegaphonePlayerIds) return SpiritRelayResult(false, "확성기는 게임 중 1회만 사용할 수 있습니다.")
        if (game.megaphoneUsedTonight) return SpiritRelayResult(false, "이번 밤에는 이미 다른 플레이어가 확성기를 사용했습니다.")

        game.mainChannel?.createMessage {
            embed {
                title = "확성기"
                description = "${sender.member.effectiveName}: $message"
            }
        }
        GameReplayLogger.logChat(
            game = game,
            actor = sender,
            body = message,
            visibility = ReplayVisibility.PUBLIC,
            title = "확성기",
            recipients = replayRecipientsFor(game, ReplayVisibility.PUBLIC),
            recipientDescription = replayRecipientDescription(game, ReplayVisibility.PUBLIC)
        )
        game.usedMegaphonePlayerIds += sender.member.id
        game.megaphoneUsedTonight = true
        return SpiritRelayResult(true, "확성기 메시지를 전송했습니다.")
    }

    private fun sendSecretLetter(game: Game, sender: PlayerData, target: PlayerData, message: String): SpiritRelayResult {
        if (game.currentPhase != GamePhase.NIGHT) return SpiritRelayResult(false, "밀서는 밤에만 보낼 수 있습니다.")
        if (sender.state.isDead) return SpiritRelayResult(false, "사망한 플레이어는 밀서를 보낼 수 없습니다.")
        if (sender.state.isSilenced) return SpiritRelayResult(false, "유혹 상태에서는 능력을 사용할 수 없습니다.")
        if (FrogCurseManager.shouldSuppressPassive(sender)) {
            return SpiritRelayResult(false, FrogCurseManager.abilityBlockedMessage(sender))
        }
        if (sender.allAbilities.none { it is SecretLetter }) return SpiritRelayResult(false, "밀서 능력이 없습니다.")
        if (sender.member.id in game.usedSecretLetterPlayerIds) return SpiritRelayResult(false, "밀서는 게임 중 1회만 보낼 수 있습니다.")
        if (target.state.isDead) return SpiritRelayResult(false, "사망한 플레이어에게는 밀서를 보낼 수 없습니다.")
        if (target.member.id == sender.member.id) return SpiritRelayResult(false, "자기 자신에게는 밀서를 보낼 수 없습니다.")
        if (message.isBlank()) return SpiritRelayResult(false, "밀서 내용을 입력해 주세요.")

        val formatted = replayCommunicationBody(sender, listOf(target), message)
        GameReplayLogger.logSystem(
            game = game,
            title = "밀서 작성",
            body = formatted,
            visibility = ReplayVisibility.DIRECT_MESSAGE,
            actor = sender,
            recipients = listOf(
                GameReplayLogger.recipient(target, ReplayVisibility.DIRECT_MESSAGE),
                GameReplayLogger.recipient(sender, ReplayVisibility.DIRECT_MESSAGE)
            )
        )
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
        if (FrogCurseManager.shouldSuppressPassive(sender)) {
            return SpiritRelayResult(false, FrogCurseManager.abilityBlockedMessage(sender))
        }
        if (sender.allAbilities.none { it is Will }) return SpiritRelayResult(false, "유언 능력이 없습니다.")
        if (message.isBlank()) return SpiritRelayResult(false, "유언 내용을 입력해 주세요.")

        game.willByPlayerId[sender.member.id] = message
        GameReplayLogger.logSystem(
            game = game,
            title = "유언 작성",
            body = replayCommunicationBody(sender, "본인", message),
            visibility = ReplayVisibility.DIRECT_MESSAGE,
            actor = sender,
            recipients = listOf(GameReplayLogger.recipient(sender, ReplayVisibility.DIRECT_MESSAGE))
        )
        return SpiritRelayResult(true, "유언을 작성했습니다. 이번 밤에 사망하면 공개됩니다.")
    }

    private fun castPerjuryVote(game: Game, sender: PlayerData, target: PlayerData): SpiritRelayResult {
        if (game.currentPhase != GamePhase.VOTE || game.defenseTargetId != null) {
            return SpiritRelayResult(false, "위증은 본투표 시간에만 사용할 수 있습니다.")
        }
        if (sender.state.isDead) return SpiritRelayResult(false, "사망한 플레이어는 위증을 사용할 수 없습니다.")
        if (sender.state.isSilenced) return SpiritRelayResult(false, "유혹 상태에서는 능력을 사용할 수 없습니다.")
        if (FrogCurseManager.shouldSuppressPassive(sender)) {
            return SpiritRelayResult(false, FrogCurseManager.abilityBlockedMessage(sender))
        }
        if (sender.member.id in game.permanentlyDisenfranchisedVoters) {
            return SpiritRelayResult(false, "투표권이 없어 위증을 사용할 수 없습니다.")
        }
        if (game.activeThreatenedVoters.containsKey(sender.member.id)) {
            return SpiritRelayResult(false, "협박받아 투표할 수 없습니다")
        }
        if (sender.allAbilities.none { it is Perjury }) return SpiritRelayResult(false, "위증 능력이 없습니다.")
        if (target.state.isDead) return SpiritRelayResult(false, "사망한 플레이어는 위증 대상으로 지정할 수 없습니다.")
        game.currentFakeVotes[sender.member.id] = target.member.id
        return SpiritRelayResult(true, "${target.member.effectiveName}님에게 가짜 투표를 행사했습니다. (집계에만 반영)")
    }

    private suspend fun sendPasswordChat(game: Game, sender: PlayerData, message: String): SpiritRelayResult {
        if (sender.state.isDead) return SpiritRelayResult(false, "사망한 플레이어는 암구호를 사용할 수 없습니다.")
        if (sender.state.isSilenced) return SpiritRelayResult(false, "유혹 상태에서는 능력을 사용할 수 없습니다.")
        if (FrogCurseManager.shouldSuppressPassive(sender)) {
            return SpiritRelayResult(false, FrogCurseManager.abilityBlockedMessage(sender))
        }
        if (sender.job !is Evil || sender.job is Villain) return SpiritRelayResult(false, "마피아 팀만 암구호를 사용할 수 있습니다.")
        if (sender.job !is Mafia && !hasContactedMafiaTeam(game, sender)) {
            return SpiritRelayResult(false, "보조 직업은 접선 후에만 암구호를 사용할 수 있습니다.")
        }
        if (sender.allAbilities.none { it is Password }) return SpiritRelayResult(false, "암구호 능력이 없습니다.")
        if (message.isBlank()) return SpiritRelayResult(false, "암구호 메시지를 입력해 주세요.")

        val relayMessage = "[암구호] ${sender.member.effectiveName}: $message"
        val recipients = game.playerDatas
            .filter { !it.state.isDead }
            .filter { it.job is Mafia || hasContactedMafiaTeam(game, it) }

        val deliveryResults = coroutineScope {
            recipients.map { recipient ->
                async {
                    recipient to runCatching {
                        recipient.member.getDmChannel().createMessage(relayMessage)
                    }.exceptionOrNull()
                }
            }.awaitAll()
        }
        val failures = deliveryResults.filter { (_, error) -> error != null }
        failures.forEach { (recipient, error) ->
            println("⚠️ ${recipient.member.effectiveName} 암구호 DM 전송 실패: ${error?.message}")
        }
        val deliveredCount = deliveryResults.size - failures.size
        if (deliveredCount == 0) {
            return SpiritRelayResult(false, "암구호 DM을 전송하지 못했습니다. 수신자의 Discord DM 설정을 확인해 주세요.")
        }

        GameReplayLogger.logChat(
            game = game,
            actor = sender,
            body = message,
            visibility = ReplayVisibility.MAFIA_CHANNEL,
            title = "암구호",
            recipients = recipients.map { GameReplayLogger.recipient(it, ReplayVisibility.MAFIA_CHANNEL) },
            recipientDescription = "마피아 채널 DM (${recipients.joinToString(", ") { it.member.effectiveName }})"
        )
        val resultMessage = if (failures.isEmpty()) {
            "암구호 메시지를 마피아 채널 이용자 모두에게 DM으로 전송했습니다."
        } else {
            "암구호 메시지를 ${deliveredCount}명에게 DM으로 전송했습니다. ${failures.size}명에게는 DM을 보낼 수 없었습니다."
        }
        return SpiritRelayResult(true, resultMessage)
    }

    private fun hasContactedMafiaTeam(game: Game, player: PlayerData): Boolean {
        if (!isMafiaSupportJob(player)) return false
        if (player.state.hasContactedMafiaByInformant) return true

        return when (val job = player.job) {
            is Beastman -> player.state.isTamed
            is Godfather -> GodfatherContactPolicy.hasContactedMafia(game, player)
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

    suspend fun relayShamanMessage(memberId: Snowflake, message: String): SpiritRelayResult {
        val game = currentGame ?: return SpiritRelayResult(false, "진행 중인 게임이 없습니다.")
        val sender = game.getPlayer(memberId) ?: return SpiritRelayResult(false, "게임 참가자만 사용할 수 있습니다.")
        if (message.isBlank()) return SpiritRelayResult(false, "메시지를 입력해 주세요.")
        if (
            sender.allAbilities.none { it is ShamanAbilityOne } ||
            sender.state.isDead
        ) {
            return SpiritRelayResult(false, "생존한 영매 또는 접신 능력을 훔친 도둑만 사용할 수 있습니다.")
        }
        if (FrogCurseManager.shouldSuppressPassive(sender)) {
            return SpiritRelayResult(false, FrogCurseManager.abilityBlockedMessage(sender))
        }
        if (game.currentPhase != GamePhase.NIGHT) return SpiritRelayResult(false, "접신 메시지는 밤에만 보낼 수 있습니다.")
        val deadChannel = game.deadChannel ?: return SpiritRelayResult(false, "죽은 자들의 채널을 찾을 수 없습니다.")

        val relayMessage = if (sender.job is Thief) {
            "🔴 ${sender.member.effectiveName}: $message"
        } else {
            "[접신] ${sender.member.effectiveName}: $message"
        }
        val sendResult = runCatching {
            deadChannel.createMessage(relayMessage)
        }
        if (sendResult.isFailure) {
            val error = sendResult.exceptionOrNull()
            println("[GameManager] 접신 메시지 전송 실패: channelId=${deadChannel.id}, senderId=${sender.member.id}, reason=${error?.message}")
            return SpiritRelayResult(false, "죽은 자들의 채널에 접신 메시지를 보내지 못했습니다. 봇 권한과 채널 설정을 확인해 주세요.")
        }

        GameReplayLogger.logChat(
            game = game,
            actor = sender,
            body = message,
            visibility = ReplayVisibility.DEAD_CHANNEL,
            title = "접신",
            recipients = replayRecipientsFor(game, ReplayVisibility.DEAD_CHANNEL),
            recipientDescription = replayRecipientDescription(game, ReplayVisibility.DEAD_CHANNEL)
        )
        return SpiritRelayResult(true, "죽은 자들의 채널에 접신 메시지를 보냈습니다.")
    }

    suspend fun relayShamanedMessage(memberId: Snowflake, message: String): SpiritRelayResult {
        val game = currentGame ?: return SpiritRelayResult(false, "진행 중인 게임이 없습니다.")
        val sender = game.getPlayer(memberId) ?: return SpiritRelayResult(false, "게임 참가자만 사용할 수 있습니다.")
        if (message.isBlank()) return SpiritRelayResult(false, "메시지를 입력해 주세요.")
        if (!sender.state.isDead || !sender.state.isShamaned) return SpiritRelayResult(false, "성불된 사망자만 사용할 수 있습니다.")

        val deadChannel = game.deadChannel ?: return SpiritRelayResult(false, "죽은 자들의 채널을 찾을 수 없습니다.")
        val relayMessage = "[강령] ${sender.member.effectiveName}: $message"
        val sendResult = runCatching {
            deadChannel.createMessage(relayMessage)
        }
        if (sendResult.isFailure) {
            val error = sendResult.exceptionOrNull()
            println("[GameManager] 강령 메시지 전송 실패: channelId=${deadChannel.id}, senderId=${sender.member.id}, reason=${error?.message}")
            return SpiritRelayResult(false, "죽은 자들의 채널에 강령 메시지를 보내지 못했습니다. 봇 권한과 채널 설정을 확인해 주세요.")
        }

        val manifestShamans = shamanedRelayRecipients(game)
        GameReplayLogger.logChat(
            game = game,
            actor = sender,
            body = message,
            visibility = ReplayVisibility.DEAD_CHANNEL,
            title = "강령",
            recipients = replayRecipientsFor(game, ReplayVisibility.DEAD_CHANNEL),
            recipientDescription = replayRecipientDescription(game, ReplayVisibility.DEAD_CHANNEL)
        )
        relayShamanedPlayerMessage(game, sender, message, manifestShamans)
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
            .filterNot(FrogCurseManager::shouldSuppressPassive)
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
                val replayMessage = "[도청] ${sender.member.effectiveName}: ${event.message.content}"
                watcher.member.getDmChannel().createMessage(replayMessage)
            }
        }
    }

    private fun dispatchDeceasedChatEvent(game: Game, event: GameEvent.DeceasedChat) {
        val observers = game.playerDatas
            .filter { !it.state.isDead }
            .filterNot(FrogCurseManager::shouldSuppressPassive)
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
            .filterNot(FrogCurseManager::shouldSuppressPassive)
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
                    val replayMessage = "[도청] ${event.chatSender.member.effectiveName}: ${event.chat}"
                    watcher.member.getDmChannel().createMessage(replayMessage)
                }
            }
        }
    }

    private fun shamanedRelayRecipients(game: Game): List<PlayerData> =
        game.playerDatas
            .asSequence()
            .filter { !it.state.isDead }
            .filterNot(FrogCurseManager::shouldSuppressPassive)
            .filter { it.job is Shaman }
            .filter { player -> player.allAbilities.any { it is Manifesto } }
            .toList()

    private fun relayShamanedPlayerMessage(
        game: Game,
        sender: PlayerData,
        message: String,
        manifestShamans: List<PlayerData> = shamanedRelayRecipients(game)
    ) {
        if (manifestShamans.isEmpty()) return

        manifestShamans.forEach { shaman ->
            gameLoopScope.launch {
                runCatching {
                    val replayMessage = replayCommunicationBody(sender, listOf(shaman), message)
                    shaman.member.getDmChannel().createMessage(replayMessage)
                }
            }
        }
    }

    // 지목투표 데이터 저장
    fun receiveMainVote(voterId: Snowflake, targetIdString: String): VoteSubmissionResult {
        val game = currentGame ?: return VoteSubmissionResult.FAILURE

        return synchronized(game) {
            // 현재 페이즈가 본투표 시간일 때만 표를 받습니다.
            if (game.currentPhase != GamePhase.VOTE || game.defenseTargetId != null) return@synchronized VoteSubmissionResult.FAILURE
            val voter = game.getPlayer(voterId) ?: return@synchronized VoteSubmissionResult.FAILURE
            if (voter.state.isDead) return@synchronized VoteSubmissionResult.FAILURE
            if (voterId in game.permanentlyDisenfranchisedVoters) return@synchronized VoteSubmissionResult.FAILURE
            if (game.activeThreatenedVoters.containsKey(voterId)) return@synchronized VoteSubmissionResult.THREATENED
            val targetId = runCatching { Snowflake(targetIdString) }.getOrNull() ?: return@synchronized VoteSubmissionResult.FAILURE
            val target = game.getPlayer(targetId) ?: return@synchronized VoteSubmissionResult.FAILURE
            if (target.state.isDead) return@synchronized VoteSubmissionResult.FAILURE
            if (
                GameLoopManager.isMadScientistDistortionHidden(voter) &&
                voter.member.id == target.member.id
            ) return@synchronized VoteSubmissionResult.FAILURE
            val dictatorshipPolitician = game.playerDatas.singleOrNull { !it.state.isDead && it.job !is Evil }
                ?.takeIf {
                    it.job is Politician &&
                        !FrogCurseManager.shouldSuppressPassive(it)
                }
            if (dictatorshipPolitician != null && dictatorshipPolitician.member.id != voterId) return@synchronized VoteSubmissionResult.FAILURE

            game.currentMainVotes[voterId] = target.member.id.toString()
            if (game.dayCount == 1 && voter.job is Hostess && !game.hostessFirstVoteTargetByDay.containsKey(voterId)) {
                game.hostessFirstVoteTargetByDay[voterId] = target.member.id
            }
            GameReplayLogger.log(
                game = game,
                type = ReplayLogType.VOTE_CAST,
                visibility = ReplayVisibility.EPHEMERAL,
                title = "본투표",
                body = "${voter.member.effectiveName} -> ${target.member.effectiveName}",
                actor = voter,
                recipients = listOf(GameReplayLogger.recipient(voter, ReplayVisibility.EPHEMERAL))
            )
            VoteSubmissionResult.SUCCESS
        }
    }

    // 찬반 투표 데이터 저장
    fun receiveProsConsVote(
        voterId: Snowflake,
        isPros: Boolean,
        expectedDefenseTargetId: Snowflake? = null
    ): VoteSubmissionResult {
        val game = currentGame ?: return VoteSubmissionResult.FAILURE

        return synchronized(game) {
            if (game.currentPhase != GamePhase.VOTE || game.defenseTargetId == null) return@synchronized VoteSubmissionResult.FAILURE
            if (expectedDefenseTargetId != null && game.defenseTargetId != expectedDefenseTargetId) return@synchronized VoteSubmissionResult.FAILURE
            val voter = game.getPlayer(voterId) ?: return@synchronized VoteSubmissionResult.FAILURE
            if (voter.state.isDead) return@synchronized VoteSubmissionResult.FAILURE
            if (voterId in game.permanentlyDisenfranchisedVoters) return@synchronized VoteSubmissionResult.FAILURE
            if (game.activeThreatenedVoters.containsKey(voterId)) return@synchronized VoteSubmissionResult.THREATENED
            val dictatorshipPolitician = game.playerDatas.singleOrNull { !it.state.isDead && it.job !is Evil }
                ?.takeIf {
                    it.job is Politician &&
                        !FrogCurseManager.shouldSuppressPassive(it)
                }
            if (dictatorshipPolitician != null && dictatorshipPolitician.member.id != voterId) return@synchronized VoteSubmissionResult.FAILURE
            if (game.currentProsConsVotes.containsKey(voterId)) return@synchronized VoteSubmissionResult.FAILURE

            game.currentProsConsVotes[voterId] = isPros
            GameReplayLogger.log(
                game = game,
                type = ReplayLogType.PROS_CONS_VOTE,
                visibility = ReplayVisibility.EPHEMERAL,
                title = "찬반 투표",
                body = "${voter.member.effectiveName}: ${if (isPros) "찬성" else "반대"}",
                actor = voter,
                recipients = listOf(GameReplayLogger.recipient(voter, ReplayVisibility.EPHEMERAL))
            )
            VoteSubmissionResult.SUCCESS
        }
    }
}
