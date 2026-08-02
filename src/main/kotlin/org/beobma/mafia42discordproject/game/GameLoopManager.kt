package org.beobma.mafia42discordproject.game

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.channel.edit
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.ThreadChannel
import dev.kord.rest.builder.channel.addMemberOverwrite
import dev.kord.rest.builder.channel.addRoleOverwrite
import dev.kord.rest.builder.component.actionRow
import dev.kord.rest.builder.component.option
import dev.kord.rest.builder.message.embed
import kotlinx.coroutines.*
import org.beobma.mafia42discordproject.discord.DiscordMessageManager.playGameSound
import org.beobma.mafia42discordproject.discord.DiscordMessageManager.sendMainChannelMessageWithImage
import org.beobma.mafia42discordproject.discord.DiscordMessageManager.sendMainChannelMessageWithImageAndSound
import org.beobma.mafia42discordproject.discord.DiscordMessageManager.sendMainChannerCombinedMessage
import org.beobma.mafia42discordproject.discord.DiscordMessageManager.sendMainChannerMessage
import org.beobma.mafia42discordproject.discord.DiscordMessageManager.stopLoopingGameSound
import org.beobma.mafia42discordproject.game.loop.*
import org.beobma.mafia42discordproject.game.mode.GameStartMode
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.replay.GameReplayLogger
import org.beobma.mafia42discordproject.game.replay.ReplayLogType
import org.beobma.mafia42discordproject.game.replay.ReplayVisibility
import org.beobma.mafia42discordproject.game.system.*
import org.beobma.mafia42discordproject.game.system.notifications.PoliceNotificationManager
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.JobManager
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.Belongings
import org.beobma.mafia42discordproject.job.ability.general.definition.list.Source
import org.beobma.mafia42discordproject.job.ability.general.definition.list.couple.CoupleAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.shaman.ShamanAbilityOne
import org.beobma.mafia42discordproject.job.ability.general.definition.list.administrator.AdministratorInvestigationPolicy
import org.beobma.mafia42discordproject.job.ability.general.definition.list.agent.AgentOperation
import org.beobma.mafia42discordproject.job.ability.general.definition.list.detective.DetectiveAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.doctor.Calm
import org.beobma.mafia42discordproject.job.ability.general.definition.list.doctor.DoctorAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.gangster.TravelCompanion
import org.beobma.mafia42discordproject.job.ability.general.definition.list.hacker.Synchronization
import org.beobma.mafia42discordproject.job.ability.general.definition.list.inspector.InspectorInvestigation
import org.beobma.mafia42discordproject.job.ability.general.definition.list.judge.GovernmentAuthority
import org.beobma.mafia42discordproject.job.ability.general.definition.list.judge.JudgeAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.magician.Assistant
import org.beobma.mafia42discordproject.job.ability.general.definition.list.magician.Xray
import org.beobma.mafia42discordproject.job.ability.general.definition.list.martyr.Explosion
import org.beobma.mafia42discordproject.job.ability.general.definition.list.martyr.Flash
import org.beobma.mafia42discordproject.job.ability.general.definition.list.mentalist.MentalistAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.other.Resolute
import org.beobma.mafia42discordproject.job.ability.general.definition.list.other.UnwrittenRule
import org.beobma.mafia42discordproject.job.ability.general.definition.list.police.Autopsy
import org.beobma.mafia42discordproject.job.ability.general.definition.list.police.Confidential
import org.beobma.mafia42discordproject.job.ability.general.definition.list.politician.PoliticianAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.priest.Blessing
import org.beobma.mafia42discordproject.job.ability.general.definition.list.prophet.Apostle
import org.beobma.mafia42discordproject.job.ability.general.definition.list.prophet.Pioneer
import org.beobma.mafia42discordproject.job.ability.general.definition.list.reporter.BreakingNews
import org.beobma.mafia42discordproject.job.ability.general.definition.list.reporter.Obituary
import org.beobma.mafia42discordproject.job.ability.general.definition.list.reporter.ReporterAssets
import org.beobma.mafia42discordproject.job.ability.general.definition.list.soldier.MentalStrength
import org.beobma.mafia42discordproject.job.ability.general.evil.list.Instructions
import org.beobma.mafia42discordproject.job.ability.general.evil.list.Terminal
import org.beobma.mafia42discordproject.job.ability.general.evil.list.assistance.TheInformant
import org.beobma.mafia42discordproject.job.ability.general.evil.list.beastman.Barbarism
import org.beobma.mafia42discordproject.job.ability.general.evil.list.beastman.BeastmanAgility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.beastman.Roar
import org.beobma.mafia42discordproject.job.ability.general.evil.list.godfather.Discipline
import org.beobma.mafia42discordproject.job.ability.general.evil.list.godfather.GodfatherContactPolicy
import org.beobma.mafia42discordproject.job.ability.general.evil.list.hostess.Deception
import org.beobma.mafia42discordproject.job.ability.general.evil.list.madscientist.Analysis
import org.beobma.mafia42discordproject.job.ability.general.evil.list.madscientist.Distortion
import org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.MafiaAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.Concealment
import org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.Exorcism
import org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.Poisoning
import org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.Probation
import org.beobma.mafia42discordproject.job.ability.general.evil.list.spy.SpyAbility
import org.beobma.mafia42discordproject.job.ability.general.list.*
import org.beobma.mafia42discordproject.job.definition.list.*
import org.beobma.mafia42discordproject.job.evil.Evil
import org.beobma.mafia42discordproject.job.evil.list.*
import kotlin.time.Duration.Companion.milliseconds
import org.beobma.mafia42discordproject.job.ability.general.definition.list.priest.Exorcism as PriestExorcism

object GameLoopManager {
    private var timeThreadChannel: ThreadChannel? = null
    private var timeStatusMessage: Message? = null
    private val countdownLock = Any()
    private var activeCountdown: ActiveCountdown? = null
    private val cabalNotificationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val votePresentationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private const val DAWN_ANNOUNCEMENT_INTERVAL_MS = 1_000L
    private const val DAWN_PRIORITY_MAFIA_EXECUTION = 0
    private const val DAWN_PRIORITY_MAFIA_REACTION = 10
    private const val DAWN_PRIORITY_MAFIA_SUPPORT = 20
    private const val DAWN_PRIORITY_OTHER_EXECUTION = 100
    private const val DAWN_PRIORITY_REPORTER = 200
    private const val DAWN_PRIORITY_QUIET_NIGHT = 1_000
    private const val DAWN_PRIORITY_BELONGINGS = 2_000
    private const val DAWN_PRIORITY_WILL = 2_010

    fun resetTimeThreadState() {
        timeThreadChannel = null
        timeStatusMessage = null
    }

    suspend fun prepareGameChannels(game: Game) {
        val mainChannel = game.mainChannel ?: return
        val mafiaChannel = game.mafiaChannel ?: return
        val coupleChannel = game.coupleChannel ?: return
        val deadChannel = game.deadChannel ?: return

        updateMainChannelSpectatorPermissions(game, mainChannel)
        updateMafiaChannelPermissions(game, mafiaChannel, isNight = false)
        updateCoupleChannelPermissions(game, coupleChannel, isNight = false)
        updateDeadChannelPermissions(game, deadChannel)
        muteSpectators(game)
    }

    private suspend fun updateMainChannelSpectatorPermissions(game: Game, mainChannel: TextChannel) {
        if (game.playerDatas.isEmpty() && game.spectatorMembers.isEmpty()) return

        mainChannel.edit {
            game.playerDatas.forEach { player ->
                addMemberOverwrite(player.member.id) {
                    denied = Permissions()
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

    private suspend fun muteSpectators(game: Game) {
        game.spectatorMembers.forEach { spectator ->
            runCatching {
                spectator.edit {
                    muted = true
                }
            }
        }
    }

    suspend fun clearTimeThread() {
        runCatching {
            timeThreadChannel?.delete("게임 종료로 인한 시간 스레드 정리")
        }
        resetTimeThreadState()
    }

    suspend fun adjustDayTimeByPlayer(game: Game, playerId: Snowflake, isIncrease: Boolean): DayTimeAdjustmentResult {
        val player = game.getPlayer(playerId)
            ?: return DayTimeAdjustmentResult(false, "게임 참가자만 시간을 조정할 수 있습니다.")

        if (player.state.isDead) {
            return DayTimeAdjustmentResult(false, "사망한 플레이어는 시간을 조정할 수 없습니다.")
        }

        if (game.currentPhase != GamePhase.DAY) {
            return DayTimeAdjustmentResult(false, "시간 조정은 낮 페이즈에서만 가능합니다.")
        }

        if (game.mode == GameStartMode.ANNIHILATION) {
            return DayTimeAdjustmentResult(false, "말살 모드의 낮 시간은 120초 고정이며 연장하거나 단축할 수 없습니다.")
        }

        val delta = if (isIncrease) DAY_TIME_ADJUSTMENT_MS else -DAY_TIME_ADJUSTMENT_MS
        val remainingAfterAdjustment = synchronized(countdownLock) {
            if (game.dayTimeAdjustmentUsedPlayers.contains(playerId)) {
                return@synchronized null
            }

            val countdown = activeCountdown
            if (countdown == null ||
                countdown.guildId != game.guild.id ||
                countdown.phase != GamePhase.DAY ||
                countdown.label != "낮"
            ) {
                return@synchronized null
            }

            countdown.endAtMillis += delta
            val remaining = countdown.endAtMillis - System.currentTimeMillis()
            if (remaining <= 0L) {
                countdown.forceFinished = true
            }
            game.dayTimeAdjustmentUsedPlayers += playerId
            remaining
        } ?: return DayTimeAdjustmentResult(
            false,
            if (game.dayTimeAdjustmentUsedPlayers.contains(playerId)) {
                "하루에 한 번만 시간 조정을 사용할 수 있습니다."
            } else {
                "현재 조정 가능한 낮 카운트다운이 없습니다."
            }
        )

        updateTimeStatusMessage(game, "낮", remainingAfterAdjustment.coerceAtLeast(0L))

        if (!isIncrease && remainingAfterAdjustment <= 0L) {
            return DayTimeAdjustmentResult(
                true,
                "남은 시간이 0초 이하가 되어 즉시 다음 페이즈로 넘어갑니다."
            )
        }

        val actionText = if (isIncrease) "증가" else "감소"
        return DayTimeAdjustmentResult(
            true,
            "낮 시간을 15초 $actionText 했습니다. (하루 1회 사용 완료)"
        )
    }

    private suspend fun runPhaseCountdown(
        game: Game,
        label: String,
        durationMillis: Long,
        midpointAction: (suspend () -> Unit)? = null,
        beforeEndAction: (suspend () -> Unit)? = null,
        beforeEndOffsetMillis: Long = 0L,
        stopLoopingSoundOnEnd: Boolean = true
    ) {
        val initialDuration = durationMillis.coerceAtLeast(0L)
        val now = System.currentTimeMillis()
        val midpointAtMillis = now + initialDuration / 2
        val beforeEndAtMillis = now + (initialDuration - beforeEndOffsetMillis).coerceAtLeast(0L)
        var midpointTriggered = midpointAction == null || initialDuration <= 0L
        var beforeEndTriggered = beforeEndAction == null || initialDuration <= 0L
        synchronized(countdownLock) {
            activeCountdown = ActiveCountdown(
                guildId = game.guild.id,
                phase = game.currentPhase,
                label = label,
                endAtMillis = now + initialDuration
            )
        }

        updateTimeStatusMessage(game, label, initialDuration)

        while (true) {
            val remainingMillis = synchronized(countdownLock) {
                val countdown = activeCountdown
                if (countdown == null || countdown.guildId != game.guild.id || countdown.phase != game.currentPhase) {
                    0L
                } else if (countdown.forceFinished) {
                    0L
                } else {
                    countdown.endAtMillis - System.currentTimeMillis()
                }
            }

            if (remainingMillis <= 0L) {
                break
            }

            if (!midpointTriggered && System.currentTimeMillis() >= midpointAtMillis) {
                midpointTriggered = true
                midpointAction?.invoke()
            }

            if (!beforeEndTriggered && System.currentTimeMillis() >= beforeEndAtMillis) {
                beforeEndTriggered = true
                beforeEndAction?.invoke()
            }

            delay(minOf(remainingMillis, 500L).milliseconds)
        }

        if (!midpointTriggered) {
            midpointTriggered = true
            midpointAction?.invoke()
        }
        if (!beforeEndTriggered) {
            beforeEndTriggered = true
            beforeEndAction?.invoke()
        }

        updateTimeStatusMessageAtZero(game, label)
        if (stopLoopingSoundOnEnd) {
            game.stopLoopingGameSound()
        }
        synchronized(countdownLock) {
            activeCountdown = null
        }
    }

    private suspend fun updateTimeStatusMessage(game: Game, phaseLabel: String, remainingMillis: Long) {
        val targetEpochSeconds = ((System.currentTimeMillis() + remainingMillis) / 1_000L).coerceAtLeast(0L)
        val content = "${game.dayCount}일차 $phaseLabel - <t:${targetEpochSeconds}:R>"

        editTimeStatusMessage(game, content)
    }

    private suspend fun updateTimeStatusMessageAtZero(game: Game, phaseLabel: String) {
        val content = "${game.dayCount}일차 $phaseLabel - 0초"
        editTimeStatusMessage(game, content)
    }

    private suspend fun editTimeStatusMessage(game: Game, content: String) {
        val statusMessage = ensureTimeStatusMessage(game) ?: return

        runCatching {
            statusMessage.edit {
                this.content = content
            }
        }.onFailure {
            timeStatusMessage = null
            val recreated = ensureTimeStatusMessage(game) ?: return
            recreated.edit {
                this.content = content
            }
        }
    }

    private suspend fun ensureTimeStatusMessage(game: Game): Message? {
        if (timeStatusMessage != null) return timeStatusMessage

        val threadChannel = ensureTimeThread(game) ?: return null
        return runCatching {
            threadChannel.createMessage("시간 정보를 준비 중입니다...")
        }.onSuccess {
            timeStatusMessage = it
        }.getOrNull()
    }

    private suspend fun ensureTimeThread(game: Game): ThreadChannel? {
        timeThreadChannel?.let { return it }

        val mainChannel = game.mainChannel ?: return null
        return runCatching {
            mainChannel.startPublicThread(TIME_THREAD_NAME)
        }.onSuccess {
            timeThreadChannel = it
        }.onFailure {
            timeThreadChannel = null
            timeStatusMessage = null
        }.getOrNull()
    }

    suspend fun startNightPhase(game: Game) {
        notifyMindReadingResults(game)
        game.currentPhase = GamePhase.NIGHT
        game.dayCount += 1
        GameReplayLogger.logPhase(game, "${game.dayCount}일차 밤")
        if (game.dayCount > 1) {
            game.mafiaExecutionProtectedTargetId = null
        }
        processMadScientistNightTransitions(game)
        game.nightPhaseStartedAtMillis = System.currentTimeMillis()
        FrogCurseManager.clearExpiredAtNightStart(game)
        game.prophetSpecialWinScheduledTeam = null
        game.abilityUsersThisPhase.clear()
        game.abilityTargetByUserThisPhase.clear()
        game.detectiveRouteOwnerByObservedPlayerId.clear()
        game.nightAttacks.clear()
        game.nightDeathCandidates.clear()
        game.pendingNightDeathPlayerIds.clear()
        game.pendingNightDeathSourceByPlayerId.clear()
        game.nightEvents.clear()
        game.pendingBeastmanTameIds.clear()
        game.pendingWitchCurseByCaster.clear()
        game.pendingOblivionCurseByCaster.clear()
        game.pendingDayStartDiscoveries.clear()
        game.concealmentForcedQuietNight = false
        game.megaphoneUsedTonight = false
        game.willByPlayerId.clear()
        game.coupleSacrificeMap.clear()
        game.activeThreatenedVoters.clear()
        game.lastNightSummary = NightResolutionSummary()
        game.mafiaExecutionSucceededLastNight = false
        game.playerDatas.forEach { player ->
            player.state.isThreatened = false
        }
        game.playerDatas.forEach { player ->
            (player.job as? Cabal)?.let { cabalJob ->
                cabalJob.moonMarkedSunTonight = false
                cabalJob.cabalSpecialWinReady = false
            }
            (player.job as? Police)?.let { policeJob ->
                policeJob.currentSearchTarget = null
                policeJob.hasUsedSearchThisNight = false
            }
            (player.job as? Beastman)?.cravingTargetIdTonight = null
            (player.job as? Inspector)?.pendingInvestigationTargetId = null
            (player.job as? Detective)?.let {
                DetectiveAbility.resetNightState(player)
            }
            player.actualOrStolenJob<Administrator>()?.let { administratorJob ->
                administratorJob.investigationResultPlayerId = null
            }
            (player.job as? Gangster)?.prepareNightThreatSelection()
            (player.job as? Fortuneteller)?.fixedFortuneTargetId = null
            player.actualOrStolenJob<Hypnotist>()?.selectedTargetIdTonight = null
            (player.job as? HitMan)?.let { hitMan ->
                hitMan.firstContractTargetId = null
                hitMan.firstContractSelectedTargetId = null
                hitMan.firstContractGuessedJobName = null
            }
            (player.job as? Spy)?.remainingIntelUsesTonight = 1
            (player.job as? Mentalist)?.let {
                MentalistAbility.resetDayState(player)
            }
        }
        resolveCabalSunInvestigation(game)
        applyPoliceConfidentialInvestigation(game)

        game.sendMainChannelMessageWithImageAndSound(
            imageLink = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(7).png",
            message = "밤이 되었습니다.",
            soundPath = NIGHT_START_SOUND_PATH,
            loopSound = true
        )
        announceSourceMafiaCountAtNightStart(game)
        resolveHackerHacks(game)
        val nightStartEvents = dispatchEvents(game)
        JobDiscoveryNotificationManager.notifyDiscoveredTargets(nightStartEvents, game)
        notifyMercenaryClientsAtFirstNight(game)

        val mainChannel = game.mainChannel ?: return
        val mafiaChannel = game.mafiaChannel ?: return
        val coupleChannel = game.coupleChannel ?: return
        val deadChannel = game.deadChannel ?: return
        val alivePlayers = game.playerDatas.filter { !it.state.isDead }

        game.playerDatas.forEach { player ->
            runCatching {
                player.member.edit {
                    muted = true
                }
            }
        }
        muteSpectators(game)
        applyHostessSeductionStates(game)

        mainChannel.edit {
            addRoleOverwrite(game.guild.id) {
                allowed = Permissions(Permission.ReadMessageHistory, Permission.UseApplicationCommands)
                denied = Permissions(Permission.SendMessages)
            }

            game.playerDatas.forEach { player ->
                addMemberOverwrite(player.member.id) {
                    allowed = Permissions()
                    denied = Permissions()
                }
            }
        }
        updateMafiaChannelPermissions(game, mafiaChannel, isNight = true)
        updateCoupleChannelPermissions(game, coupleChannel, isNight = true)
        updateDeadChannelPermissions(game, deadChannel)

        alivePlayers.forEach { player ->
            player.allAbilities
                 .filterIsInstance<PassiveAbility>()
                .filterNot { FrogCurseManager.shouldSuppressPassive(player) }
                .sortedByDescending(PassiveAbility::priority)
                .forEach { passive ->
                    passive.onPhaseChanged(game, player, GamePhase.NIGHT)
                }
        }
    }

    suspend fun resolveNightPhase(game: Game): NightResolutionSummary {
        cancelSuppressedPendingAttacks(game)
        cancelSuppressedCoupleSacrifices(game)
        resolveGodfatherDiscipline(game)
        val blockedAttacks = mutableListOf<AttackEvent>()
        val successfulAttacks = linkedSetOf<AttackEvent>()
        val protectedMafiaExecutionBlockedAttacks = mutableSetOf<AttackEvent>()
        val beastmanAgilityBlockedMafiaAttacks = mutableSetOf<AttackEvent>()
        val beastmanMercenaryEncounters = mutableListOf<AttackEvent>()
        val negotiationBlockedAttacks = mutableSetOf<AttackEvent>()
        val fixedNightDeathPlayers = linkedSetOf<PlayerData>().apply {
            game.pendingNightDeathPlayerIds
                .mapNotNull(game::getPlayer)
                .filterTo(this) { !it.state.isDead }
        }
        val playersToDie = linkedSetOf<PlayerData>().apply {
            addAll(fixedNightDeathPlayers)
        }
        fun rebuildPlayersToDieFromAttackResults() {
            playersToDie.clear()
            playersToDie += fixedNightDeathPlayers.filterNot { it.state.isDead }
            successfulAttacks
                .map { it.target }
                .filterTo(playersToDie) { !it.state.isDead }
        }
        game.doctorSavedTargetTonight = null

        resolveGangsterThreats(game)
        resolveNursePrescriptions(game)
        val finalHealEvents = resolveDoctorHeals(game)
        resolveAdministratorInvestigations(game)
        val healEventsTonight = finalHealEvents.toMutableList()
        fun rememberHealEvents(events: List<GameEvent>) {
            events.filterIsInstance<GameEvent.PlayerHealed>()
                .forEach { event ->
                    if (event !in healEventsTonight) {
                        healEventsTonight += event
                    }
                }
        }

        val attacksToEvaluate = game.nightAttacks
            .toList()
            .sortedBy { (attackKey, _) -> attackEvaluationPriority(attackKey) }
        for ((attackKey, attackEvent) in attacksToEvaluate) {
            val target = attackEvent.target
            if (target.state.isDead) continue

            if (
                attackKey.startsWith("VIGILANTE_") &&
                shouldMafiaExecutionCancelAttack(game, attackEvent, successfulAttacks)
            ) {
                cancelNightAttack(game, attackKey, attackEvent)
                continue
            }
            if (attackKey.startsWith("MERCENARY_")) {
                val vigilanteHasPriority = game.nightAttacks.any { (otherKey, otherAttack) ->
                    otherKey.startsWith("VIGILANTE_") && otherAttack.target == attackEvent.target
                }
                if (
                    vigilanteHasPriority ||
                    shouldMafiaExecutionCancelAttack(game, attackEvent, successfulAttacks)
                ) {
                    cancelNightAttack(game, attackKey, attackEvent)
                    continue
                }
            }

            val negotiation = SwindlerManager.shouldTriggerNegotiation(game, target)
            if (negotiation != null) {
                val (swindlerPlayer, swindlerWasTarget) = negotiation
                if (!FrogCurseManager.shouldSuppressPassive(swindlerPlayer)) {
                    SwindlerManager.contactMafia(game, swindlerPlayer)
                    if (swindlerWasTarget) {
                        blockedAttacks += attackEvent
                        negotiationBlockedAttacks += attackEvent
                        if (isMafiaTeamAttackKey(attackKey)) {
                            game.concealmentForcedQuietNight = true
                        }
                        continue
                    }
                }
            }

            if (attackKey == "MAFIA_TEAM" && game.mafiaExecutionProtectedTargetId == target.member.id) {
                blockedAttacks += attackEvent
                protectedMafiaExecutionBlockedAttacks += attackEvent
                continue
            }

            if (isExecutionImmuneBeastmanTarget(attackEvent)) {
                blockedAttacks += attackEvent
                if (attackKey.startsWith("MERCENARY_") && attackEvent.attacker.job is Mercenary) {
                    beastmanMercenaryEncounters += attackEvent
                }
                if (isMafiaTeamAttackKey(attackKey)) {
                    beastmanAgilityBlockedMafiaAttacks += attackEvent
                    tameBeastmanByBarbarismIfNeeded(game, target)
                }
                continue
            }

            // 패시브(방탄 등)가 방어력(healTier)에 개입할 기회를 주기 위한 평가 이벤트 통보
            game.nightEvents += GameEvent.BeforeAttackEvaluated(attackEvent)
            val processedEvents = dispatchEvents(game)
            rememberHealEvents(processedEvents)

            if (target.state.healTier.level >= attackEvent.attackTier.level) {
                blockedAttacks += attackEvent

                val blockingHealEvents = healEventsTonight.filter { healEvent ->
                    healEvent.target == target && healEvent.defenseTier.level >= attackEvent.attackTier.level
                }
                if (blockingHealEvents.isNotEmpty()) {
                    game.doctorSavedTargetTonight = target
                    blockingHealEvents.forEach { healEvent ->
                        game.nightEvents += GameEvent.HealSucceeded(
                            healer = healEvent.healer,
                            target = healEvent.target,
                            blockedAttack = attackEvent
                        )
                    }
                }
            } else {
                successfulAttacks += attackEvent
                playersToDie += target
            }
        }

        notifyMercenaryBeastmanEncounters(game, beastmanMercenaryEncounters)
        resolveReporterScoops(game)
        rebuildPlayersToDieFromAttackResults()
        resolveMercenaryContractDeaths(game, blockedAttacks, playersToDie)

        resolveMartyrNightExplosions(game, playersToDie)

        val mafiaAttack = game.nightAttacks["MAFIA_TEAM"]
        
        // 마피아 팀의 모든 공격(대부 포함)을 순회하며 성공 여부 및 부수 효과 처리
        val allMafiaTeamAttacks = game.nightAttacks.filter { (key, _) ->
            key == "MAFIA_TEAM" || key.startsWith("GODFATHER_")
        }.values

        if (allMafiaTeamAttacks.isNotEmpty()) {
            val swindlerNegotiationBlockedExecution =
                mafiaAttack != null && mafiaAttack in negotiationBlockedAttacks

            var atLeastOneMafiaExecutionSucceeded = false
            val failedAttacks = mutableListOf<AttackEvent>()

            allMafiaTeamAttacks.forEach { attack ->
                val attackSucceeded = attack in successfulAttacks
                if (attackSucceeded) {
                    atLeastOneMafiaExecutionSucceeded = true
                    registerCoupleResentment(game, attack)
                    applyMafiaExecutionSuccessEffects(game, attack)
                    if (
                        attack.attackTier == AttackTier.PIERCE &&
                        attack.attacker.allAbilities.any { it is Exorcism }
                    ) {
                        applyCalmFromHealEvents(game, attack.target, healEventsTonight)
                    }
                } else {
                    failedAttacks += attack
                }
            }

            // 마피아 팀 중 적어도 한 명이라도 처형에 성공했다면 은폐가 발동하지 않도록 함
            if (!atLeastOneMafiaExecutionSucceeded) {
                failedAttacks.forEach { attack ->
                    if (attack in protectedMafiaExecutionBlockedAttacks) return@forEach
                    if (attack in beastmanAgilityBlockedMafiaAttacks) return@forEach
                    if (!swindlerNegotiationBlockedExecution) {
                        applyMafiaExecutionFailureEffects(game, attack)
                        applyCalmFromHealEvents(game, attack.target, healEventsTonight)
                    }
                }
            }

            game.mafiaAttackFailedPreviousNight = !atLeastOneMafiaExecutionSucceeded
            game.mafiaExecutionSucceededLastNight = atLeastOneMafiaExecutionSucceeded
        } else {
            game.mafiaAttackFailedPreviousNight = false
            game.mafiaExecutionSucceededLastNight = false
        }
        val successfulProbationAttacks = allMafiaTeamAttacks
            .filter { attack ->
                attack in successfulAttacks &&
                    attack.attacker.allAbilities.any { it is Probation }
            }
            .toList()
        resolveBeastmanCravingTaming(game, mafiaAttack, successfulAttacks, playersToDie)
        applyTravelCompanionPenalty(game, successfulAttacks, mafiaAttack)
        resolveNurseDoctorContactsFromFinalHeals(game, finalHealEvents, playersToDie)
        InspectorInvestigation.resolveNightInvestigations(game, playersToDie)

        playersToDie.forEach { victim ->
            game.nightEvents += GameEvent.PlayerDied(victim)
        }

        org.beobma.mafia42discordproject.job.ability.general.evil.list.witch.WitchAbility.applyOblivionCursesAtNightEnd(game)
        val processedEvents = dispatchEvents(game)
        cacheReporterDiscoveryResults(processedEvents)
        val deferredProcessedEvents = processedEvents.filterIsInstance<GameEvent.JobDiscovered>()
            .filter(::shouldNotifyAtDayStart)
        if (deferredProcessedEvents.isNotEmpty()) {
            game.pendingDayStartDiscoveries += deferredProcessedEvents
        }
        JobDiscoveryNotificationManager.notifyDiscoveredTargets(
            processedEvents
                .filterNot(::shouldNotifyAtDayStart)
                .filterNot(::isPresentedAsDawnAnnouncement),
            game
        )
        val deaths = playersToDie.toList()
        val dawnPresentation = buildDawnPresentation(game, deaths)
        applyProbationEffectsAfterGraveRobbing(game, successfulProbationAttacks)

        // 아침 이벤트(예: 도굴꾼 JobDiscovered, 도굴 이후 수습) 해소를 위한 추가 디스패치 파이프라인
        val additionalProcessedEvents = dispatchEvents(game)
        cacheReporterDiscoveryResults(additionalProcessedEvents)
        val additionalDeferredEvents = additionalProcessedEvents.filterIsInstance<GameEvent.JobDiscovered>()
            .filter(::shouldNotifyAtDayStart)
        if (additionalDeferredEvents.isNotEmpty()) {
            game.pendingDayStartDiscoveries += additionalDeferredEvents
        }
        JobDiscoveryNotificationManager.notifyDiscoveredTargets(
            additionalProcessedEvents
                .filterNot(::shouldNotifyAtDayStart)
                .filterNot(::isPresentedAsDawnAnnouncement),
            game
        )
        applyProbationCitizenConversion(game)

        val summary = NightResolutionSummary(
            processedEvents = processedEvents + additionalProcessedEvents,
            deaths = deaths,
            blockedAttacks = blockedAttacks.toList(),
            dawnPresentation = dawnPresentation
        )
        game.lastNightSummary = summary

        game.nightAttacks.clear()
        game.nightDeathCandidates.clear()
        game.pendingNightDeathPlayerIds.clear()
        game.pendingNightDeathSourceByPlayerId.clear()
        game.nightEvents.clear()
        game.playerDatas.forEach { player ->
            (player.job as? Doctor)?.currentHealTarget = null
            (player.job as? Nurse)?.let { nurse ->
                nurse.currentHealTarget = null
                nurse.prescribedTargetId = null
            }
            (player.job as? Thief)?.stolenHealTargetId = null
            (player.job as? Gangster)?.finalizeNightThreatSelection()
            player.actualOrStolenJob<Hypnotist>()?.let { hypnotist ->
                if (hypnotist.blockedNightsRemaining > 0) {
                    hypnotist.blockedNightsRemaining -= 1
                }
            }
            player.state.resetForNextPhase()
        }

        return summary
    }

    suspend fun resolveDawnPhase(game: Game, summary: NightResolutionSummary = game.lastNightSummary) {
        game.currentPhase = GamePhase.DAWN
        game.clearExpiredBlessingProtectedTargets()
        val deferredBelongingsAnnouncements = mutableListOf<DawnAnnouncement>()
        val poisonedVictims = game.playerDatas.filter { player ->
            !player.state.isDead &&
                player.state.isPoisoned &&
                player.state.poisonedDeathDay != null &&
                game.dayCount >= player.state.poisonedDeathDay!!
        }
        poisonedVictims.forEach { victim ->
            victim.state.isPoisoned = false
            victim.state.poisonedDeathDay = null
            if (victim !in summary.deaths) {
                processPlayerDeath(game, victim, isLynch = false)
                revealBelongingsIfNeeded(game, victim, deferredBelongingsAnnouncements)
            }
        }

        summary.deaths.forEach { victim ->
            if (victim.state.isDead) return@forEach
            processPlayerDeath(game, victim, isLynch = false)
            revealBelongingsIfNeeded(game, victim, deferredBelongingsAnnouncements)
        }
        resolvePriestResurrection(game)
        notifyPendingBeastmanTaming(game)

        announceCoupleSacrificeReveal(game, summary.deaths)

        val processedDawnEvents = dispatchEvents(game)
        val spyAssassinVictim = resolveSpyAssassin(game, deferredBelongingsAnnouncements)
        resolveCabalMoonInvestigation(game)
        resolveCabalSpecialWinReadiness(game)
        resolveProphetPioneerSpecialWinReadiness(game, summary)
        val dawnDeaths = (summary.deaths + poisonedVictims + listOfNotNull(spyAssassinVictim)).distinct()
        val reporterAnnouncements = publishReporterArticles(game)
        val willAnnouncements = buildNightWillAnnouncements(game, dawnDeaths)
        val dawnPresentation = summary.dawnPresentation ?: DawnPresentation(
            imageUrl = SystemImage.QUIET_NIGHT.imageUrl,
            message = "조용하게 밤이 넘어갔습니다."
        )
        val hasPoisonedVictims = poisonedVictims.isNotEmpty()
        val dawnPresentationImageUrls = dawnPresentation.imageUrls
        val isQuietNightPresentation =
            dawnPresentationImageUrls == listOf(SystemImage.QUIET_NIGHT.imageUrl) &&
                dawnPresentation.message.contains("조용하게 밤이 넘어갔습니다.")
        val shouldSendBasePresentation =
            (dawnPresentation.message.isNotBlank() || dawnPresentationImageUrls.isNotEmpty()) &&
                (!hasPoisonedVictims || !isQuietNightPresentation) &&
                (reporterAnnouncements.isEmpty() || !isQuietNightPresentation)

        val dawnAnnouncements = mutableListOf<DawnAnnouncement>()
        if (shouldSendBasePresentation) {
            if (dawnPresentation.announcements.isNotEmpty()) {
                dawnAnnouncements += dawnPresentation.announcements
            } else {
                dawnAnnouncements += DawnAnnouncement(
                    imageUrl = dawnPresentationImageUrls.firstOrNull().orEmpty(),
                    message = dawnPresentation.message,
                    priority = DAWN_PRIORITY_OTHER_EXECUTION
                )
            }
        }
        poisonedVictims
            .distinctBy { it.member.id }
            .forEach { victim ->
                dawnAnnouncements += DawnAnnouncement(
                    imageUrl = SystemImage.DEATH_BY_POISON.imageUrl,
                    message = "${victim.member.effectiveName}님이 중독으로 사망했습니다.",
                    priority = DAWN_PRIORITY_MAFIA_SUPPORT,
                    targetId = victim.member.id
                )
            }
        dawnAnnouncements += reporterAnnouncements
        dawnAnnouncements += deferredBelongingsAnnouncements
        dawnAnnouncements += willAnnouncements
        sendDawnAnnouncements(
            game,
            dawnAnnouncements.sortedBy(DawnAnnouncement::priority)
        )

        game.lastNightSummary = summary.copy(
            processedEvents = summary.processedEvents + processedDawnEvents,
            dawnPresentation = dawnPresentation
        )

        game.nightEvents.clear()
        game.coupleSacrificeMap.clear()
    }

    private suspend fun sendDawnAnnouncements(game: Game, announcements: List<DawnAnnouncement>) {
        announcements.forEachIndexed { index, announcement ->
            if (announcement.embedTitle != null) {
                game.mainChannel?.createMessage {
                    embed {
                        title = announcement.embedTitle
                        description = announcement.message
                    }
                }
            } else if (announcement.soundPath != null) {
                game.sendMainChannelMessageWithImageAndSound(
                    imageLink = announcement.imageUrl,
                    message = announcement.message,
                    soundPath = announcement.soundPath
                )
            } else if (announcement.imageUrl.isNotBlank()) {
                game.sendMainChannelMessageWithImage(
                    imageLink = announcement.imageUrl,
                    message = announcement.message
                )
            } else {
                game.sendMainChannerMessage(announcement.message)
            }

            if (index < announcements.lastIndex) {
                delay(DAWN_ANNOUNCEMENT_INTERVAL_MS)
            }
        }
    }

    private suspend fun resolvePriestResurrection(game: Game) {
        game.playerDatas.forEach { priestPlayer ->
            val priestJob = priestPlayer.job as? Priest
            val thiefJob = priestPlayer.job as? Thief
            val targetId = priestJob?.pendingResurrectionTargetId ?: thiefJob?.stolenPriestResurrectionTargetId ?: return@forEach
            priestJob?.pendingResurrectionTargetId = null
            thiefJob?.stolenPriestResurrectionTargetId = null
            if (FrogCurseManager.shouldSuppressPassive(priestPlayer)) return@forEach

            if (priestPlayer.state.isDead) {
                game.sendMainChannerMessage("${priestPlayer.member.effectiveName}님이 사망하여 소생이 취소되었습니다.")
                return@forEach
            }

            val target = game.getPlayer(targetId) ?: return@forEach
            if (!target.state.isDead) {
                game.sendMainChannerMessage("${target.member.effectiveName}님은 이미 생존 상태여서 소생이 실패했습니다.")
                return@forEach
            }

            val hasExorcism = priestPlayer.allAbilities.any { it is PriestExorcism }
            if (target.state.isShamaned && !hasExorcism) {
                game.sendMainChannerMessage("${target.member.effectiveName}님은 성불 상태여서 소생이 실패했습니다.")
                return@forEach
            }

            val wasMadScientist = target.job is MadScientist
            target.job = Citizen()

            target.state.isDead = false
            target.state.diedDayCount = null
            target.state.isShamaned = false
            target.state.isPoisoned = false
            target.state.poisonedDeathDay = null
            if (wasMadScientist) {
                target.state.pendingMadScientistRevivalNight = null
                target.state.pendingMadScientistPublicRevealNight = null
                target.state.isMadScientistDistortionHidden = false
                target.state.madScientistAnalysisEligibleDay = null
                target.state.hasUsedMadScientistAnalysis = false
                game.pendingMadScientistRevivalAnnouncementIds -= target.member.id
            }
            game.publiclyRevealedAbilityTargetIds += target.member.id
            priestPlayer.job?.name?.let { game.publiclyRevealedJobNames += it }
            if (priestPlayer.allAbilities.any { it is Blessing }) {
                game.markBlessingProtectedTarget(target)
            }

            game.sendMainChannelMessageWithImageAndSound(
                imageLink = SystemImage.PRIEST_RESURRECTION.imageUrl,
                message = "${target.member.effectiveName}님이 부활했습니다.",
                soundPath = PRIEST_RESURRECTION_SOUND_PATH
            )
        }
    }

    private fun registerCoupleResentment(game: Game, mafiaAttack: AttackEvent) {
        val victimCouple = mafiaAttack.target.job as? Couple ?: return
        if (FrogCurseManager.shouldSuppressPassive(mafiaAttack.target)) return
        val partnerId = victimCouple.pairedPlayerId ?: return
        val partner = game.getPlayer(partnerId) ?: return
        val partnerCouple = partner.job as? Couple ?: return
        if (partner.state.isDead) return
        if (FrogCurseManager.shouldSuppressPassive(partner)) return

        partnerCouple.avengedMafiaIds += mafiaAttack.attacker.member.id
    }

    private fun cancelSuppressedCoupleSacrifices(game: Game) {
        val mafiaAttack = game.nightAttacks["MAFIA_TEAM"] ?: return
        val originalTargetId = game.coupleSacrificeMap[mafiaAttack.target.member.id] ?: return
        val originalTarget = game.getPlayer(originalTargetId) ?: return
        val substitute = mafiaAttack.target
        if (
            !FrogCurseManager.shouldSuppressPassive(originalTarget) &&
            !FrogCurseManager.shouldSuppressPassive(substitute)
        ) {
            return
        }

        game.nightAttacks["MAFIA_TEAM"] = mafiaAttack.copy(target = originalTarget)
        game.coupleSacrificeMap.remove(substitute.member.id)
        game.nightDeathCandidates.remove(substitute)
        if (originalTarget !in game.nightDeathCandidates) {
            game.nightDeathCandidates += originalTarget
        }
    }

    private fun cancelSuppressedPendingAttacks(game: Game) {
        val cancelledAttacks = game.nightAttacks
            .filterValues { attack -> FrogCurseManager.shouldSuppressPassive(attack.attacker) }
            .toList()
        cancelledAttacks.forEach { (attackKey, attack) ->
            game.nightAttacks.remove(attackKey)
            if (game.nightAttacks.values.none { it.target == attack.target }) {
                game.nightDeathCandidates.remove(attack.target)
            }
        }
    }

    private suspend fun resolveGodfatherDiscipline(game: Game) {
        val mafiaAttack = game.nightAttacks["MAFIA_TEAM"] ?: return
        val godfatherPlayer = mafiaAttack.target
        if (mafiaAttack.attacker.job !is Mafia) return
        if (godfatherPlayer.job !is Godfather) return
        if (GodfatherContactPolicy.hasContactedMafia(game, godfatherPlayer)) return
        if (FrogCurseManager.shouldSuppressPassive(godfatherPlayer)) return
        if (godfatherPlayer.allAbilities.none { it is Discipline }) return

        game.nightAttacks.remove("MAFIA_TEAM")
        removeNightDeathCandidateIfNoSourceRemains(game, godfatherPlayer)

        val aliveMafias = game.playerDatas.filter { player ->
            !player.state.isDead && player.job is Mafia
        }
        if (aliveMafias.size == 1) {
            mafiaAttack.attacker.state.mafiaAbilityBlockedNight = game.dayCount + 1
        } else {
            val counterAttack = AttackEvent(
                attacker = godfatherPlayer,
                target = mafiaAttack.attacker,
                attackTier = AttackTier.NORMAL
            )
            game.nightAttacks["DISCIPLINE_${godfatherPlayer.member.id}"] = counterAttack
            if (counterAttack.target !in game.nightDeathCandidates) {
                game.nightDeathCandidates += counterAttack.target
            }
        }

        godfatherPlayer.state.hasContactedMafiaByDiscipline = true
        if (!godfatherPlayer.state.hasAnnouncedGodfatherContact) {
            godfatherPlayer.state.hasAnnouncedGodfatherContact = true
            announceMafiaSupportContact(game, godfatherPlayer, GODFATHER_CONTACT_IMAGE_URL)
        }
        refreshMafiaChannelContactState(game)
    }

    private suspend fun announceCoupleSacrificeReveal(game: Game, deaths: List<PlayerData>) {

        deaths.forEach { deadPlayer ->
            val originalTargetId = game.coupleSacrificeMap[deadPlayer.member.id] ?: return@forEach
            val originalTarget = game.getPlayer(originalTargetId) ?: return@forEach

            deadPlayer.state.isJobPubliclyRevealed = true
            originalTarget.state.isJobPubliclyRevealed = true
            deadPlayer.job?.name?.let(game.publiclyRevealedJobNames::add)
            originalTarget.job?.name?.let(game.publiclyRevealedJobNames::add)

            val deadRole = (deadPlayer.job as? Couple)?.role

            // 1. 성별에 따른 이미지 URL 선택
            val imageUrl = when (deadRole) {
                CoupleRole.MALE -> SystemImage.DEATH_MALE_COUPLE.imageUrl
                CoupleRole.FEMALE -> SystemImage.DEATH_WOMAN_COUPLE.imageUrl
                else -> SystemImage.DEATH_BY_MAFIA.imageUrl
            }

            // 2. 메시지 구성
            val message =
                "${deadPlayer.member.effectiveName}님이 연인 ${originalTarget.member.effectiveName}님을 살리고 대신 처형당했습니다!"

            // 3. 텍스트 대신 이미지와 함께 전송
            game.sendMainChannelMessageWithImageAndSound(
                imageLink = imageUrl,
                message = message,
                soundPath = COUPLE_SACRIFICE_SOUND_PATH
            )
        }
    }

    suspend fun startDayPhase(
        game: Game
    ) {
        game.unwrittenRuleBlockedTargetIdTonight = null
        val mainChannel = game.mainChannel ?: return
        val mafiaChannel = game.mafiaChannel ?: return
        val coupleChannel = game.coupleChannel ?: return
        val deadChannel = game.deadChannel ?: return

        // 1. 게임 상태 및 날짜 변경
        game.currentPhase = GamePhase.DAY
        GameReplayLogger.logPhase(game, "${game.dayCount}일차 낮")
        game.dayTimeAdjustmentUsedPlayers.clear()
        game.abilityUsersThisPhase.clear()
        game.lastNightAbilityTargetByUser.clear()
        game.lastNightAbilityTargetByUser.putAll(game.abilityTargetByUserThisPhase)
        game.abilityTargetByUserThisPhase.clear()
        notifyMercenaryContractReception(game)
        AdministratorInvestigationNotificationManager.notifyResults(game)
        game.playerDatas.forEach { player ->
            (player.job as? Magician)?.let { magician ->
                magician.trickSubstitutedTargetId = null
                if (!magician.hasUsedTrick) magician.trickTargetId = null
            }
            (player.job as? Thief)?.clearStolenAbility()
        }

        game.sendMainChannelMessageWithImageAndSound(
            imageLink = SystemImage.DAY_START.imageUrl,
            message = "낮이 되었습니다.",
            soundPath = DAY_START_SOUND_PATH,
            loopSound = true
        )
        applyHostessSeductionStates(game)
        if (game.pendingDayStartDiscoveries.isNotEmpty()) {
            JobDiscoveryNotificationManager.notifyDiscoveredTargets(game.pendingDayStartDiscoveries.toList(), game)
            game.pendingDayStartDiscoveries.clear()
        }
        deliverSecretLetters(game)
        notifyPendingPoisonEffects(game)
        notifyInstructionsAtFirstDay(game)
        notifyTheInformantAutoContactAtSecondDay(game)

        mainChannel.edit {
            addRoleOverwrite(game.guild.id) {
                allowed = Permissions(Permission.SendMessages, Permission.ReadMessageHistory)
                denied = Permissions()
            }

            game.playerDatas.forEach { player ->
                if (shouldRestrictCommunication(player)) {
                    addMemberOverwrite(player.member.id) {
                        denied = Permissions(Permission.SendMessages)
                    }
                } else {
                    addMemberOverwrite(player.member.id) {
                        denied = Permissions()
                    }
                }
            }

            game.spectatorMembers.forEach { spectator ->
                addMemberOverwrite(spectator.id) {
                    allowed = Permissions(Permission.ViewChannel)
                    denied = Permissions(Permission.SendMessages)
                }
            }
        }

        game.playerDatas.forEach { player ->
            val shouldMute = shouldRestrictCommunication(player)
            runCatching {
                player.member.edit {
                    muted = shouldMute
                }
            }
        }
        muteSpectators(game)

        updateMafiaChannelPermissions(game, mafiaChannel, isNight = false)
        updateCoupleChannelPermissions(game, coupleChannel, isNight = false)
        updateDeadChannelPermissions(game, deadChannel)

        game.playerDatas
            .filter { !it.state.isDead }
            .forEach { player ->
                player.allAbilities
                     .filterIsInstance<PassiveAbility>()
                    .filterNot { FrogCurseManager.shouldSuppressPassive(player) }
                    .sortedByDescending(PassiveAbility::priority)
                    .forEach { passive ->
                        passive.onPhaseChanged(game, player, GamePhase.DAY)
                    }
            }

        notifyBeastmanRoarAtFirstDay(game)
    }

    private suspend fun updateMafiaChannelPermissions(
        game: Game,
        mafiaChannel: TextChannel,
        isNight: Boolean,
        notifyContact: Boolean = true
    ) {
        mafiaChannel.edit {
            addRoleOverwrite(game.guild.id) {
                denied = Permissions(
                    Permission.ViewChannel,
                    Permission.ReadMessageHistory,
                    Permission.SendMessages
                )
            }

            game.playerDatas.forEach { player ->
                if (player.state.isDead) {
                    addMemberOverwrite(player.member.id) {
                        allowed = Permissions(Permission.ViewChannel)
                        denied = Permissions(Permission.ReadMessageHistory, Permission.SendMessages)
                    }
                    return@forEach
                }

                if (canAccessMafiaChannel(game, player)) {
                    val canSend = isNight && !shouldRestrictCommunication(player)
                    addMemberOverwrite(player.member.id) {
                        allowed = if (canSend) {
                            Permissions(
                                Permission.ViewChannel,
                                Permission.SendMessages
                            )
                        } else {
                            Permissions(Permission.ViewChannel)
                        }
                        denied = if (canSend) {
                            Permissions(Permission.ReadMessageHistory)
                        } else {
                            Permissions(Permission.ReadMessageHistory, Permission.SendMessages)
                        }
                    }
                } else {
                    addMemberOverwrite(player.member.id) {
                        denied = Permissions(
                            Permission.ViewChannel,
                            Permission.ReadMessageHistory,
                            Permission.SendMessages
                        )
                    }
                }
            }

            game.spectatorMembers.forEach { spectator ->
                addMemberOverwrite(spectator.id) {
                    allowed = Permissions(Permission.ViewChannel)
                    denied = Permissions(Permission.ReadMessageHistory, Permission.SendMessages)
                }
            }
        }

        if (notifyContact) {
            notifyGodfatherContactInMafiaChannel(game)
        }
    }

    suspend fun refreshNightPrivateChannelPermissions(game: Game) {
        if (game.currentPhase != GamePhase.NIGHT) return
        game.mafiaChannel?.let {
            updateMafiaChannelPermissions(game, it, isNight = true, notifyContact = false)
        }
        game.coupleChannel?.let {
            updateCoupleChannelPermissions(game, it, isNight = true)
        }
    }

    private suspend fun notifyGodfatherContactInMafiaChannel(game: Game) {
        game.playerDatas.forEach { player ->
            if (player.state.isDead) return@forEach
            if (player.job !is Godfather) return@forEach
            if (player.state.hasAnnouncedGodfatherContact) return@forEach
            if (!GodfatherContactPolicy.hasContactedMafia(game, player)) return@forEach

            player.state.hasAnnouncedGodfatherContact = true
            announceMafiaSupportContact(game, player, GODFATHER_CONTACT_IMAGE_URL)
        }
    }

    private fun canAccessMafiaChannel(game: Game, player: PlayerData): Boolean {
        if (player.job is Mafia) return true
        if (!isMafiaSupportJob(player)) return false

        return player.state.hasContactedMafiaByInformant || hasContactedMafiaByJobState(game, player)
    }

    private fun isMafiaSupportJob(player: PlayerData): Boolean {
        val job = player.job
        return job is Evil && job !is Mafia && job !is Villain
    }

    private fun hasContactedMafiaByJobState(game: Game, player: PlayerData): Boolean {
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

    private suspend fun resolveSpyAssassin(
        game: Game,
        deferredBelongingsAnnouncements: MutableList<DawnAnnouncement>? = null
    ): PlayerData? {
        val aliveMafia = game.playerDatas.filter { !it.state.isDead && it.job is Mafia }
        if (aliveMafia.isNotEmpty()) return null

        val aliveEvil = game.playerDatas.filter { !it.state.isDead && it.job is Evil }
        if (aliveEvil.size != 1) return null

        val spyPlayer = aliveEvil.firstOrNull { it.job is Spy } ?: return null
        val spyJob = spyPlayer.job as? Spy ?: return null
        if (FrogCurseManager.shouldSuppressPassive(spyPlayer)) return null
        if (spyJob.hasTriggeredAssassin) return null

        val targetId = spyJob.lastInvestigatedTargetId ?: return null
        val target = game.getPlayer(targetId) ?: return null
        if (target.state.isDead) return null
        if (target.member.id == spyPlayer.member.id) return null

        spyJob.hasTriggeredAssassin = true
        processPlayerDeath(game, target, isLynch = false)
        revealBelongingsIfNeeded(game, target, deferredBelongingsAnnouncements)

        game.sendMainChannelMessageWithImage(
            imageLink = SPY_ASSASSIN_IMAGE_URL,
            message = "**${target.member.effectiveName}이(가) 자객에 의해 살해당하였습니다.**"
        )
        return target
    }

    private suspend fun applyImmediateDeathCommunicationState(game: Game, player: PlayerData) {
        runCatching {
            player.member.edit {
                muted = true
            }
        }

        game.mainChannel?.edit {
            addMemberOverwrite(player.member.id) {
                denied = Permissions(Permission.SendMessages)
            }
        }

        val isNight = game.currentPhase == GamePhase.NIGHT
        game.mafiaChannel?.let { updateMafiaChannelPermissions(game, it, isNight) }
        game.coupleChannel?.let { updateCoupleChannelPermissions(game, it, isNight) }
        game.deadChannel?.let { updateDeadChannelPermissions(game, it) }
    }

    private suspend fun processPlayerDeath(
        game: Game,
        victim: PlayerData,
        isLynch: Boolean
    ) {
        if (victim.state.isDead) return

        victim.state.isDead = true
        victim.state.diedDayCount = game.dayCount
        HackerRedirectManager.releaseProxiesTargeting(game, victim)
        handleMadScientistDeath(game, victim, isLynch = isLynch)
        game.nightEvents += GameEvent.PlayerDied(victim, isLynch = isLynch)
        val abilityTarget = HackerRedirectManager.resolveTarget(game, victim) ?: victim
        applyPoliceAutopsy(game, abilityTarget)
        SpyAbility.applyAutopsyOnDeath(game, abilityTarget)
        applyImmediateDeathCommunicationState(game, victim)
        sendDeadChannelDeathMention(game, victim)
    }

    private suspend fun sendDeadChannelDeathMention(game: Game, victim: PlayerData) {
        val deadChannel = game.deadChannel ?: return
        runCatching {
            deadChannel.createMessage(mention(victim))
        }.onFailure { error ->
            println("Failed to send dead channel death mention for ${victim.member.id.value}: ${error.message}")
        }
    }

    fun isMadScientistDistortionHidden(player: PlayerData): Boolean {
        return player.job is MadScientist && player.state.isMadScientistDistortionHidden
    }

    private fun shouldRestrictCommunication(player: PlayerData): Boolean {
        return shouldRestrictCommunication(
            isDead = player.state.isDead,
            isShamaned = player.state.isShamaned,
            isSilenced = player.state.isSilenced,
            isMadScientistDistortionHidden = isMadScientistDistortionHidden(player)
        )
    }

    internal fun shouldRestrictCommunication(
        isDead: Boolean,
        isShamaned: Boolean,
        isSilenced: Boolean,
        isMadScientistDistortionHidden: Boolean
    ): Boolean {
        return isDead || isShamaned || isSilenced || isMadScientistDistortionHidden
    }

    private fun isMafiaEliminated(game: Game): Boolean {
        return game.playerDatas.none { !it.state.isDead && it.job is Mafia }
    }

    private suspend fun processMadScientistNightTransitions(game: Game) {
        game.playerDatas.forEach { player ->
            if (player.job !is MadScientist) return@forEach
            if (FrogCurseManager.shouldSuppressPassive(player)) return@forEach

            val revealNight = player.state.pendingMadScientistPublicRevealNight
            if (revealNight != null && revealNight <= game.dayCount) {
                player.state.pendingMadScientistPublicRevealNight = null
                player.state.isMadScientistDistortionHidden = false
                player.state.isJobPubliclyRevealed = true
                game.publiclyRevealedAbilityTargetIds += player.member.id
                game.publiclyRevealedJobNames += MadScientist().name
                queueMadScientistRevivalAnnouncement(game, player)
            }

            val reviveNight = player.state.pendingMadScientistRevivalNight
            if (reviveNight == null || reviveNight > game.dayCount) return@forEach
            player.state.pendingMadScientistRevivalNight = null
            if (!player.state.isDead) {
                player.state.pendingMadScientistPublicRevealNight = null
                player.state.isMadScientistDistortionHidden = false
                return@forEach
            }

            if (player.state.isShamaned || isMafiaEliminated(game)) {
                player.state.isMadScientistDistortionHidden = false
                player.state.pendingMadScientistPublicRevealNight = null
                return@forEach
            }

            player.state.isDead = false
            player.state.diedDayCount = null
            player.state.isShamaned = false
            player.state.isPoisoned = false
            player.state.poisonedDeathDay = null
            player.state.madScientistAnalysisEligibleDay = game.dayCount
            player.state.hasUsedMadScientistAnalysis = false
            game.publiclyRevealedAbilityTargetIds += player.member.id

            val hasDistortion = player.allAbilities.any { it is Distortion }
            if (hasDistortion) {
                player.state.isMadScientistDistortionHidden = true
                player.state.pendingMadScientistPublicRevealNight = game.dayCount + 1
            } else {
                player.state.isMadScientistDistortionHidden = false
                player.state.pendingMadScientistPublicRevealNight = null
                player.state.isJobPubliclyRevealed = true
                game.publiclyRevealedJobNames += MadScientist().name
                queueMadScientistRevivalAnnouncement(game, player)
            }
        }
    }

    private fun queueMadScientistRevivalAnnouncement(game: Game, player: PlayerData) {
        game.pendingMadScientistRevivalAnnouncementIds += player.member.id
    }

    private suspend fun announcePendingMadScientistRevivals(game: Game) {
        if (game.currentPhase != GamePhase.NIGHT) return
        val announcementIds = game.pendingMadScientistRevivalAnnouncementIds.toList()
        if (announcementIds.isEmpty()) return
        game.pendingMadScientistRevivalAnnouncementIds.clear()

        val revivedScientists = announcementIds
            .mapNotNull { playerId -> game.getPlayer(playerId) }
            .filter { player -> !player.state.isDead && player.job is MadScientist }
        if (revivedScientists.isEmpty()) return

        game.stopLoopingGameSound()
        revivedScientists.forEach { player ->
            game.sendMainChannelMessageWithImageAndSound(
                imageLink = MAD_SCIENTIST_REVIVE_IMAGE_URL,
                message = "${player.member.effectiveName}님이 부활하셨습니다!",
                soundPath = MAD_SCIENTIST_REVIVE_SOUND_PATH
            )
        }
    }

    private suspend fun handleMadScientistDeath(game: Game, victim: PlayerData, isLynch: Boolean) {
        if (victim.job !is MadScientist) return
        if (FrogCurseManager.shouldSuppressPassive(victim)) return
        if (!victim.state.hasUsedMadScientistRegeneration) {
            victim.state.hasUsedMadScientistRegeneration = true
            victim.state.pendingMadScientistRevivalNight = game.dayCount + 1
            val lynchedVoteTargetId = if (isLynch) {
                game.currentMainVotes[victim.member.id]?.let(::Snowflake)
            } else {
                null
            }
            val lynchedVoteTarget = lynchedVoteTargetId?.let(game::getPlayer)
            val isAnalysisIgnored =
                victim.allAbilities.any { it is Analysis } &&
                    lynchedVoteTarget != null &&
                    shouldIgnoreHarmfulEffectByMentalStrength(game, lynchedVoteTarget)
            victim.state.madScientistLynchedVoteTargetId =
                lynchedVoteTargetId.takeUnless { isAnalysisIgnored }
        }

        victim.state.isMadScientistDistortionHidden = false
        victim.state.pendingMadScientistPublicRevealNight = null
        game.pendingMadScientistRevivalAnnouncementIds -= victim.member.id

        if (!victim.state.hasContactedMafiaOnDeath) {
            victim.state.hasContactedMafiaOnDeath = true
            if (!victim.state.hasAnnouncedMadScientistContact) {
                victim.state.hasAnnouncedMadScientistContact = true
                announceMafiaSupportContact(game, victim, MAD_SCIENTIST_CONTACT_IMAGE_URL)
            }
        }

        refreshMafiaChannelContactState(game)
    }

    suspend fun refreshMafiaChannelContactState(game: Game) {
        val mafiaChannel = game.mafiaChannel ?: return
        updateMafiaChannelPermissions(game, mafiaChannel, isNight = game.currentPhase == GamePhase.NIGHT)
    }

    suspend fun announceMafiaSupportContact(
        game: Game,
        contactPlayer: PlayerData,
        contactImageUrl: String,
        supportJobNameOverride: String? = null
    ) {
        refreshMafiaChannelContactState(game)

        val aliveMafiaMentions = game.playerDatas
            .filter { !it.state.isDead && it.job is Mafia }
            .map(::mention)

        val mafiaDescription = if (aliveMafiaMentions.isEmpty()) {
            "마피아가 없는 상태에서"
        } else {
            "마피아 ${aliveMafiaMentions.joinToString(", ")}님과"
        }

        val supportJobName = supportJobNameOverride ?: contactPlayer.job?.name ?: "악인"
        val channelMessage =
            "$contactImageUrl\n$mafiaDescription $supportJobName ${mention(contactPlayer)}님이 접선했습니다."
        val directMessage = "$contactImageUrl\n$mafiaDescription 접선했습니다."

        runCatching { game.mafiaChannel?.createMessage(channelMessage) }
        GameReplayLogger.logDirectMessage(game, contactPlayer, directMessage, "$supportJobName 접선")
        runCatching { contactPlayer.member.getDmChannel().createMessage(directMessage) }
    }

    private fun mention(player: PlayerData): String = "<@${player.member.id.value}>"

    suspend fun notifyHitmanContact(game: Game, hitmanPlayer: PlayerData) {
        if (hitmanPlayer.state.hasAnnouncedHitmanContact) return
        hitmanPlayer.state.hasAnnouncedHitmanContact = true
        announceMafiaSupportContact(
            game = game,
            contactPlayer = hitmanPlayer,
            contactImageUrl = HITMAN_CONTACT_IMAGE_URL
        )
        refreshMafiaChannelContactState(game)
    }

    private fun applyHostessSeductionStates(game: Game) {
        game.playerDatas.forEach { target ->
            target.state.isSilenced = isSeducedAtCurrentTime(game, target)
        }
    }

    private fun isSeducedAtCurrentTime(game: Game, target: PlayerData): Boolean {
        if (target.state.isDead) return false
        val seduction = game.seductionStatusByTarget[target.member.id] ?: return false
        if (target.job is Soldier && target.allAbilities.any { it is MentalStrength }) return false

        val hostess = game.getPlayer(seduction.hostessId)
        if (hostess != null && FrogCurseManager.shouldSuppressPassive(hostess)) return false
        val hostessAlive = hostess != null && !hostess.state.isDead && hostess.job is Hostess
        return seduction.isPermanent || hostessAlive || game.dayCount <= seduction.minimumReleaseDay
    }

    private suspend fun applyHostessSeductionFromVote(game: Game) {
        val seductionTargetsByHostess = mutableMapOf<PlayerData, MutableSet<Snowflake>>()

        game.currentMainVotes.forEach { (voterId, targetIdString) ->
            val voter = game.getPlayer(voterId) ?: return@forEach
            if (voter.state.isDead || voter.job !is Hostess) return@forEach
            if (FrogCurseManager.shouldSuppressPassive(voter)) return@forEach
            val target = game.getPlayer(Snowflake(targetIdString)) ?: return@forEach
            if (target.state.isDead) return@forEach
            seductionTargetsByHostess.getOrPut(voter) { mutableSetOf() } += target.member.id
        }

        if (game.dayCount == 1) {
            game.hostessFirstVoteTargetByDay.forEach { (hostessId, firstTargetId) ->
                val hostessPlayer = game.getPlayer(hostessId) ?: return@forEach
                if (hostessPlayer.state.isDead || hostessPlayer.job !is Hostess) return@forEach
                if (FrogCurseManager.shouldSuppressPassive(hostessPlayer)) return@forEach
                val firstTarget = game.getPlayer(firstTargetId) ?: return@forEach
                if (firstTarget.state.isDead) return@forEach
                seductionTargetsByHostess.getOrPut(hostessPlayer) { mutableSetOf() } += firstTarget.member.id
            }
        }

        seductionTargetsByHostess.forEach { (hostessPlayer, targetIds) ->
            val hostessJob = hostessPlayer.job as? Hostess ?: return@forEach
            targetIds.forEach { targetId ->
                applyHostessSeduction(
                    game = game,
                    hostessPlayer = hostessPlayer,
                    hostessJob = hostessJob,
                    targetId = targetId
                )
            }
        }
        applyHostessSeductionStates(game)
    }

    private suspend fun applyHostessSeduction(
        game: Game,
        hostessPlayer: PlayerData,
        hostessJob: Hostess,
        targetId: Snowflake
    ) {
        if (FrogCurseManager.shouldSuppressPassive(hostessPlayer)) return
        val target = game.getPlayer(targetId) ?: return
        if (target.state.isDead) return
        if (shouldIgnoreHarmfulEffectByMentalStrength(game, target)) return

        val hasDeception = hostessPlayer.allAbilities.any { it is Deception }
        val minimumReleaseDay = game.dayCount + 1
        val existing = game.seductionStatusByTarget[target.member.id]
        if (existing == null || existing.minimumReleaseDay < minimumReleaseDay) {
            game.seductionStatusByTarget[target.member.id] = SeductionStatus(
                hostessId = hostessPlayer.member.id,
                minimumReleaseDay = minimumReleaseDay,
                isPermanent = hasDeception
            )
        } else if (hasDeception) {
            existing.isPermanent = true
        }

        if (!hostessJob.hasContactedMafia && target.job is Mafia) {
            hostessJob.hasContactedMafia = true
            announceMafiaSupportContact(game, hostessPlayer, HOSTESS_CONTACT_IMAGE_URL)
            refreshMafiaChannelContactState(game)
        }
    }

    private fun resolveOriginallySelectedMafiaTarget(game: Game, mafiaAttack: AttackEvent): PlayerData {
        val selectedTargetId = game.coupleSacrificeMap[mafiaAttack.target.member.id] ?: return mafiaAttack.target
        return game.getPlayer(selectedTargetId) ?: mafiaAttack.target
    }

    private fun resolveBeastmanCravingTaming(
        game: Game,
        mafiaAttack: AttackEvent?,
        successfulAttacks: Set<AttackEvent>,
        playersToDie: Set<PlayerData>
    ) {
        val executedTarget = mafiaAttack
            ?.takeIf { it in successfulAttacks }
            ?.target

        game.playerDatas.forEach { player ->
            val beastman = player.job as? Beastman ?: return@forEach
            val cravingTargetId = beastman.cravingTargetIdTonight ?: return@forEach
            beastman.cravingTargetIdTonight = null

            if (executedTarget == null) return@forEach
            if (player.state.isDead || player.state.isTamed || player in playersToDie) return@forEach
            if (FrogCurseManager.shouldSuppressPassive(player)) return@forEach
            if (cravingTargetId != executedTarget.member.id) return@forEach

            game.pendingBeastmanTameIds += player.member.id
        }
    }

    private fun tameBeastmanByBarbarismIfNeeded(game: Game, target: PlayerData) {
        if (target.state.isDead || target.state.isTamed) return
        if (target.job !is Beastman) return
        if (FrogCurseManager.shouldSuppressPassive(target)) return
        if (target.allAbilities.none { it is Barbarism }) return

        game.pendingBeastmanTameIds += target.member.id
    }

    private fun isExecutionImmuneBeastmanTarget(attackEvent: AttackEvent): Boolean {
        if (attackEvent.target.job !is Beastman) return false
        if (FrogCurseManager.shouldSuppressPassive(attackEvent.target)) return false
        return attackEvent.target.allAbilities.any { it is BeastmanAgility }
    }

    private fun isMafiaTeamAttackKey(attackKey: String): Boolean {
        return attackKey == "MAFIA_TEAM" || attackKey.startsWith("GODFATHER_")
    }

    private suspend fun notifyPendingBeastmanTaming(game: Game) {
        if (game.pendingBeastmanTameIds.isEmpty()) return

        val targetIds = game.pendingBeastmanTameIds.toSet()
        game.pendingBeastmanTameIds.clear()

        val mafiaPlayers = game.playerDatas.filter { it.job is Mafia }
        targetIds.forEach { beastmanId ->
            val beastmanPlayer = game.getPlayer(beastmanId) ?: return@forEach
            if (beastmanPlayer.state.isDead) return@forEach
            if (beastmanPlayer.job !is Beastman) return@forEach

            beastmanPlayer.state.isTamed = true

            GameReplayLogger.logDirectMessage(
                game = game,
                recipient = beastmanPlayer,
                body = "$BEASTMAN_TAMED_IMAGE_URL\n길들여졌습니다.",
                title = "짐승인간 접선"
            )
            runCatching {
                beastmanPlayer.member.getDmChannel().createMessage("$BEASTMAN_TAMED_IMAGE_URL\n길들여졌습니다.")
            }

            announceMafiaSupportContact(game, beastmanPlayer, BEASTMAN_TAMED_IMAGE_URL)

            mafiaPlayers.forEach { mafiaPlayer ->
                GameReplayLogger.logDirectMessage(
                    game = game,
                    recipient = mafiaPlayer,
                    body = "$BEASTMAN_TAMED_IMAGE_URL\n${mention(beastmanPlayer)}님이 접선했습니다.",
                    title = "짐승인간 접선"
                )
                runCatching {
                    mafiaPlayer.member.getDmChannel().createMessage("$BEASTMAN_TAMED_IMAGE_URL\n${mention(beastmanPlayer)}님이 접선했습니다.")
                }
            }
        }

        refreshMafiaChannelContactState(game)
    }

    private suspend fun notifyBeastmanRoarAtFirstDay(game: Game) {
        if (game.dayCount != 1) return
        val mafiaChannel = game.mafiaChannel ?: return

        val hasAliveRoarBeastman = game.playerDatas.any { player ->
            !player.state.isDead &&
                player.job is Beastman &&
                !FrogCurseManager.shouldSuppressPassive(player) &&
                player.allAbilities.any { it is Roar }
        }
        if (!hasAliveRoarBeastman) return

        mafiaChannel.createMessage("$BEASTMAN_ROAR_IMAGE_URL\n짐승의 포효소리가 들려왔습니다!")
    }

    private suspend fun updateCoupleChannelPermissions(game: Game, coupleChannel: TextChannel, isNight: Boolean) {
        coupleChannel.edit {
            addRoleOverwrite(game.guild.id) {
                denied = Permissions(
                    Permission.ViewChannel,
                    Permission.ReadMessageHistory,
                    Permission.SendMessages
                )
            }

            game.playerDatas.forEach { player ->
                if (player.state.isDead) {
                    addMemberOverwrite(player.member.id) {
                        allowed = Permissions(Permission.ViewChannel)
                        denied = Permissions(Permission.ReadMessageHistory, Permission.SendMessages)
                    }
                    return@forEach
                }

                if (player.allAbilities.any { it is CoupleAbility }) {
                    val isSuppressed = FrogCurseManager.shouldSuppressPassive(player)
                    val canAccess = isNight && !shouldRestrictCommunication(player) && !isSuppressed
                    addMemberOverwrite(player.member.id) {
                        allowed = if (isSuppressed) {
                            Permissions()
                        } else if (canAccess) {
                            Permissions(
                                Permission.ViewChannel,
                                Permission.SendMessages
                            )
                        } else {
                            Permissions(Permission.ViewChannel)
                        }
                        denied = if (isSuppressed) {
                            Permissions(
                                Permission.ViewChannel,
                                Permission.ReadMessageHistory,
                                Permission.SendMessages
                            )
                        } else if (canAccess) {
                            Permissions(Permission.ReadMessageHistory)
                        } else {
                            Permissions(Permission.ReadMessageHistory, Permission.SendMessages)
                        }
                    }
                } else {
                    addMemberOverwrite(player.member.id) {
                        denied = Permissions(
                            Permission.ViewChannel,
                            Permission.ReadMessageHistory,
                            Permission.SendMessages
                        )
                    }
                }
            }

            game.spectatorMembers.forEach { spectator ->
                addMemberOverwrite(spectator.id) {
                    allowed = Permissions(Permission.ViewChannel)
                    denied = Permissions(Permission.ReadMessageHistory, Permission.SendMessages)
                }
            }
        }
    }

    suspend fun refreshCoupleChannelAccess(game: Game) {
        game.coupleChannel?.let {
            updateCoupleChannelPermissions(game, it, game.currentPhase == GamePhase.NIGHT)
        }
        game.deadChannel?.let { updateDeadChannelPermissions(game, it) }
    }

    private suspend fun updateDeadChannelPermissions(game: Game, deadChannel: TextChannel) {
        deadChannel.edit {
            addRoleOverwrite(game.guild.id) {
                denied = Permissions(
                    Permission.ViewChannel,
                    Permission.ReadMessageHistory,
                    Permission.SendMessages
                )
            }

            game.playerDatas.forEach { player ->
                if (
                    !player.state.isDead &&
                    (
                        player.allAbilities.none { it is ShamanAbilityOne } ||
                            FrogCurseManager.shouldSuppressPassive(player)
                    )
                ) {
                    addMemberOverwrite(player.member.id) {
                        denied = Permissions(
                            Permission.ViewChannel,
                            Permission.ReadMessageHistory,
                            Permission.SendMessages
                        )
                    }
                    return@forEach
                }

                addMemberOverwrite(player.member.id) {
                    allowed = if (player.state.isDead) {
                        Permissions(
                            Permission.ViewChannel,
                            Permission.SendMessages
                        )
                    } else {
                        Permissions(Permission.ViewChannel)
                    }
                    denied = if (player.state.isDead) {
                        Permissions(Permission.ReadMessageHistory)
                    } else {
                        Permissions(Permission.ReadMessageHistory, Permission.SendMessages)
                    }
                }
            }

            game.spectatorMembers.forEach { spectator ->
                addMemberOverwrite(spectator.id) {
                    allowed = Permissions(Permission.ViewChannel)
                    denied = Permissions(Permission.ReadMessageHistory, Permission.SendMessages)
                }
            }
        }
    }

    suspend fun startVotePhase(game: Game) {
        val mainChannel = game.mainChannel ?: return
        processEscapedPlayerDeaths(game)
        game.currentPhase = GamePhase.VOTE
        GameReplayLogger.logPhase(game, "${game.dayCount}일차 투표")
        game.currentMainVotes.clear()
        game.currentFakeVotes.clear()
        game.currentProsConsVotes.clear()
        game.judgeAuthorityDisabledByThiefIds.clear()
        game.hostessFirstVoteTargetByDay.clear()
        game.defenseTargetId = null
        game.playerDatas.forEach { player ->
            (player.job as? Thief)?.hasUsedTheftThisVote = false
        }
        muteSpectators(game)

        val alivePlayers = game.playerDatas.filter { !it.state.isDead }

        game.sendMainChannelMessageWithImageAndSound(
            imageLink = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(10).png",
            message = "투표 시간입니다. 의심되는 사람을 투표하세요.",
            soundPath = VOTE_PHASE_SOUND_PATH,
            soundVolume = 50,
            loopSound = true
        )
        mainChannel.createMessage {
            actionRow {
                stringSelect("main_vote_select") {
                    placeholder = "처형할 플레이어 선택"
                    alivePlayers.forEach { player ->
                        option(player.member.effectiveName, player.member.id.toString()) {
                            description = "이 플레이어에게 투표합니다."
                        }
                    }
                }
            }
        }

        val voteStatusMessage = mainChannel.createMessage {
            content = buildMainVoteStatusContent(game, alivePlayers, isHidden = false)
        }

        votePresentationScope.launch {
            val refreshInterval = 1_000L
            val refreshCount = (INITIAL_VOTE_REVEAL_DURATION_MS / refreshInterval).toInt()

            repeat(refreshCount) {
                delay(refreshInterval.milliseconds)
                runCatching {
                    voteStatusMessage.edit {
                        content = buildMainVoteStatusContent(game, alivePlayers, isHidden = false)
                    }
                }
            }

            runCatching {
                voteStatusMessage.edit {
                    content = buildMainVoteStatusContent(game, alivePlayers, isHidden = true)
                }
            }
        }
    }

    suspend fun resolveVotePhase(game: Game): PlayerData? {
        val mainChannel = game.mainChannel ?: return null
        val mainVoteSnapshot = synchronized(game) {
            game.currentPhase = GamePhase.DAY
            game.currentMainVotes.toMap()
        }
        val alivePlayers = game.playerDatas.filter { !it.state.isDead }
        applyHostessSeductionFromVote(game)
        val dictatorshipPolitician = findAliveDictatorshipPolitician(game)
        if (dictatorshipPolitician != null) {
            val politicianVoteTargetId = mainVoteSnapshot[dictatorshipPolitician.member.id]
            val politicianTarget = politicianVoteTargetId
                ?.let { targetId -> game.getPlayer(Snowflake(targetId)) }
                ?.takeUnless { it.state.isDead }
            return if (politicianTarget != null) {
                resolveMagicianTrickSubstitution(game, politicianTarget, mainVoteSnapshot)
            } else {
                game.sendMainChannelMessageWithImage(
                    imageLink = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(60).webp",
                    message = "처형될 대상을 고르지 못했습니다."
                )
                null
            }
        }
        val authorityJudge = findRevealedAliveJudge(game)
        if (authorityJudge != null) {
            val judgeVoteTargetId = mainVoteSnapshot[authorityJudge.member.id]
            val judgeTarget = judgeVoteTargetId
                ?.let { targetId -> game.getPlayer(Snowflake(targetId)) }
                ?.takeUnless { it.state.isDead }
            return if (judgeTarget != null) {
                resolveMagicianTrickSubstitution(game, judgeTarget, mainVoteSnapshot)
            } else {
                game.sendMainChannelMessageWithImage(
                    imageLink = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(60).webp",
                    message = "처형될 대상을 고르지 못했습니다."
                )
                null
            }
        }
        val voteCounts = mutableMapOf<PlayerData, Int>()
        val fakeVoteCounts = mutableMapOf<PlayerData, Int>()
        var invalidVoteCount = 0
        val weightedVoteTargets = mutableListOf<PlayerData>()
        val gangsterTransferredVoteWeights = calculateTransferredVoteWeights(game)

        alivePlayers.forEach { voter ->
            if (voter.member.id in game.permanentlyDisenfranchisedVoters) {
                return@forEach
            }
            if (game.activeThreatenedVoters.containsKey(voter.member.id)) {
                return@forEach
            }

            val baseWeight = baseVoteWeight(voter)
            val weightEvent = GameEvent.CalculateVoteWeight(voter, weight = baseWeight)
            voter.allAbilities
                 .filterIsInstance<PassiveAbility>()
                .filterNot { FrogCurseManager.shouldSuppressPassive(voter) }
                .sortedByDescending(PassiveAbility::priority)
                .forEach { passive ->
                    passive.onEventObserved(game, voter, weightEvent)
                }
            weightEvent.weight += gangsterTransferredVoteWeights[voter.member.id] ?: 0

            if (
                voter.job is MadScientist &&
                !FrogCurseManager.shouldSuppressPassive(voter) &&
                voter.allAbilities.any { it is Analysis } &&
                voter.state.madScientistAnalysisEligibleDay == game.dayCount &&
                !voter.state.hasUsedMadScientistAnalysis
            ) {
                val targetId = mainVoteSnapshot[voter.member.id]?.let(::Snowflake)
                if (targetId != null && targetId == voter.state.madScientistLynchedVoteTargetId) {
                    weightEvent.weight += 1
                    voter.state.hasUsedMadScientistAnalysis = true
                }
            }

            if (weightEvent.weight <= 0) {
                return@forEach
            }

            val targetIdString = mainVoteSnapshot[voter.member.id]
            if (targetIdString == null) {
                invalidVoteCount += weightEvent.weight
                return@forEach
            }

            val target = game.getPlayer(Snowflake(targetIdString)) ?: return@forEach
            if (target.state.isDead) {
                invalidVoteCount += weightEvent.weight
                return@forEach
            }
            voteCounts[target] = (voteCounts[target] ?: 0) + weightEvent.weight
            repeat(weightEvent.weight.coerceAtLeast(0)) {
                weightedVoteTargets += target
            }
        }

        game.currentFakeVotes.forEach { (voterId, targetId) ->
            val voter = game.getPlayer(voterId) ?: return@forEach
            val target = game.getPlayer(targetId) ?: return@forEach
            if (voter.state.isDead || target.state.isDead) return@forEach
            if (FrogCurseManager.shouldSuppressPassive(voter)) return@forEach
            if (voter.member.id in game.permanentlyDisenfranchisedVoters) return@forEach
            if (game.activeThreatenedVoters.containsKey(voter.member.id)) return@forEach
            fakeVoteCounts[target] = (fakeVoteCounts[target] ?: 0) + 1
            // 위증은 투표 공개에만 반영하고 실제 최다 득표/동표 판정에는 포함하지 않는다.
            weightedVoteTargets += target
        }

        if (weightedVoteTargets.isNotEmpty()) {
            delay(1_000L.milliseconds)
            val progressiveVoteCounts = mutableMapOf<PlayerData, Int>()
            val tallyMessage = mainChannel.createMessage {
                content = buildFinalVoteTallyContent(alivePlayers, progressiveVoteCounts)
            }

            weightedVoteTargets.forEach { target ->
                progressiveVoteCounts[target] = (progressiveVoteCounts[target] ?: 0) + 1
                runCatching {
                    tallyMessage.edit {
                        content = buildFinalVoteTallyContent(
                            alivePlayers = alivePlayers,
                            voteCounts = progressiveVoteCounts,
                            fakeVoteCounts = fakeVoteCounts
                        )
                    }
                }
                delay(FINAL_VOTE_TALLY_STEP_MS.milliseconds)
            }
        }

        val maxVotes = voteCounts.values.maxOrNull() ?: 0
        if (invalidVoteCount > maxVotes || maxVotes == 0) {
            game.sendMainChannelMessageWithImage(
                imageLink = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(60).webp",
                message = "처형될 대상을 고르지 못했습니다."
            )
            return null
        }

        val maxVotedPlayers = voteCounts.filter { it.value == maxVotes }.keys.toList()

        if (maxVotedPlayers.size > 1) {
            val halfThreshold = (alivePlayers.size + 1) / 2
            if (maxVotes < halfThreshold) {
                val juryResolved = resolveJuryTarget(game, maxVotedPlayers, mainVoteSnapshot)
                if (juryResolved != null) {
                    game.sendMainChannerMessage("배심원의 결정으로 인해 투표 대상이 정해졌습니다!")
                    if (isInnocentTarget(game, juryResolved)) {
                        game.sendMainChannelMessageWithImage(
                            imageLink = INNOCENCE_IMAGE_URL,
                            message = "${juryResolved.member.effectiveName}님은 결백합니다!"
                        )
                        return null
                    }
                    return resolveMagicianTrickSubstitution(game, juryResolved, mainVoteSnapshot)
                }
            }
            game.sendMainChannelMessageWithImage(
                imageLink = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(60).webp",
                message = "처형될 대상을 고르지 못했습니다."
            )
            return null
        }

        val finalTarget = maxVotedPlayers.first()
        if (isInnocentTarget(game, finalTarget)) {
            game.sendMainChannelMessageWithImage(
                imageLink = INNOCENCE_IMAGE_URL,
                message = "${finalTarget.member.effectiveName}님은 결백합니다!"
            )
            return null
        }
        return resolveMagicianTrickSubstitution(game, finalTarget, mainVoteSnapshot)
    }

    private suspend fun resolveMagicianTrickSubstitution(
        game: Game,
        originalTarget: PlayerData,
        mainVoteSnapshot: Map<Snowflake, String>
    ): PlayerData {
        val magician = originalTarget.actualOrStolenJob<Magician>() ?: return originalTarget
        val thief = originalTarget.job as? Thief
        if (thief?.hasUsedStolenMagicianTrick == true) return originalTarget
        if (originalTarget.state.isDead || magician.hasUsedTrick) return originalTarget
        if (FrogCurseManager.shouldSuppressPassive(originalTarget)) return originalTarget

        val substitute = magician.trickTargetId
            ?.let(game::getPlayer)
            ?.takeUnless { it.state.isDead }
            ?.takeUnless { it.member.id == originalTarget.member.id }
            ?: return originalTarget

        magician.hasUsedTrick = true
        thief?.hasUsedStolenMagicianTrick = true
        magician.trickSubstitutedTargetId = substitute.member.id
        magician.trickTargetId = substitute.member.id
        originalTarget.state.isJobPubliclyRevealed = true
        game.publiclyRevealedJobNames += magician.name

        val publicMessage =
            "마술사 (${originalTarget.member.effectiveName})님이 최다 득표자를 ${substitute.member.effectiveName}님으로 바꿔치기했습니다!"
        game.sendMainChannelMessageWithImage(
            imageLink = MAGICIAN_TRICK_SUCCESS_IMAGE_URL,
            message = publicMessage
        )
        GameReplayLogger.log(
            game = game,
            type = ReplayLogType.ABILITY_USED,
            visibility = ReplayVisibility.PUBLIC,
            title = "트릭",
            body = publicMessage,
            actor = originalTarget,
            imageUrls = listOf(MAGICIAN_TRICK_SUCCESS_IMAGE_URL)
        )

        if (originalTarget.allAbilities.any { it is Xray }) {
            val votedTargetName = mainVoteSnapshot[substitute.member.id]
                ?.let { targetIdString -> runCatching { Snowflake(targetIdString) }.getOrNull() }
                ?.let(game::getPlayer)
                ?.takeUnless { it.state.isDead }
                ?.member
                ?.effectiveName
            val xrayMessage = if (votedTargetName == null) {
                "${substitute.member.effectiveName}님은 이번 투표에서 유효한 대상에게 투표하지 않았습니다."
            } else {
                "${substitute.member.effectiveName}님은 이번 투표에서 ${votedTargetName}님에게 투표했습니다."
            }
            GameReplayLogger.logDirectMessage(game, originalTarget, xrayMessage, "투시")
            runCatching {
                originalTarget.member.getDmChannel().createMessage(xrayMessage)
            }
        }

        return substitute
    }

    private fun buildMainVoteStatusContent(
        game: Game,
        alivePlayers: List<PlayerData>,
        isHidden: Boolean
    ): String {
        val currentVoteCounts = mutableMapOf<PlayerData, Int>()
        val mainVotes = synchronized(game) { game.currentMainVotes.toMap() }
        val fakeVoteTargetIds = synchronized(game) { game.currentFakeVotes.values.toList() }

        mainVotes.forEach { (_, targetId) ->
            val target = game.getPlayer(Snowflake(targetId)) ?: return@forEach
            if (target.state.isDead) return@forEach
            currentVoteCounts[target] = (currentVoteCounts[target] ?: 0) + 1
        }
        fakeVoteTargetIds.forEach { targetId ->
            val target = game.getPlayer(targetId) ?: return@forEach
            if (target.state.isDead) return@forEach
            currentVoteCounts[target] = (currentVoteCounts[target] ?: 0) + 1
        }

        return buildString {
            alivePlayers.forEach { player ->
                val voteDisplay = if (isHidden) "?" else (currentVoteCounts[player] ?: 0).toString()
                appendLine("- ${player.member.effectiveName}: ${voteDisplay}표")
            }
        }
    }

    private fun buildFinalVoteTallyContent(
        alivePlayers: List<PlayerData>,
        voteCounts: Map<PlayerData, Int>,
        fakeVoteCounts: Map<PlayerData, Int> = emptyMap()
    ): String {
        return buildString {
            alivePlayers.forEach { player ->
                val total = (voteCounts[player] ?: 0)
                val fakeCount = fakeVoteCounts[player] ?: 0
                if (fakeCount > 0) {
                    appendLine("- ${player.member.effectiveName}: ${total}표")
                } else {
                    appendLine("- ${player.member.effectiveName}: ${total}표")
                }
            }
        }
    }

    private fun resolveJuryTarget(
        game: Game,
        tiedTargets: List<PlayerData>,
        mainVoteSnapshot: Map<Snowflake, String>
    ): PlayerData? {
        val juryVoteCounts = mutableMapOf<PlayerData, Int>()
        mainVoteSnapshot.forEach { (voterId, targetIdString) ->
            val voter = game.getPlayer(voterId) ?: return@forEach
            if (!isEffectiveJuryVoter(game, voter)) return@forEach
            val targetId = runCatching { Snowflake(targetIdString) }.getOrNull() ?: return@forEach
            val target = game.getPlayer(targetId) ?: return@forEach
            if (target !in tiedTargets) return@forEach
            juryVoteCounts[target] = (juryVoteCounts[target] ?: 0) + 1
        }

        if (juryVoteCounts.isEmpty()) return null
        val maxJuryVotes = juryVoteCounts.values.maxOrNull() ?: return null
        val topTargets = juryVoteCounts.filterValues { it == maxJuryVotes }.keys
        return topTargets.singleOrNull()
    }

    private fun isEffectiveJuryVoter(game: Game, voter: PlayerData): Boolean {
        return !voter.state.isDead &&
            hasJuryAbility(voter) &&
            voter.member.id !in game.permanentlyDisenfranchisedVoters &&
            !game.activeThreatenedVoters.containsKey(voter.member.id)
    }

    private fun hasJuryAbility(player: PlayerData): Boolean {
        return !FrogCurseManager.shouldSuppressPassive(player) &&
            player.allAbilities.any { it is Jury }
    }

    private fun canSpeakDuringDefense(player: PlayerData, target: PlayerData): Boolean {
        return !shouldRestrictCommunication(player) &&
            (player.member.id == target.member.id || hasJuryAbility(player))
    }

    private fun isInnocentTarget(game: Game, candidate: PlayerData): Boolean {
        if (FrogCurseManager.shouldSuppressPassive(candidate)) return false
        if (candidate.allAbilities.none { it is Innocence }) return false
        val candidateVoteTargetId = game.currentMainVotes[candidate.member.id] ?: return false
        val candidateVoteTarget = game.getPlayer(Snowflake(candidateVoteTargetId)) ?: return false
        if (candidateVoteTarget.state.isDead) return false

        val sameTeam = (candidate.job is Evil) == (candidateVoteTarget.job is Evil)
        if (sameTeam) return false

        val reverseVoteTargetId = game.currentMainVotes[candidateVoteTarget.member.id]
        return reverseVoteTargetId != candidate.member.id.toString()
    }

    private fun notifyMindReadingResults(game: Game) {
        if (game.currentMainVotes.isEmpty()) return

        game.playerDatas
            .filter {
                !it.state.isDead &&
                    !FrogCurseManager.shouldSuppressPassive(it) &&
                    it.allAbilities.any { ability -> ability is MindReading }
            }
            .forEach { mindReader ->
                val voters = game.currentMainVotes
                    .filterValues { it == mindReader.member.id.toString() }
                    .keys
                    .mapNotNull { voterId -> game.getPlayer(voterId)?.member?.effectiveName }

                cabalNotificationScope.launch {
                    runCatching {
                        val message = if (voters.isEmpty()) {
                            "이번 투표에서 당신에게 투표한 사람이 없습니다."
                        } else {
                            "당신에게 투표한 사람은 ${voters.joinToString(", ")}"
                        }
                        GameReplayLogger.logDirectMessage(game, mindReader, message, "독심 결과")
                        mindReader.member.getDmChannel().createMessage(message)
                    }
                }
            }
    }

    private suspend fun processEscapedPlayerDeaths(game: Game) {
        val pendingTargets = game.pendingEscapedPlayerIds.toList()
        if (pendingTargets.isEmpty()) return

        pendingTargets.forEach { escapedPlayerId ->
            val escapedPlayer = game.getPlayer(escapedPlayerId) ?: run {
                game.pendingEscapedPlayerIds.remove(escapedPlayerId)
                return@forEach
            }
            if (escapedPlayer.state.isDead) {
                game.pendingEscapedPlayerIds.remove(escapedPlayerId)
                return@forEach
            }

            processPlayerDeath(game, escapedPlayer, isLynch = true)
            game.pendingEscapedPlayerIds.remove(escapedPlayerId)
            game.sendMainChannelMessageWithImage(
                imageLink = ESCAPE_DEATH_IMAGE_URL,
                message = "투표에서 도주한 ${escapedPlayer.member.effectiveName}님이 사망하였습니다."
            )
            revealBelongingsIfNeeded(game, escapedPlayer)
        }

        dispatchEvents(game)
        game.nightEvents.clear()
        game.deadChannel?.let { updateDeadChannelPermissions(game, it) }
        refreshMafiaChannelContactState(game)
    }

    private fun deliverSecretLetters(game: Game) {
        if (game.pendingLettersByRecipient.isEmpty()) return
        val deliveries = game.pendingLettersByRecipient.toMap()
        game.pendingLettersByRecipient.clear()

        deliveries.forEach { (recipientId, letters) ->
            val recipient = game.getPlayer(recipientId) ?: return@forEach
            cabalNotificationScope.launch {
                runCatching {
                    letters.forEach { letter ->
                        GameReplayLogger.logDirectMessage(
                            game = game,
                            recipient = recipient,
                            body = "${letter.title}\n${letter.content}",
                            title = "밀서 배달"
                        )
                    }
                    recipient.member.getDmChannel().createMessage {
                        letters.forEach { letter ->
                            embed {
                                title = letter.title
                                description = letter.content
                            }
                        }
                    }
                }
            }
        }
    }

    private fun buildNightWillAnnouncements(
        game: Game,
        deadPlayers: List<PlayerData>
    ): List<DawnAnnouncement> {
        val willOwners = deadPlayers.filter { player ->
            player.allAbilities.any { it is Will } && game.willByPlayerId[player.member.id]?.isNotBlank() == true
        }

        return willOwners.mapNotNull { player ->
            val willMessage = game.willByPlayerId.remove(player.member.id) ?: return@mapNotNull null
            DawnAnnouncement(
                imageUrl = "",
                message = "${player.member.effectiveName}: $willMessage",
                priority = DAWN_PRIORITY_WILL,
                targetId = player.member.id,
                embedTitle = "유언"
            )
        }
    }

    suspend fun startDefensePhase(game: Game, target: PlayerData) {
        val mainChannel = game.mainChannel ?: return
        game.currentPhase = GamePhase.VOTE
        game.defenseTargetId = target.member.id
        GameReplayLogger.logPhase(game, "${target.member.effectiveName} 최후 변론")
        (target.job as? Martyr)?.defenseBombTargetId = null
        game.sendMainChannelMessageWithImage(
            imageLink = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(1).png",
            message = "${target.member.effectiveName}의 최후의 변론"
        )

        mainChannel.edit {
            addRoleOverwrite(game.guild.id) {
                allowed = Permissions(Permission.ReadMessageHistory)
                denied = Permissions(Permission.SendMessages)
            }

            game.playerDatas.forEach { player ->
                addMemberOverwrite(player.member.id) {
                    if (canSpeakDuringDefense(player, target)) {
                        allowed = Permissions(Permission.SendMessages)
                        denied = Permissions()
                    } else {
                        allowed = Permissions()
                        denied = Permissions(Permission.SendMessages)
                    }
                }
            }
        }

        game.playerDatas.forEach { player ->
            val shouldMute = !canSpeakDuringDefense(player, target)
            runCatching {
                player.member.edit {
                    muted = shouldMute
                }
            }
        }
        muteSpectators(game)
    }

    suspend fun startProsConsVotePhase(game: Game, target: PlayerData) {
        val mainChannel = game.mainChannel ?: return
        synchronized(game) {
            game.currentPhase = GamePhase.VOTE
            game.defenseTargetId = target.member.id
            game.currentProsConsVotes.clear()
        }
        GameReplayLogger.logPhase(game, "${target.member.effectiveName} 찬반 투표")

        game.playerDatas.forEach { player ->
            val shouldMute = shouldRestrictCommunication(player)
            runCatching {
                player.member.edit {
                    muted = shouldMute
                }
            }
        }
        muteSpectators(game)

        mainChannel.edit {
            addRoleOverwrite(game.guild.id) {
                allowed = Permissions(Permission.ReadMessageHistory)
                denied = Permissions(Permission.SendMessages)
            }

            game.playerDatas.forEach { player ->
                addMemberOverwrite(player.member.id) {
                    allowed = Permissions()
                    denied = Permissions()
                }
            }
        }

        mainChannel.createMessage {
            actionRow {
                stringSelect("$PROS_CONS_VOTE_COMPONENT_ID_PREFIX:${target.member.id}") {
                    placeholder = "찬성 / 반대 선택"
                    option("찬성", "pros") {
                        description = "처형에 찬성합니다."
                    }
                    option("반대", "cons") {
                        description = "처형에 반대합니다."
                    }
                }
            }
        }
    }

    suspend fun resolveExecutionPhase(game: Game, target: PlayerData) {
        val mainChannel = game.mainChannel ?: return
        val deadChannel = game.deadChannel
        val prosConsVotes = synchronized(game) {
            game.currentPhase = GamePhase.DAY
            game.currentProsConsVotes.toMap()
        }
        val dictatorshipPolitician = findAliveDictatorshipPolitician(game)
        val gangsterTransferredVoteWeights = calculateTransferredVoteWeights(game)
        val prosCount = prosConsVotes
            .filterValues { it }
            .keys
            .sumOf { voterId -> calculateProsConsVoteWeight(game, voterId, target, gangsterTransferredVoteWeights) }
        val consCount = prosConsVotes
            .filterValues { !it }
            .keys
            .sumOf { voterId -> calculateProsConsVoteWeight(game, voterId, target, gangsterTransferredVoteWeights) }
        val aggregateDecision = prosCount > consCount
        val judgePlayer = findProsConsJudge(game, prosConsVotes, aggregateDecision)
        val judgeVote = judgePlayer?.let { prosConsVotes[it.member.id] }
        val shouldRevealJudge = judgePlayer != null &&
            !hasRevealedJudgeAuthority(judgePlayer) &&
            judgeVote != null &&
            judgeVote != aggregateDecision

        if (shouldRevealJudge) {
            revealJudgeAuthority(judgePlayer)
            judgePlayer.state.isJobPubliclyRevealed = true
            judgePlayer.job?.name?.let { game.publiclyRevealedJobNames += it }
            if (judgePlayer.allAbilities.any { it is UnwrittenRule }) {
                game.unwrittenRuleBlockedTargetIdTonight = judgePlayer.member.id
            }

            game.sendMainChannelMessageWithImage(
                JUDGE_VERDICT_IMAGE_URL,
                "판사 (${judgePlayer.member.effectiveName})님이 투표 결과를 정했습니다!"
            )
        }

        notifyJudgeProsVoters(game, target, prosConsVotes)

        if (findRevealedAliveJudge(game) != null && judgeVote == null) {
            mainChannel.createMessage("${target.member.effectiveName}님의 처형이 부결되었습니다.")
        }

        val finalDecision = when {
            dictatorshipPolitician != null -> prosConsVotes[dictatorshipPolitician.member.id] ?: false
            findRevealedAliveJudge(game) != null -> judgeVote ?: false
            else -> aggregateDecision
        }

        val executionEvent = GameEvent.DecideExecution(target, finalDecision)
        val alivePlayers = game.playerDatas.filter { !it.state.isDead }

        alivePlayers.forEach { player ->
            player.allAbilities
                 .filterIsInstance<PassiveAbility>()
                .filterNot { FrogCurseManager.shouldSuppressPassive(player) }
                .sortedByDescending(PassiveAbility::priority)
                .forEach { passive ->
                    passive.onEventObserved(game, player, executionEvent)
                }
        }

        if (!executionEvent.isApproved) {
            game.sendMainChannelMessageWithImage(
                imageLink = SystemImage.VOTING_FAILURE.imageUrl,
                message = buildString {
                    executionEvent.overrideReason?.let { reason ->
                        appendLine(reason)
                    }
                    append("${target.member.effectiveName}님의 처형이 부결되었습니다.")
                }
            )
            game.defenseTargetId = null
            return
        }

        val voteExecutionEvent = GameEvent.VoteExecution(target)
        alivePlayers.forEach { player ->
            player.allAbilities
                 .filterIsInstance<PassiveAbility>()
                .filterNot { FrogCurseManager.shouldSuppressPassive(player) }
                .sortedByDescending(PassiveAbility::priority)
                .forEach { passive ->
                    passive.onEventObserved(game, player, voteExecutionEvent)
                }
        }

        if (voteExecutionEvent.isCancelled) {
            mainChannel.createMessage(voteExecutionEvent.cancelReason ?: "처형 무효")
            game.defenseTargetId = null
            return
        }

        if (hasPoliticianAbility(target)) {
            val politicianJob = target.job ?: return
            (politicianJob as? Thief)?.hasStolenPoliticianAbility = true
            game.publiclyRevealedJobNames += politicianJob.name
            if (!target.state.isJobPubliclyRevealed) {
                target.state.isJobPubliclyRevealed = true
                if (target.allAbilities.any { it is UnwrittenRule }) {
                    game.unwrittenRuleBlockedTargetIdTonight = target.member.id
                }
            }
            
            // 단순 텍스트 대신, 처세 컷신 송출을 위한 직업 공개 퍼블릭 이벤트 방출
            val event = GameEvent.JobDiscovered(
                discoverer = target, // 시스템 브로드캐스트의 주체이므로 본인으로 세팅
                target = target,
                actualJob = politicianJob,
                revealedJob = politicianJob,
                sourceAbilityName = "처세",
                resolvedAt = DiscoveryStep.DAY,
                isPublicReveal = true,
                imageUrl = SystemImage.POLITICIAN_DICTATORSHIP.imageUrl
            )
            game.sendMainChannelMessageWithImage(
                imageLink = SystemImage.POLITICIAN_SURVIVE_VOTE.imageUrl,
                message = "정치인은 투표로 죽지 않습니다."
            )

            coroutineScope {
                launch { JobDiscoveryNotificationManager.notifyDiscoveredTargets(listOf(event), game) }
                launch { game.playGameSound(POLITICIAN_SURVIVAL_SOUND_PATH) }
            }
            
            game.defenseTargetId = null
            return
        }

        val hasMartyrExplosionTarget = ((target.job as? Martyr)?.defenseBombTargetId != null)
        if (
            !hasMartyrExplosionTarget &&
            !FrogCurseManager.shouldSuppressPassive(target) &&
            target.allAbilities.any { it is Escape } &&
            target.member.id !in game.pendingEscapedPlayerIds
        ) {
            game.pendingEscapedPlayerIds += target.member.id
            game.publiclyRevealedAbilityTargetIds += target.member.id
            game.sendMainChannelMessageWithImage(
                imageLink = ESCAPE_IMAGE_URL,
                message = "${target.member.effectiveName}님이 투표에서 도주하였습니다!"
            )
            game.defenseTargetId = null
            return
        }

        processPlayerDeath(game, target, isLynch = true)
        resolveMartyrDefenseExplosion(game, target)
        dispatchEvents(game)
        game.nightEvents.clear()
        game.sendMainChannelMessageWithImage(
            imageLink = SystemImage.VOTE_EXECUTION.imageUrl,
            message = "${target.member.effectiveName}님이 투표로 처형당하였습니다."
        )
        revealBelongingsIfNeeded(game, target)

        if (deadChannel != null) {
            updateDeadChannelPermissions(game, deadChannel)
        }
        refreshMafiaChannelContactState(game)
        game.defenseTargetId = null
    }

    fun checkWinCondition(game: Game): Team? {
        if (isCabalSpecialWinReady(game)) {
            return Team.CABAL_SPECIAL
        }
        resolveProphetSpecialWin(game)?.let { return Team.PROPHET_SPECIAL }
        resolveTerminalSpecialWin(game)?.let { return it }

        val alivePlayers = game.playerDatas.filter { !it.state.isDead }
        val countableAlivePlayers = alivePlayers.filterNot { player ->
            isPendingEscapedMafiaTeamForWinCondition(game, player)
        }
        val mafiaCount = countableAlivePlayers.count { player -> isMafiaTeamForWinCondition(game, player) }
        val citizenCount = countableAlivePlayers.sumOf { player ->
            if (isMafiaTeamForWinCondition(game, player)) {
                0
            } else {
                when (player.job) {
                    is Gangster -> if (FrogCurseManager.shouldSuppressPassive(player)) 1 else 3
                    is Politician -> if (FrogCurseManager.shouldSuppressPassive(player)) 1 else 2
                    else -> 1
                }
            }
        }
        val aliveCabals = countableAlivePlayers.count { it.job is Cabal }

        val activeMercenaryExecution = game.playerDatas.any { player ->
            if (player.state.isDead) return@any false
            val mercenary = player.job as? Mercenary ?: return@any false
            mercenary.hasExecutionAuthority && !FrogCurseManager.shouldSuppressPassive(player)
        }
        val hasAliveHackerProxyOnMafia = alivePlayers.any { player ->
            val hacker = player.job as? Hacker ?: return@any false
            val proxyTarget = hacker.hackedTargetId?.let(game::getPlayer) ?: return@any false
            !proxyTarget.state.isDead && proxyTarget.job is Mafia
        }
        val hasDormantSuccessor = mafiaCount == 0 &&
            game.playerDatas.none { !it.state.isDead && it.job is Mafia } &&
            alivePlayers.any { player ->
                val thief = player.job as? Thief ?: return@any false
                thief.hasSuccessor() && !thief.hasActivatedSuccessorMafia
            }

        return when {
            hasDormantSuccessor -> null
            mafiaCount == 0 -> Team.CITIZEN
            mafiaCount >= citizenCount &&
                aliveCabals < 2 &&
                alivePlayers.none {
                    it.job is Prophet && !FrogCurseManager.shouldSuppressPassive(it)
                } &&
                !isRevealedJudgeAlive(game) &&
                !activeMercenaryExecution &&
                !hasAliveHackerProxyOnMafia &&
                findAliveDictatorshipPolitician(game) == null -> Team.MAFIA
            else -> null
        }
    }

    private fun isMafiaTeamForWinCondition(game: Game, player: PlayerData): Boolean {
        val job = player.job
        if (job is Mafia) return true
        if (job !is Evil || job is Villain) return false
        if (
            job is Thief &&
            job.hasActivatedSuccessorMafia
        ) {
            return true
        }

        return player.state.hasContactedMafiaByInformant || hasContactedMafiaByJobState(game, player)
    }

    private fun isPendingEscapedMafiaTeamForWinCondition(game: Game, player: PlayerData): Boolean {
        return player.member.id in game.pendingEscapedPlayerIds &&
            isMafiaTeamForWinCondition(game, player)
    }

    private fun findAliveDictatorshipPolitician(game: Game): PlayerData? {
        val aliveCitizens = game.playerDatas.filter { !it.state.isDead && it.job !is Evil }
        if (aliveCitizens.size != 1) return null
        return aliveCitizens.firstOrNull { player ->
            player.job is Politician &&
                !FrogCurseManager.shouldSuppressPassive(player)
        }
    }

    private fun calculateTransferredVoteWeights(game: Game): Map<Snowflake, Int> {
        val transferredVoteWeights = mutableMapOf<Snowflake, Int>()
        game.activeThreatenedVoters.forEach { (threatenedId, gangsterId) ->
            val threatened = game.getPlayer(threatenedId) ?: return@forEach
            val gangster = game.getPlayer(gangsterId) ?: return@forEach
            if (threatened.state.isDead || gangster.state.isDead) return@forEach
            transferredVoteWeights[gangsterId] = (transferredVoteWeights[gangsterId] ?: 0) + 1
        }
        return transferredVoteWeights
    }

    private fun calculateProsConsVoteWeight(
        game: Game,
        voterId: Snowflake,
        defenseTarget: PlayerData,
        transferredVoteWeights: Map<Snowflake, Int>
    ): Int {
        val voter = game.getPlayer(voterId) ?: return 0
        if (voter.state.isDead) return 0
        if (voter.member.id in game.permanentlyDisenfranchisedVoters) return 0
        if (game.activeThreatenedVoters.containsKey(voter.member.id)) return 0

        val baseWeight = baseVoteWeight(voter)
        val magician = voter.job as? Magician
        val assistantWeight = if (
            magician != null &&
            magician.trickSubstitutedTargetId == defenseTarget.member.id &&
            voter.allAbilities.any { it is Assistant } &&
            !FrogCurseManager.shouldSuppressPassive(voter)
        ) {
            1
        } else {
            0
        }
        return baseWeight + assistantWeight + (transferredVoteWeights[voter.member.id] ?: 0)
    }

    private fun baseVoteWeight(voter: PlayerData): Int =
        if (voter.job is Politician && !FrogCurseManager.shouldSuppressPassive(voter)) 2 else 1

    private fun hasPoliticianAbility(player: PlayerData): Boolean {
        return !FrogCurseManager.shouldSuppressPassive(player) &&
            player.allAbilities.any { it is PoliticianAbility }
    }

    private fun findProsConsJudge(
        game: Game,
        prosConsVotes: Map<Snowflake, Boolean>,
        aggregateDecision: Boolean
    ): PlayerData? {
        val candidates = game.playerDatas.filter { player ->
            !player.state.isDead &&
                !FrogCurseManager.shouldSuppressPassive(player) &&
                player.member.id !in game.judgeAuthorityDisabledByThiefIds &&
                player.allAbilities.any { it is JudgeAbility }
        }
        return candidates.filter(::hasRevealedJudgeAuthority).randomOrNull()
            ?: candidates.filter { player ->
                val vote = prosConsVotes[player.member.id]
                vote != null && vote != aggregateDecision
            }.randomOrNull()
            ?: candidates.filter { player -> prosConsVotes.containsKey(player.member.id) }.randomOrNull()
    }

    private fun findRevealedAliveJudge(game: Game): PlayerData? {
        return game.playerDatas.filter { player ->
            !player.state.isDead &&
                !FrogCurseManager.shouldSuppressPassive(player) &&
                player.member.id !in game.judgeAuthorityDisabledByThiefIds &&
                player.allAbilities.any { it is JudgeAbility } &&
                hasRevealedJudgeAuthority(player)
        }.randomOrNull()
    }

    private fun hasRevealedJudgeAuthority(player: PlayerData): Boolean {
        return when (val job = player.job) {
            is Judge -> job.hasRevealedAuthority
            is Thief -> job.hasRevealedStolenJudgeAuthority
            else -> false
        }
    }

    private fun revealJudgeAuthority(player: PlayerData) {
        when (val job = player.job) {
            is Judge -> job.hasRevealedAuthority = true
            is Thief -> {
                job.hasRevealedStolenJudgeAuthority = true
                job.hasStolenJudgeAbility = true
            }
        }
    }

    private fun isRevealedJudgeAlive(game: Game): Boolean {
        return findRevealedAliveJudge(game) != null
    }

    private fun resolveTerminalSpecialWin(game: Game): Team? {
        if (game.dayCount < 2) return null
        if (game.initialPlayerCount <= 0) return null

        val requiredDayCount = (game.initialPlayerCount / 2) + 2
        if (game.dayCount < requiredDayCount) return null

        val hasAliveTerminalOwner = game.playerDatas.any { player ->
            !player.state.isDead &&
                player.job is Evil &&
                player.job !is Villain &&
                !FrogCurseManager.shouldSuppressPassive(player) &&
                player.allAbilities.any { it is Terminal }
        }

        return if (hasAliveTerminalOwner) Team.MAFIA else null
    }

    private fun notifyInstructionsAtFirstDay(game: Game) {
        if (game.dayCount != 1) return
        game.playerDatas.forEach { player ->
            if (FrogCurseManager.shouldSuppressPassive(player)) return@forEach
            Instructions.notifyAtFirstDay(game, player)
        }
    }

    private suspend fun notifyTheInformantAutoContactAtSecondDay(game: Game) {
        if (game.dayCount != 2) return

        game.playerDatas.forEach { player ->
            if (player.state.isDead) return@forEach
            if (player.state.hasContactedMafiaByInformant) return@forEach
            if (player.job !is Evil || player.job is Mafia) return@forEach
            if (FrogCurseManager.shouldSuppressPassive(player)) return@forEach
            if (player.allAbilities.none { it is TheInformant }) return@forEach

            player.state.hasContactedMafiaByInformant = true
            notifyInformantContactByJob(game, player)
        }
    }

    private suspend fun notifyInformantContactByJob(game: Game, player: PlayerData) {
        val mafiaChannel = game.mafiaChannel ?: return
        refreshMafiaChannelContactState(game)

        when (val job = player.job) {
            is HitMan -> {
                if (!job.hasContactedMafia) {
                    job.hasContactedMafia = true
                    notifyHitmanContact(game, player)
                }
            }
            is Spy -> {
                if (!job.hasContactedMafia) {
                    job.hasContactedMafia = true
                    announceMafiaSupportContact(game, player, SPY_CONTACT_IMAGE_URL)
                }
            }
            is Thief -> {
                if (!job.hasContactedMafia) {
                    job.hasContactedMafia = true
                    if (!player.state.hasAnnouncedThiefContact) {
                        player.state.hasAnnouncedThiefContact = true
                        announceMafiaSupportContact(game, player, THIEF_CONTACT_IMAGE_URL)
                    }
                }
            }
            is Witch -> {
                if (!job.hasContactedMafia) {
                    job.hasContactedMafia = true
                    announceMafiaSupportContact(game, player, WITCH_CONTACT_IMAGE_URL)
                }
            }
            is Hostess -> {
                if (!job.hasContactedMafia) {
                    job.hasContactedMafia = true
                    announceMafiaSupportContact(game, player, HOSTESS_CONTACT_IMAGE_URL)
                }
            }
            is Swindler -> {
                if (!job.hasContactedMafia) {
                    job.hasContactedMafia = true
                    announceMafiaSupportContact(game, player, SWINDLER_CONTACT_IMAGE_URL)
                }
            }
            is Godfather -> {
                if (!player.state.hasAnnouncedGodfatherContact) {
                    player.state.hasAnnouncedGodfatherContact = true
                    announceMafiaSupportContact(game, player, GODFATHER_CONTACT_IMAGE_URL)
                }
            }
            is MadScientist -> {
                if (!player.state.hasContactedMafiaOnDeath) {
                    player.state.hasContactedMafiaOnDeath = true
                }
                if (!player.state.hasAnnouncedMadScientistContact) {
                    player.state.hasAnnouncedMadScientistContact = true
                    announceMafiaSupportContact(game, player, MAD_SCIENTIST_CONTACT_IMAGE_URL)
                }
            }
            is Beastman -> {
                announceMafiaSupportContact(game, player, BEASTMAN_TAMED_IMAGE_URL)
            }
            else -> {
                mafiaChannel.createMessage("**${mention(player)}님이 밀정 능력으로 접선했습니다.**")
            }
        }
    }

    private fun notifyJudgeProsVoters(
        game: Game,
        target: PlayerData,
        prosConsVotes: Map<Snowflake, Boolean>
    ) {
        val judgePlayer = game.playerDatas.filter { player ->
            !player.state.isDead &&
                !FrogCurseManager.shouldSuppressPassive(player) &&
                player.allAbilities.any { it is GovernmentAuthority }
        }.randomOrNull() ?: return
        val prosVoters = prosConsVotes
            .filterValues { it }
            .keys
            .mapNotNull { voterId -> game.getPlayer(voterId) }
            .map { voter -> voter.member.effectiveName }

        val prosMessage = if (prosVoters.isEmpty()) {
            "없음"
        } else {
            prosVoters.joinToString(", ")
        }
        val message = "${target.member.effectiveName} 처형 찬성 투표자 - $prosMessage"

        cabalNotificationScope.launch {
            runCatching {
                GameReplayLogger.logDirectMessage(game, judgePlayer, message, "판사 투표 정보")
                judgePlayer.member.getDmChannel().createMessage(message)
            }
        }
    }

    private suspend fun resolveMartyrNightExplosions(game: Game, playersToDie: MutableSet<PlayerData>) {

        game.playerDatas.forEach { player ->
            val martyr = player.job as? Martyr
            val thief = player.job as? Thief
            if (martyr == null && thief == null) return@forEach
            if (player !in playersToDie) return@forEach
            if (FrogCurseManager.shouldSuppressPassive(player)) return@forEach

            val selectedTargetId = martyr?.nightBombTargetId ?: thief?.stolenMartyrNightBombTargetId ?: return@forEach
            val selectedTarget = game.getPlayer(selectedTargetId) ?: return@forEach
            if (selectedTarget.state.isDead) return@forEach

            val mafiaExecutionTarget = game.nightAttacks["MAFIA_TEAM"]?.target
            val selectedTargetUsesMafiaGun = selectedTarget.allAbilities.any { it is MafiaAbility }
            val isNightBombTriggered =
                mafiaExecutionTarget == player &&
                    (selectedTarget.job is Mafia || selectedTargetUsesMafiaGun)

            val hasExplosion = player.allAbilities.any { it is Explosion }
            val attackBySelectedNonMafiaEvil = game.nightAttacks.values.any { attack ->
                attack.target == player &&
                    attack.attacker.member.id == selectedTarget.member.id &&
                    attack.attacker.job is Evil &&
                    attack.attacker.job !is Mafia
            }
            val isExplosionTriggered = hasExplosion && attackBySelectedNonMafiaEvil

            if (!isNightBombTriggered && !isExplosionTriggered) return@forEach

            playersToDie += selectedTarget
            if (selectedTarget.job is Thief && selectedTargetUsesMafiaGun) {
                selectedTarget.state.forcedDisplayedJobName = "마피아"
            }

            val hasFlash = player.allAbilities.any { it is Flash }
            if (hasFlash) {
                playersToDie -= player
            }

            player.state.isJobPubliclyRevealed = true
            selectedTarget.state.isJobPubliclyRevealed = true
            player.job?.name?.let(game.publiclyRevealedJobNames::add)
            FrogCurseManager.displayedJob(selectedTarget)?.name?.let(game.publiclyRevealedJobNames::add)

            val (explosionImageUrl, explosionMessage) = if (isNightBombTriggered) {
                TERRORIST_NIGHT_MAFIA_BOMB_IMAGE_URL to
                    "테러리스트 ${player.member.effectiveName}님이 마피아 ${selectedTarget.member.effectiveName}님과 함께 자폭했습니다!"
            } else {
                TERRORIST_NIGHT_EXPLOSION_IMAGE_URL to
                    "테러리스트 ${player.member.effectiveName}님이 ${selectedTarget.member.effectiveName}님과 함께 자폭했습니다!"
            }

            game.sendMainChannelMessageWithImageAndSound(
                explosionImageUrl,
                explosionMessage,
                TERRORIST_EXPLOSION_SOUND_PATH
            )
        }
    }

    private suspend fun resolveMartyrDefenseExplosion(game: Game, executedTarget: PlayerData) {
        if (FrogCurseManager.shouldSuppressPassive(executedTarget)) return
        val martyr = executedTarget.job as? Martyr
        val thief = executedTarget.job as? Thief
        val selectedTargetId = martyr?.defenseBombTargetId ?: thief?.stolenMartyrDefenseBombTargetId ?: return
        val selectedTarget = game.getPlayer(selectedTargetId) ?: return
        if (selectedTarget.state.isDead) return
        if (selectedTarget.member.id == executedTarget.member.id) return

        processPlayerDeath(game, selectedTarget, isLynch = true)

        executedTarget.state.isJobPubliclyRevealed = true
        selectedTarget.state.isJobPubliclyRevealed = true
        executedTarget.job?.name?.let(game.publiclyRevealedJobNames::add)
        selectedTarget.job?.name?.let(game.publiclyRevealedJobNames::add)

        game.sendMainChannelMessageWithImageAndSound(
            TERRORIST_VOTE_EXPLOSION_IMAGE_URL,
            "테러리스트 ${executedTarget.member.effectiveName}님이 ${selectedTarget.member.effectiveName}님과 함께 자폭했습니다!",
            TERRORIST_EXPLOSION_SOUND_PATH
        )
        refreshMafiaChannelContactState(game)
    }

    suspend fun endGame(game: Game, winningTeam: Team) {
        game.isRunning = false
        game.currentPhase = GamePhase.END
        val resultMessage = "${winningTeam.displayName} 승리: ${winningTeam.winMessage}"
        GameReplayLogger.log(
            game = game,
            type = ReplayLogType.GAME_END,
            visibility = ReplayVisibility.PUBLIC,
            title = "승리",
            body = resultMessage
        )
        val playerJobRevealMessage = buildString {
            appendLine("## 플레이어 직업 공개")
            game.playerDatas.forEachIndexed { index, playerData ->
                val deathStatus = if (playerData.state.isDead) "사망" else "생존"
                val jobName = game.probationOriginalJobsByPlayer[playerData.member.id]?.name
                    ?: playerData.job?.name
                    ?: "알 수 없음"
                appendLine("${index + 1}. ${playerData.member.effectiveName} - $jobName ($deathStatus)")
            }
        }.trim()

        if (winningTeam.winImageUrl != null) {
            game.sendMainChannelMessageWithImage(
                imageLink = winningTeam.winImageUrl,
                message = resultMessage
            )
        } else {
            // 이미지가 없다면 기존처럼 텍스트만 전송
            game.sendMainChannerMessage(resultMessage)
        }

        game.sendMainChannerMessage(playerJobRevealMessage)

        GameManager.finalizeGameState(
            gameToStop = game,
            endReason = "WIN_CONDITION_MET",
            winningTeamName = winningTeam.displayName,
            cancelLoopJob = false
        )
    }

    suspend fun runGameLoop(game: Game) {
        while (game.isRunning) {
            startNightPhase(game)
            runPhaseCountdown(
                game = game,
                label = "밤",
                durationMillis = NIGHT_DURATION_MS,
                midpointAction = {
                    announcePendingMadScientistRevivals(game)
                    publishReporterArticles(game, publishAtNightMidpoint = true)
                },
                beforeEndAction = {
                    AgentOperation.resolveNightEndOperations(game)
                },
                beforeEndOffsetMillis = 1_000L
            )

            val nightSummary = resolveNightPhase(game)

            resolveDawnPhase(game, nightSummary)
            runPhaseCountdown(game, "새벽", DAWN_DURATION_MS)
            checkWinCondition(game)?.let { winner ->
                if (winner == Team.CABAL_SPECIAL) {
                    game.playGameSound(CABAL_SPECIAL_WIN_SOUND_PATH)
                }
                endGame(game, winner)
                break
            }

            startDayPhase(game)
            val discussionMillis = game.playerDatas.count { !it.state.isDead } * 15_000L
            runPhaseCountdown(game, "낮", discussionMillis)

            startVotePhase(game)
            runPhaseCountdown(game, "투표", VOTE_DURATION_MS, stopLoopingSoundOnEnd = false)

            val target = resolveVotePhase(game)
            if (target != null) {
                startDefensePhase(game, target)
                runPhaseCountdown(game, "변론", DEFENSE_DURATION_MS)

                startProsConsVotePhase(game, target)
                runPhaseCountdown(game, "찬반 투표", PROS_CONS_VOTE_DURATION_MS)

                resolveExecutionPhase(game, target)
            } else {
                game.stopLoopingGameSound()
            }

            checkWinCondition(game)?.let { winner ->
                if (winner == Team.CABAL_SPECIAL) {
                    game.playGameSound(CABAL_SPECIAL_WIN_SOUND_PATH)
                }
                endGame(game, winner)
                break
            }
        }
    }

    private fun resolveCabalSunInvestigation(game: Game) {
        val cabalPlayers = game.playerDatas.filter { it.job is Cabal }
        cabalPlayers.forEach { sunPlayer ->
            val sunCabal = sunPlayer.job as? Cabal ?: return@forEach
            if (sunCabal.role != CabalRole.SUN || sunPlayer.state.isDead) return@forEach
            if (FrogCurseManager.shouldSuppressPassive(sunPlayer)) return@forEach

            val selectedTargetId = sunCabal.selectedTargetId ?: return@forEach
            val selectedTarget = game.getPlayer(selectedTargetId) ?: return@forEach

            val isMoon = selectedTarget.job is Cabal &&
                (selectedTarget.job as? Cabal)?.role == CabalRole.MOON &&
                selectedTarget.member.id == sunCabal.pairedPlayerId

            if (isMoon) {
                val newlyFoundMoon = !sunCabal.hasFoundMoon
                sunCabal.hasFoundMoon = true
                val moonCabal = selectedTarget.job as? Cabal
                moonCabal?.wasFoundBySun = true
                if (newlyFoundMoon) {
                    notifyCabalMarkerFound(game, sunPlayer, selectedTarget)
                }
            } else if ((selectedTarget.job as? Thief)?.stolenJob is Cabal) {
                sendCabalDm(game, selectedTarget, "비밀결사의 표식이 감지되었습니다.")
            }
        }
    }

    private fun resolveCabalMoonInvestigation(game: Game) {
        val cabalPlayers = game.playerDatas.filter { it.job is Cabal }
        cabalPlayers.forEach { moonPlayer ->
            val moonCabal = moonPlayer.job as? Cabal ?: return@forEach
            if (moonCabal.role != CabalRole.MOON) return@forEach

            val selectedTargetId = moonCabal.selectedTargetId ?: return@forEach
            val selectedTarget = game.getPlayer(selectedTargetId) ?: return@forEach
            if ((selectedTarget.job as? Thief)?.stolenJob is Cabal) {
                sendCabalDm(game, selectedTarget, "비밀결사의 표식이 감지되었습니다.")
                return@forEach
            }
            if (moonCabal.hasFoundSun || !moonCabal.moonMarkedSunTonight) return@forEach
            val isSun = selectedTarget.job is Cabal &&
                (selectedTarget.job as? Cabal)?.role == CabalRole.SUN &&
                selectedTarget.member.id == moonCabal.pairedPlayerId
            if (!isSun) return@forEach

            moonCabal.hasFoundSun = true
            notifyCabalMarkerFound(game, moonPlayer, selectedTarget)
        }
    }

    private fun notifyCabalMarkerFound(game: Game, first: PlayerData, second: PlayerData) {
        sendCabalDm(game, first, "비밀결사의 표식을 발견했습니다.")
        sendCabalDm(game, second, "비밀결사의 표식을 발견했습니다.")
    }

    private fun resolveCabalSpecialWinReadiness(game: Game) {
        val aliveOrDeadCabals = game.playerDatas
            .mapNotNull { player ->
                val cabal = player.job as? Cabal ?: return@mapNotNull null
                player to cabal
            }
        val sun = aliveOrDeadCabals.filter { (_, cabal) -> cabal.role == CabalRole.SUN }.randomOrNull() ?: return
        val moon = aliveOrDeadCabals.filter { (_, cabal) -> cabal.role == CabalRole.MOON }.randomOrNull() ?: return

        val sunPlayer = sun.first
        val sunCabal = sun.second
        val moonPlayer = moon.first
        val moonCabal = moon.second

        val rolesStillCabal = sunPlayer.job is Cabal && moonPlayer.job is Cabal
        val canTrigger =
            rolesStillCabal &&
                !FrogCurseManager.isCursed(moonPlayer) &&
                sunCabal.hasFoundMoon &&
                moonCabal.wasFoundBySun &&
                moonCabal.hasFoundSun

        sunCabal.cabalSpecialWinReady = canTrigger
        moonCabal.cabalSpecialWinReady = canTrigger
    }

    private fun isCabalSpecialWinReady(game: Game): Boolean {
        return game.playerDatas.any { player ->
            val cabal = player.job as? Cabal ?: return@any false
            cabal.cabalSpecialWinReady &&
                (cabal.role != CabalRole.MOON || !FrogCurseManager.isCursed(player))
        }
    }

    private fun resolveProphetPioneerSpecialWinReadiness(game: Game, summary: NightResolutionSummary) {
        val shouldTrigger = summary.deaths.any { player ->
            if (player.state.isDead.not()) return@any false
            if (player.job !is Prophet) return@any false
            if (FrogCurseManager.shouldSuppressPassive(player)) return@any false
            if (player.allAbilities.none { it is Pioneer }) return@any false
            if (player.member.id in game.probationOriginalJobsByPlayer) return@any false

            val day4RevelationReady = game.dayCount >= 4
            val apostleRevelationReady = player.allAbilities.any { it is Apostle } &&
                game.playerDatas.none { candidate ->
                    ProphetApostlePolicy.isCitizenTeamSurvivor(candidate)
                }

            day4RevelationReady || apostleRevelationReady
        }

        if (!shouldTrigger) return
        game.prophetSpecialWinScheduledTeam = Team.CITIZEN
    }

    private fun resolveProphetSpecialWin(game: Game): Team? {
        game.prophetSpecialWinScheduledTeam?.let { return it }

        val aliveProphets = game.playerDatas.filter {
            !it.state.isDead &&
                it.job is Prophet &&
                !FrogCurseManager.shouldSuppressPassive(it)
        }
        if (aliveProphets.isEmpty()) return null

        if (game.dayCount >= 4) {
            return Team.CITIZEN
        }

        val aliveCitizens = game.playerDatas.filter(ProphetApostlePolicy::isCitizenTeamSurvivor)
        val isApostleTriggered = aliveProphets.any { prophet ->
            prophet.allAbilities.any { it is Apostle } &&
                aliveCitizens.size == 1 &&
                aliveCitizens.first().member.id == prophet.member.id
        }

        return if (isApostleTriggered) Team.CITIZEN else null
    }

    private fun sendCabalDm(game: Game, target: PlayerData, message: String, title: String = "비공개 알림") {
        cabalNotificationScope.launch {
            runCatching {
                GameReplayLogger.logDirectMessage(game, target, message, title)
                target.member.getDmChannel().createMessage(message)
            }
        }
    }

    private fun buildDawnPresentation(
        game: Game,
        deaths: List<PlayerData>,
        poisonedDeaths: List<PlayerData> = emptyList()
    ): DawnPresentation {
        val attacks = game.nightAttacks.values.toList()
        val presentationEvent = GameEvent.ResolveDawnPresentation(
            dayCount = game.dayCount,
            attacks = attacks,
            deaths = deaths,
            presentation = buildDefaultDawnPresentation(
                attacksByKey = game.nightAttacks,
                deaths = deaths,
                poisonedDeaths = poisonedDeaths,
                game = game
            )
        )

        game.playerDatas
            .filter { !it.state.isDead }
            .forEach { player ->
                player.allAbilities
                     .filterIsInstance<PassiveAbility>()
                    .filterNot { FrogCurseManager.shouldSuppressPassive(player) }
                    .sortedByDescending(PassiveAbility::priority)
                    .forEach { passive ->
                        passive.onEventObserved(game, player, presentationEvent)
                    }
            }

        return presentationEvent.presentation
    }

    private fun buildDefaultDawnPresentation(
        attacksByKey: Map<String, AttackEvent>,
        deaths: List<PlayerData>,
        poisonedDeaths: List<PlayerData>,
        game: Game
    ): DawnPresentation {

        // 연인이 희생해서 죽은 사람이 있는지 확인
        val hasCoupleSacrifice = deaths.any { it.member.id in game.coupleSacrificeMap }

        if (hasCoupleSacrifice) {
            // 이미 announceCoupleSacrificeReveal에서 화려하게 이미지를 띄웠으므로
            // 여기서는 조용히 넘어가거나, 아주 간략한 요약만 반환하게 합니다.
            return DawnPresentation(imageUrl = "", message = "")
        }

        val doctorSavedTarget = if (game.concealmentForcedQuietNight) null else game.doctorSavedTargetTonight
        val attacks = attacksByKey.values.toList()
        val deathsSet = deaths.toSet()
        val announcements = mutableListOf<DawnAnnouncement>()

        fun addAnnouncement(
            priority: Int,
            imageUrl: String,
            message: String,
            target: PlayerData? = null,
            soundPath: String? = null
        ) {
            announcements += DawnAnnouncement(
                imageUrl = imageUrl,
                message = message,
                priority = priority,
                targetId = target?.member?.id,
                soundPath = soundPath
            )
        }

        attacksByKey["MAFIA_TEAM"]
            ?.target
            ?.takeIf { it in deathsSet }
            ?.let { victim ->
                addAnnouncement(
                    priority = DAWN_PRIORITY_MAFIA_EXECUTION,
                    imageUrl = SystemImage.DEATH_BY_MAFIA.imageUrl,
                    message = "${victim.member.effectiveName}이(가) 살해당했습니다.",
                    target = victim,
                    soundPath = MAFIA_EXECUTION_SOUND_PATH
                )
            }

        val disciplineTargets = attacksByKey
            .filterKeys { it.startsWith("DISCIPLINE_") }
            .values
            .map { it.target }
            .filter { it in deathsSet }
            .distinctBy { it.member.id }
        disciplineTargets.forEach { victim ->
            addAnnouncement(
                priority = DAWN_PRIORITY_MAFIA_EXECUTION,
                imageUrl = SystemImage.DEATH_BY_MAFIA.imageUrl,
                message = "${victim.member.effectiveName}이(가) 살해당했습니다.",
                target = victim,
                soundPath = MAFIA_EXECUTION_SOUND_PATH
            )
        }

        if (doctorSavedTarget != null) {
            game.publiclyRevealedAbilityTargetIds += doctorSavedTarget.member.id
            game.publiclyRevealedJobNames += Doctor().name
            addAnnouncement(
                priority = DAWN_PRIORITY_MAFIA_REACTION,
                imageUrl = SystemImage.DOCTOR_HEAL.imageUrl,
                message = "${doctorSavedTarget.member.effectiveName}님이 의사의 치료를 받고 살아났습니다!",
                target = doctorSavedTarget,
                soundPath = DOCTOR_HEAL_SOUND_PATH
            )
        }

        attacks
            .filter {
                it.attacker.job is Godfather &&
                    it.target in deathsSet &&
                    it.target !in disciplineTargets
            }
            .map { it.target }
            .distinctBy { it.member.id }
            .forEach { victim ->
                addAnnouncement(
                    priority = DAWN_PRIORITY_MAFIA_SUPPORT,
                    imageUrl = GODFATHER_EXECUTION_IMAGE_URL,
                    message = "${victim.member.effectiveName}가 살해당하였습니다.",
                    target = victim
                )
            }

        attacks
            .filter { it.attacker.job is Beastman && it.target in deathsSet }
            .map { it.target }
            .distinctBy { it.member.id }
            .forEach { victim ->
                addAnnouncement(
                    priority = DAWN_PRIORITY_MAFIA_SUPPORT,
                    imageUrl = BEASTMAN_ATTACK_IMAGE_URL,
                    message = "${victim.member.effectiveName}님이 짐승에게 습격당하였습니다.",
                    target = victim
                )
            }

        poisonedDeaths
            .distinctBy { it.member.id }
            .forEach { victim ->
                addAnnouncement(
                    priority = DAWN_PRIORITY_MAFIA_SUPPORT,
                    imageUrl = SystemImage.DEATH_BY_POISON.imageUrl,
                    message = "${victim.member.effectiveName}님이 중독으로 사망했습니다.",
                    target = victim
                )
            }

        attacksByKey
            .filter { (attackKey, attack) ->
                attackKey.startsWith("VIGILANTE_") && attack.target in deathsSet
            }
            .map { (_, attack) -> attack.target }
            .distinctBy { it.member.id }
            .forEach { victim ->
                victim.state.isJobPubliclyRevealed = true
                val revealedJob = victim.job
                revealedJob?.name?.let(game.publiclyRevealedJobNames::add)
                addAnnouncement(
                    priority = DAWN_PRIORITY_OTHER_EXECUTION,
                    imageUrl = VIGILANTE_EXECUTION_IMAGE_URL,
                    message = "${victim.member.effectiveName}가 살해당하였습니다." +
                        if (revealedJob != null) "\n${victim.member.effectiveName}님의 직업은 ${revealedJob.name}입니다." else "",
                    target = victim
                )
            }

        attacks
            .filter { it.attacker.job is Mercenary && it.target in deathsSet }
            .map { it.target }
            .distinctBy { it.member.id }
            .forEach { victim ->
                addAnnouncement(
                    priority = DAWN_PRIORITY_OTHER_EXECUTION + 10,
                    imageUrl = SystemImage.MERCENARY_EXECUTION.imageUrl,
                    message = "${victim.member.effectiveName}가 살해당하였습니다.",
                    target = victim
                )
            }

        if (announcements.isEmpty()) {
            val quietAnnouncement = DawnAnnouncement(
                imageUrl = SystemImage.QUIET_NIGHT.imageUrl,
                message = "조용하게 밤이 넘어갔습니다.",
                priority = DAWN_PRIORITY_QUIET_NIGHT
            )
            return DawnPresentation(
                imageUrl = quietAnnouncement.imageUrl,
                message = quietAnnouncement.message,
                announcements = listOf(quietAnnouncement)
            )
        }

        val sortedAnnouncements = announcements.sortedBy(DawnAnnouncement::priority)
        val imageUrls = sortedAnnouncements.map(DawnAnnouncement::imageUrl)
            .filter(String::isNotBlank)
            .distinct()

        return DawnPresentation(
            imageUrl = imageUrls.firstOrNull().orEmpty(),
            message = sortedAnnouncements.joinToString("\n", transform = DawnAnnouncement::message),
            extraImageUrls = imageUrls.drop(1),
            announcements = sortedAnnouncements
        )
    }

    private fun dispatchEvents(game: Game): List<GameEvent> {
        val processedEvents = mutableListOf<GameEvent>()

        while (game.nightEvents.isNotEmpty()) {
            val eventsToProcess = game.nightEvents.toList()
            game.nightEvents.clear()
            processedEvents += eventsToProcess

            val observers = game.playerDatas
                .filter { !it.state.isDead }
                .mapNotNull { player ->
                    val passives = player.allAbilities
                         .filterIsInstance<PassiveAbility>()
                        .filterNot { FrogCurseManager.shouldSuppressPassive(player) }
                        .sortedByDescending(PassiveAbility::priority)
                    if (passives.isEmpty()) null else player to passives
                }

            eventsToProcess.forEach { event ->
                applyNurseDoctorInheritanceOnDeath(game, event)
                when (event) {
                    is GameEvent.PlayerDied -> {
                        GameReplayLogger.log(
                            game = game,
                            type = ReplayLogType.DEATH,
                            visibility = ReplayVisibility.PUBLIC,
                            title = if (event.isLynch) "처형" else "사망",
                            body = "${event.victim.member.effectiveName} 사망",
                            actor = event.victim
                        )
                    }
                    is GameEvent.JobDiscovered -> {
                        if (event.sourceAbilityName != "도굴") {
                            FrogCurseManager.displayedJob(event.target)?.let { event.revealedJob = it }
                        }
                        GameReplayLogger.logSystem(
                            game = game,
                            title = "직업 정보",
                            body = "${event.discoverer.member.effectiveName} -> ${event.target.member.effectiveName}: ${event.revealedJob.name}",
                            visibility = if (event.isPublicReveal) ReplayVisibility.PUBLIC else ReplayVisibility.DIRECT_MESSAGE,
                            actor = event.discoverer,
                            recipients = listOf(GameReplayLogger.recipient(event.discoverer, ReplayVisibility.DIRECT_MESSAGE))
                        )
                    }
                    is GameEvent.PoliceJobRevealed -> {
                        FrogCurseManager.displayedJob(event.target)?.let { event.revealedJob = it }
                    }
                    else -> Unit
                }
                observers.forEach { (player, passives) ->
                    passives.forEach { passive ->
                        passive.onEventObserved(game, player, event)
                    }
                }
            }
        }

        return processedEvents
    }

    private fun applyNurseDoctorInheritanceOnDeath(game: Game, event: GameEvent) {
        val deathEvent = event as? GameEvent.PlayerDied ?: return
        if (deathEvent.victim.job !is Doctor) return

        game.playerDatas.forEach { nursePlayer ->
            val nurseJob = nursePlayer.job as? Nurse ?: return@forEach
            if (!nurseJob.hasContactedDoctor) return@forEach

            nurseJob.canUseInheritedHeal = true
            if (nursePlayer.job?.abilities?.none { it is DoctorAbility } == true) {
                nursePlayer.job?.abilities?.add(DoctorAbility())
            }
        }
    }

    private fun applyPoliceAutopsy(game: Game, victim: PlayerData) {
        game.playerDatas.forEach { policePlayer ->
            if (policePlayer.state.isDead) return@forEach
            if (FrogCurseManager.shouldSuppressPassive(policePlayer)) return@forEach
            if (policePlayer.member.id == victim.member.id) return@forEach
            if (policePlayer.allAbilities.none { it is Autopsy }) return@forEach

            val policeJob = policePlayer.job as? Police ?: return@forEach
            policeJob.eavesdroppingTargetId = victim.member.id
            policeJob.searchedTargets += victim.member.id
            val message =
                "${victim.member.effectiveName}님은 ${if (PoliceAutopsyPolicy.isMafia(victim.job)) "마피아입니다." else "마피아가 아닙니다."}"

            votePresentationScope.launch {
                runCatching {
                    GameReplayLogger.logDirectMessage(game, policePlayer, message, "검시 결과")
                    policePlayer.member.getDmChannel().createMessage(message)
                }
            }
        }
    }

    private fun applyPoliceConfidentialInvestigation(game: Game) {
        if (game.dayCount != 1) return

        game.playerDatas.forEach { policePlayer ->
            if (policePlayer.state.isDead) return@forEach
            if (FrogCurseManager.shouldSuppressPassive(policePlayer)) return@forEach
            if (policePlayer.allAbilities.none { it is Confidential }) return@forEach

            val policeJob = policePlayer.job as? Police ?: return@forEach
            if (policeJob.hasUsedConfidential) return@forEach

            val candidates = game.playerDatas.filter {
                !it.state.isDead && it.member.id != policePlayer.member.id
            }
            val selectedTarget = candidates.randomOrNull() ?: return@forEach

            policeJob.hasUsedConfidential = true
            val searchEvent = GameEvent.PoliceSearchResolved(
                police = policePlayer,
                target = selectedTarget,
                isMafia = PoliceSearchPolicy.isMafia(selectedTarget),
                isRepeatedSearch = selectedTarget.member.id in policeJob.searchedTargets
            )
            dispatchPoliceSearchEvent(game, searchEvent)
            policeJob.eavesdroppingTargetId = selectedTarget.member.id
            policeJob.searchedTargets += selectedTarget.member.id

            votePresentationScope.launch {
                runCatching {
                    PoliceNotificationManager.notifySearchResult(policePlayer, searchEvent)
                }
            }
        }
    }

    private fun dispatchPoliceSearchEvent(game: Game, event: GameEvent.PoliceSearchResolved) {
        game.playerDatas
            .filter { !it.state.isDead }
            .forEach { player ->
                player.allAbilities
                    .filterIsInstance<PassiveAbility>()
                    .filterNot { FrogCurseManager.shouldSuppressPassive(player) }
                    .sortedByDescending(PassiveAbility::priority)
                    .forEach { passive ->
                        passive.onEventObserved(game, player, event)
                    }
            }
    }

    private suspend fun announceSourceMafiaCountAtNightStart(game: Game) {
        if (game.dayCount <= 1) return

        val aliveSources = game.playerDatas.filter { player ->
            !player.state.isDead &&
                !FrogCurseManager.shouldSuppressPassive(player) &&
                player.allAbilities.any { it is Source }
        }
        if (aliveSources.isEmpty()) return

        val aliveMafiaTeamCount = game.playerDatas.count { player ->
            !player.state.isDead && player.job is Evil
        }

        aliveSources.forEach { sourcePlayer ->
            GameReplayLogger.logDirectMessage(
                game = game,
                recipient = sourcePlayer,
                body = "정보원에 의해 현재 ${aliveMafiaTeamCount}명의 마피아팀이 살아있을 것이 밝혀졌습니다.",
                title = "정보원 결과"
            )
            runCatching {
                sourcePlayer.member.getDmChannel().createMessage(
                    "정보원에 의해 현재 ${aliveMafiaTeamCount}명의 마피아팀이 살아남은 것이 밝혀졌습니다."
                )
            }
        }
    }

    private suspend fun revealBelongingsIfNeeded(
        game: Game,
        victim: PlayerData,
        deferredAnnouncements: MutableList<DawnAnnouncement>? = null
    ) {
        if (victim.state.isJobPubliclyRevealed) return
        if (FrogCurseManager.shouldSuppressPassive(victim)) return
        if (victim.allAbilities.none { it is Belongings }) return
        if (game.probationOriginalJobsByPlayer.containsKey(victim.member.id) && victim.job is Citizen) return

        val revealedJob = victim.job ?: return
        victim.state.isJobPubliclyRevealed = true
        game.publiclyRevealedJobNames += revealedJob.name
        val announcement = DawnAnnouncement(
            imageUrl = BELONGINGS_REVEAL_IMAGE_URL,
            message = "${victim.member.effectiveName}님의 유품을 통해 직업이 ${revealedJob.name}(이)라고 밝혀졌습니다!",
            priority = DAWN_PRIORITY_BELONGINGS,
            targetId = victim.member.id
        )
        if (deferredAnnouncements != null) {
            deferredAnnouncements += announcement
        } else {
            game.sendMainChannelMessageWithImage(
                imageLink = announcement.imageUrl,
                message = announcement.message
            )
        }
    }

    private fun attackEvaluationPriority(attackKey: String): Int {
        return when {
            attackKey == "MAFIA_TEAM" -> 0
            attackKey.startsWith("DISCIPLINE_") -> 5
            attackKey.startsWith("VIGILANTE_") -> 10
            attackKey.startsWith("MERCENARY_") -> 20
            else -> 30
        }
    }

    private fun shouldMafiaExecutionCancelAttack(
        game: Game,
        attack: AttackEvent,
        successfulAttacks: Set<AttackEvent>
    ): Boolean {
        val mafiaAttack = game.nightAttacks["MAFIA_TEAM"] ?: return false
        if (mafiaAttack.target != attack.attacker) return false
        if (mafiaAttack !in successfulAttacks) return false
        return attack.attacker.allAbilities.none { it is Resolute }
    }

    private fun cancelNightAttack(game: Game, attackKey: String, attack: AttackEvent) {
        game.nightAttacks.remove(attackKey)
        if (game.nightAttacks.values.none { it.target == attack.target }) {
            game.coupleSacrificeMap.remove(attack.target.member.id)
        }
        removeNightDeathCandidateIfNoSourceRemains(game, attack.target)
    }

    private suspend fun notifyMercenaryBeastmanEncounters(
        game: Game,
        encounters: List<AttackEvent>
    ) {
        encounters
            .distinctBy { it.attacker.member.id to it.target.member.id }
            .forEach { encounter ->
                val mercenary = encounter.attacker
                val beastman = encounter.target
                val mercenaryMessage = "${beastman.member.effectiveName}님은 짐승인간입니다."
                val beastmanMessage = "${mercenary.member.effectiveName}님은 용병입니다."

                runCatching {
                    GameReplayLogger.logDirectMessage(
                        game,
                        mercenary,
                        mercenaryMessage,
                        "용병 처형 결과"
                    )
                    mercenary.member.getDmChannel().createMessage(mercenaryMessage)
                }
                runCatching {
                    GameReplayLogger.logDirectMessage(
                        game,
                        beastman,
                        beastmanMessage,
                        "민첩 발동"
                    )
                    beastman.member.getDmChannel().createMessage(beastmanMessage)
                }
            }
    }
    private fun removeNightDeathCandidateIfNoSourceRemains(game: Game, target: PlayerData) {
        val hasAttackSource = game.nightAttacks.values.any { attack -> attack.target == target }
        val hasFixedNightDeathSource = target.member.id in game.pendingNightDeathPlayerIds
        if (!hasAttackSource && !hasFixedNightDeathSource) {
            game.nightDeathCandidates.remove(target)
        }
    }
    private fun resolveMercenaryContractDeaths(
        game: Game,
        blockedAttacks: List<AttackEvent>,
        playersToDie: MutableSet<PlayerData>
    ) {
        val unblockedAttacks = game.nightAttacks.values.filterNot { it in blockedAttacks }

        game.playerDatas.forEach { mercenaryPlayer ->
            val mercenary = mercenaryPlayer.job as? Mercenary ?: return@forEach
            val clientId = mercenary.clientPlayerId ?: return@forEach
            val client = game.getPlayer(clientId) ?: return@forEach

            if (!mercenary.hasReceivedContract || mercenary.hasExecutionAuthority) return@forEach
            if (client !in playersToDie) return@forEach

            val killingAttack = unblockedAttacks.firstOrNull { it.target == client }
            val pendingNightDeathKillerId = game.pendingNightDeathSourceByPlayerId[client.member.id]
            mercenary.hasExecutionAuthority = true
            mercenary.clientKilledByPlayerId = killingAttack?.attacker?.member?.id ?: pendingNightDeathKillerId
            sendCabalDm(
                game,
                mercenaryPlayer,
                "${SystemImage.MERCENARY_CLIENT_DEATH.imageUrl}\n의뢰인 (${client.member.effectiveName})님이 사망했습니다."
            )
        }
    }

    private fun notifyMercenaryContractReception(game: Game) {
        assignGraveRobbedMercenaryClientsAtFirstDay(game)

        game.playerDatas.forEach { mercenaryPlayer ->
            val mercenary = mercenaryPlayer.job as? Mercenary ?: return@forEach
            if (mercenary.hasReceivedContract) return@forEach
            if (mercenaryPlayer.state.isDead) return@forEach

            val clientId = mercenary.clientPlayerId ?: return@forEach
            val client = game.getPlayer(clientId) ?: return@forEach
            if (client.state.isDead) return@forEach

            mercenary.hasReceivedContract = true
            sendCabalDm(
                game,
                mercenaryPlayer,
                "누군가에게 의뢰를 받았습니다"
            )
            sendCabalDm(game, client, "용병 ${mercenaryPlayer.member.effectiveName}님에게 의뢰를 했습니다")
        }
    }

    private fun assignGraveRobbedMercenaryClientsAtFirstDay(game: Game) {
        if (game.dayCount != 1) return

        game.playerDatas.forEach { mercenaryPlayer ->
            val mercenary = mercenaryPlayer.job as? Mercenary ?: return@forEach
            if (mercenaryPlayer.state.isDead) return@forEach
            if (!mercenaryPlayer.state.hasCompletedGraveRobbing) return@forEach
            if (mercenary.clientPlayerId != null) return@forEach

            MercenaryClientManager.assignRandomClient(game, mercenaryPlayer)
        }
    }

    private suspend fun notifyMercenaryClientsAtFirstNight(game: Game) {
        if (game.dayCount != 1) return

        game.playerDatas.forEach { mercenaryPlayer ->
            val mercenary = mercenaryPlayer.job as? Mercenary ?: return@forEach
            val clientId = mercenary.clientPlayerId ?: return@forEach
            val client = game.getPlayer(clientId) ?: return@forEach

            GameReplayLogger.logDirectMessage(game, client, "의뢰인으로 지정되었습니다.", "용병 의뢰")
            runCatching {
                client.member.getDmChannel().createMessage("의뢰인으로 지정되었습니다.")
            }
        }
    }

    private suspend fun resolveNurseDoctorContactsFromFinalHeals(
        game: Game,
        finalHealEvents: List<GameEvent.PlayerHealed>,
        playersToDie: Set<PlayerData>
    ) {
        finalHealEvents.forEach { healEvent ->
            val doctorPlayer = healEvent.healer
            val nursePlayer = healEvent.target

            if (doctorPlayer in playersToDie || nursePlayer in playersToDie) return@forEach
            if (doctorPlayer.state.isDead || nursePlayer.state.isDead) return@forEach
            if (FrogCurseManager.shouldSuppressPassive(doctorPlayer)) return@forEach
            if (FrogCurseManager.shouldSuppressPassive(nursePlayer)) return@forEach
            if (doctorPlayer.job !is Doctor || nursePlayer.job !is Nurse) return@forEach

            notifyNurseDoctorContact(game, doctorPlayer, nursePlayer)
        }
    }

    private suspend fun notifyNurseDoctorContact(game: Game, doctorPlayer: PlayerData, nursePlayer: PlayerData) {
        val doctorJob = doctorPlayer.job as? Doctor ?: return
        val nurseJob = nursePlayer.job as? Nurse ?: return

        val firstContact = !nurseJob.hasContactedDoctor
        nurseJob.hasContactedDoctor = true
        nurseJob.contactedDoctorId = doctorPlayer.member.id
        doctorJob.hasContactedNurse = true

        if (!firstContact) return

        val nurseMessage = "$NURSE_DOCTOR_CONTACT_IMAGE_URL\n의사 (${mention(doctorPlayer)})님과 접선했습니다."
        val doctorMessage = "$NURSE_DOCTOR_CONTACT_IMAGE_URL\n간호사 (${mention(nursePlayer)})님과 접선했습니다."
        runCatching {
            GameReplayLogger.logDirectMessage(game, nursePlayer, nurseMessage, "간호사 접선")
            nursePlayer.member.getDmChannel().createMessage(nurseMessage)
        }
        runCatching {
            GameReplayLogger.logDirectMessage(game, doctorPlayer, doctorMessage, "간호사 접선")
            doctorPlayer.member.getDmChannel().createMessage(doctorMessage)
        }
    }

    private fun resolveNursePrescriptions(game: Game) {
        game.playerDatas.forEach { nursePlayer ->
            if (nursePlayer.state.isDead) return@forEach
            if (FrogCurseManager.shouldSuppressPassive(nursePlayer)) return@forEach
            val nurseJob = nursePlayer.job as? Nurse ?: return@forEach

            val targetId = nurseJob.prescribedTargetId ?: return@forEach
            val target = game.getPlayer(targetId) ?: return@forEach
            if (target.state.isDead) return@forEach

            if (target.job is Doctor) {
                val targetJob = target.job ?: return@forEach
                game.nightEvents += GameEvent.JobDiscovered(
                    discoverer = nursePlayer,
                    target = target,
                    actualJob = targetJob,
                    revealedJob = targetJob,
                    sourceAbilityName = "처방",
                    resolvedAt = DiscoveryStep.NIGHT,
                    notifyTarget = false
                )
            }
        }
    }

    private fun resolveDoctorHeals(game: Game): List<GameEvent.PlayerHealed> {
        val healers = game.playerDatas.filter { player ->
            val isDoctor = player.job is Doctor
            val isInheritedNurse = (player.job as? Nurse)?.canUseInheritedHeal == true
            val isStolenDoctor = (player.job as? Thief)?.stolenHealTargetId != null
            isDoctor || isInheritedNurse || isStolenDoctor
        }
        val healEvents = mutableListOf<GameEvent.PlayerHealed>()

        healers.forEach { player ->
            if (player.state.isDead) return@forEach
            if (FrogCurseManager.shouldSuppressPassive(player)) return@forEach

            val doctorJob = player.job as? Doctor
            val nurseJob = player.job as? Nurse
            val thiefJob = player.job as? Thief
            val targetId = doctorJob?.currentHealTarget ?: nurseJob?.currentHealTarget ?: thiefJob?.stolenHealTargetId ?: return@forEach
            val target = game.getPlayer(targetId) ?: run {
                doctorJob?.currentHealTarget = null
                nurseJob?.currentHealTarget = null
                thiefJob?.stolenHealTargetId = null
                return@forEach
            }

            val isAbsoluteHeal = doctorJob?.hasContactedNurse == true || nurseJob?.hasContactedDoctor == true
            val healEvent = GameEvent.PlayerHealed(
                healer = player,
                target = target,
                defenseTier = if (isAbsoluteHeal) DefenseTier.ABSOLUTE else DefenseTier.NORMAL
            )

            if (!isAbsoluteHeal) {
                player.job?.abilities
                    ?.filterIsInstance<PassiveAbility>()
                    ?.filterNot { FrogCurseManager.shouldSuppressPassive(player) }
                    ?.forEach { passive ->
                        passive.onEventObserved(game, player, healEvent)
                    }
            }

            target.state.healTier = maxOf(target.state.healTier, healEvent.defenseTier)

            applyCalmHarmfulEffectRemoval(game, player, target)

            game.nightEvents += healEvent
            healEvents += healEvent
            doctorJob?.currentHealTarget = null
            nurseJob?.currentHealTarget = null
            thiefJob?.stolenHealTargetId = null
        }

        return healEvents
    }

    private fun applyCalmFromHealEvents(
        game: Game,
        target: PlayerData,
        healEvents: List<GameEvent.PlayerHealed>
    ) {
        healEvents
            .filter { healEvent -> healEvent.target == target }
            .forEach { healEvent ->
                applyCalmHarmfulEffectRemoval(game, healEvent.healer, target)
            }
    }

    private fun applyCalmHarmfulEffectRemoval(game: Game, healer: PlayerData, target: PlayerData) {
        if (healer.allAbilities.none { it is Calm }) return

        val targetId = target.member.id
        val hadPoisonEffect =
            target.state.isPoisoned ||
                target.state.poisonedDeathDay != null ||
                targetId in game.pendingPoisonNotifications
        val hadSeductionEffect = target.state.isSilenced
        val hadThreatEffect =
            target.state.isThreatened ||
                targetId in game.activeThreatenedVoters ||
                game.playerDatas.any { player ->
                    (player.job as? Gangster)?.threatenedTargetIdsTonight?.contains(targetId) == true ||
                        (player.job as? Thief)?.stolenThreatenedTargetIdsTonight?.contains(targetId) == true
                }
        val hadSoulReleaseEffect = target.state.isShamaned
        val analysisOwners = game.playerDatas.filter { player ->
            player.job is MadScientist &&
                player.allAbilities.any { it is Analysis } &&
                player.state.madScientistLynchedVoteTargetId == targetId
        }
        val hackerOwners = game.playerDatas.filter { player ->
            (player.job as? Hacker)?.hackedTargetId == targetId ||
                (player.job as? Thief)?.stolenHackerTargetId == targetId
        }
        val removedEffectCount =
            listOf(hadSeductionEffect, hadThreatEffect, hadSoulReleaseEffect, hadPoisonEffect).count { it } +
                analysisOwners.size +
                hackerOwners.size

        // NOTE: 진정의 제거 대상은 유혹, 공갈, 성불, 독살, 분석, 해킹이다.
        // 이후 해로운 효과(예: 저주, 봉인, 추가 상태이상 등)가 확장되면 여기에서 함께 정리한다.
        target.state.isSilenced = false
        game.seductionStatusByTarget.remove(targetId)
        target.state.isPoisoned = false
        target.state.poisonedDeathDay = null
        target.state.isThreatened = false
        target.state.isShamaned = false
        game.activeThreatenedVoters.remove(targetId)
        game.playerDatas.forEach { gangsterOwner ->
            val gangsterJob = gangsterOwner.job as? Gangster ?: return@forEach
            gangsterJob.threatenedTargetIdsTonight.remove(targetId)
        }
        game.playerDatas.forEach { thiefOwner ->
            val thiefJob = thiefOwner.job as? Thief ?: return@forEach
            thiefJob.stolenThreatenedTargetIdsTonight.remove(targetId)
        }
        analysisOwners.forEach { scientist ->
            scientist.state.madScientistLynchedVoteTargetId = null
        }
        hackerOwners.forEach { hackerOwner ->
            (hackerOwner.job as? Hacker)?.hackedTargetId = null
            (hackerOwner.job as? Thief)?.stolenHackerTargetId = null
        }

        if (removedEffectCount > 0) {
            game.pendingCalmRemovalNotificationCounts.merge(targetId, removedEffectCount, Int::plus)
        }
    }

    private fun resolveGangsterThreats(game: Game) {
        game.activeThreatenedVoters.clear()
        game.playerDatas.forEach { player ->
            val gangster = player.job as? Gangster ?: return@forEach
            gangster.threatenedTargetIdsTonight.toList().forEach { targetId ->
                val target = game.getPlayer(targetId) ?: return@forEach
                if (target.state.isDead) return@forEach
                if (shouldIgnoreHarmfulEffectByMentalStrength(game, target)) {
                    gangster.threatenedTargetIdsTonight.remove(targetId)
                    return@forEach
                }
                val shouldNotify = !target.state.isThreatened
                target.state.isThreatened = true
                game.activeThreatenedVoters[targetId] = player.member.id
                if (shouldNotify) {
                    notifyThreatenedByGangster(game, target)
                }
            }
        }
        game.playerDatas.forEach { player ->
            val thief = player.job as? Thief ?: return@forEach
            thief.stolenThreatenedTargetIdsTonight.toList().forEach { targetId ->
                val target = game.getPlayer(targetId) ?: return@forEach
                if (target.state.isDead) return@forEach
                if (shouldIgnoreHarmfulEffectByMentalStrength(game, target)) {
                    thief.stolenThreatenedTargetIdsTonight.remove(targetId)
                    return@forEach
                }
                val shouldNotify = !target.state.isThreatened
                target.state.isThreatened = true
                game.activeThreatenedVoters[targetId] = player.member.id
                if (shouldNotify) {
                    notifyThreatenedByGangster(game, target)
                }
            }
        }
    }

    private fun notifyThreatenedByGangster(game: Game, target: PlayerData) {
        cabalNotificationScope.launch {
            runCatching {
                val message = "의문의 괴한으로부터 협박을 당했습니다."
                GameReplayLogger.logDirectMessage(game, target, message, "협박")
                target.member.getDmChannel().createMessage(message)
            }
        }
    }

    private fun applyTravelCompanionPenalty(
        game: Game,
        successfulAttacks: Set<AttackEvent>,
        mafiaAttack: AttackEvent?
    ) {
        val attack = mafiaAttack ?: return
        if (attack !in successfulAttacks) return

        val deadGangster = attack.target
        val gangsterJob = deadGangster.job as? Gangster ?: return
        if (FrogCurseManager.shouldSuppressPassive(deadGangster)) return
        if (deadGangster.allAbilities.none { it is TravelCompanion }) return

        val killerId = attack.attacker.member.id
        if (killerId !in gangsterJob.threatenedTargetIdsTonight) return

        game.permanentlyDisenfranchisedVoters += killerId
        game.activeThreatenedVoters.remove(killerId)
    }

    private fun resolveAdministratorInvestigations(game: Game) {
        game.playerDatas.forEach { player ->
            val administratorJob = player.actualOrStolenJob<Administrator>() ?: return@forEach
            val selectedJobName = administratorJob.selectedInvestigationJobName ?: return@forEach
            val selectedJob = org.beobma.mafia42discordproject.job.JobManager.findByName(selectedJobName) ?: run {
                administratorJob.investigationResultPlayerId = null
                return@forEach
            }

            val alivePlayers = game.playerDatas.filter { !it.state.isDead }
            val spoofedTarget = alivePlayers.filter { candidate ->
                AdministratorInvestigationPolicy.shouldApplyHypocrisySpoof(game.dayCount, selectedJob, candidate)
            }.randomOrNull()

            val target = spoofedTarget ?: alivePlayers.filter { candidate ->
                FrogCurseManager.displayedJob(candidate)?.name == selectedJob.name
            }.randomOrNull()
            administratorJob.investigationResultPlayerId = target?.member?.id
        }
    }


    private suspend fun resolveHackerHacks(game: Game) {
        game.playerDatas.forEach { player ->
            if (player.state.isDead) return@forEach

            val hacker = player.actualOrStolenJob<Hacker>() ?: return@forEach
            if (hacker.hasResolvedHackDiscovery) return@forEach

            val hackedTargetId = hacker.hackedTargetId ?: return@forEach
            val target = game.getPlayer(hackedTargetId) ?: return@forEach
            val isIgnoredByMentalStrength =
                !target.state.isDead &&
                    shouldIgnoreHarmfulEffectByMentalStrength(game, target)
            if (target.state.isDead || isIgnoredByMentalStrength) {
                runCatching {
                    val message =
                        "해킹에 실패했습니다.\nhttps://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(2).webp"
                    GameReplayLogger.logDirectMessage(game, player, message, "해킹 실패")
                    player.member.getDmChannel().createMessage(message)
                }
                if (isIgnoredByMentalStrength) {
                    hacker.hackedTargetId = null
                }
                hacker.hasResolvedHackDiscovery = true
                return@forEach
            }

            val targetJob = target.job ?: return@forEach
            val shouldNotifyTarget =
                player.allAbilities.any { it is Synchronization } &&
                    InvestigationTeam.isCitizen(target)

            game.nightEvents += GameEvent.JobDiscovered(
                discoverer = player,
                target = target,
                actualJob = targetJob,
                revealedJob = targetJob,
                sourceAbilityName = "해킹",
                resolvedAt = DiscoveryStep.NIGHT,
                notifyTarget = shouldNotifyTarget
            )
            hacker.hasResolvedHackDiscovery = true
        }
    }

    private fun resolveReporterScoops(game: Game) {
        game.playerDatas.forEach { player ->
            val reporter = player.actualOrStolenJob<Reporter>() ?: return@forEach
            if (player.state.isDead) return@forEach
            if (!reporter.hasUsedScoop) return@forEach
            if (reporter.articlePublishDay != null) return@forEach

            val targetId = reporter.selectedTargetId ?: return@forEach
            val target = game.getPlayer(targetId) ?: return@forEach
            val actualJob = reporter.discoveredActualJobName
                ?.let(JobManager::findByName)
                ?: target.job
                ?: return@forEach

            if (reporter.discoveredJobName == null) {
                reporter.discoveredJobName = (FrogCurseManager.displayedJob(target) ?: actualJob).name
            }
            reporter.discoveredActualJobName = actualJob.name
            reporter.discoveredImageUrl = ReporterAssets.PUBLIC_SCOOP_ARTICLE_IMAGE_URL
            scheduleReporterArticle(game, player, reporter, actualJob)
        }
    }

    private fun scheduleReporterArticle(game: Game, player: PlayerData, reporter: Reporter, actualJob: Job) {
        reporter.articlePublishDay = if (game.dayCount == 1) 2 else game.dayCount
        reporter.articlePublishAtNightMidpoint = game.dayCount == 1 &&
            player.allAbilities.any { it is BreakingNews } &&
            actualJob is Mafia
    }

    private fun cacheReporterDiscoveryResults(events: List<GameEvent>) {
        events
            .filterIsInstance<GameEvent.JobDiscovered>()
            .filter { event ->
                event.sourceAbilityName == "특종" && !event.isCancelled
            }
            .forEach { event ->
                val reporter = event.discoverer.actualOrStolenJob<Reporter>() ?: return@forEach
                reporter.discoveredActualJobName = event.actualJob.name
                reporter.discoveredJobName = event.revealedJob.name
                reporter.discoveredImageUrl = ReporterAssets.PUBLIC_SCOOP_ARTICLE_IMAGE_URL
            }
    }

    private suspend fun publishReporterArticles(
        game: Game,
        publishAtNightMidpoint: Boolean = false
    ): List<DawnAnnouncement> {
        val dawnAnnouncements = mutableListOf<DawnAnnouncement>()
        var hasPublishedPublicArticle = false
        game.playerDatas.forEach { player ->
            val reporter = player.actualOrStolenJob<Reporter>() ?: return@forEach
            if (player.state.isDead) return@forEach
            if (reporter.hasPublishedArticle) return@forEach
            if (reporter.articlePublishAtNightMidpoint != publishAtNightMidpoint) return@forEach

            val discoveredJobName = reporter.discoveredJobName ?: return@forEach
            val targetId = reporter.selectedTargetId ?: return@forEach
            val publishDay = reporter.articlePublishDay ?: return@forEach
            if (game.dayCount < publishDay) return@forEach

            val target = game.getPlayer(targetId) ?: return@forEach
            val canPublishOnDeadTarget = player.allAbilities.any { it is Obituary }
            if (target.state.isDead && !canPublishOnDeadTarget) {
                reporter.hasPublishedArticle = true
                runCatching {
                    val message = "취재 대상 ${target.member.effectiveName}님이 사망하여 기사를 발행하지 못했습니다."
                    GameReplayLogger.logDirectMessage(game, player, message, "취재 실패")
                    player.member.getDmChannel().createMessage(message)
                }
                return@forEach
            }

            val actualJob = reporter.discoveredActualJobName
                ?.let(JobManager::findByName)
                ?: target.job
                ?: return@forEach
            val revealedJob = findReporterArticleJob(discoveredJobName, actualJob)
            target.state.isJobPubliclyRevealed = true
            game.publiclyRevealedAbilityTargetIds += target.member.id
            game.publiclyRevealedJobNames += revealedJob.name

            val event = GameEvent.JobDiscovered(
                discoverer = player,
                target = target,
                actualJob = actualJob,
                revealedJob = revealedJob,
                sourceAbilityName = "특종",
                resolvedAt = DiscoveryStep.DAY,
                isPublicReveal = true,
                notifyTarget = false
            ).apply {
                imageUrl = reporter.discoveredImageUrl ?: ReporterAssets.PUBLIC_SCOOP_ARTICLE_IMAGE_URL
            }

            if (publishAtNightMidpoint) {
                if (hasPublishedPublicArticle) {
                    delay(DAWN_ANNOUNCEMENT_INTERVAL_MS)
                }
                coroutineScope {
                    launch { JobDiscoveryNotificationManager.notifyDiscoveredTargets(listOf(event), game) }
                    launch { game.playGameSound(REPORTER_SCOOP_SOUND_PATH) }
                }
                hasPublishedPublicArticle = true
            } else {
                SwindlerManager.notifyFooledByDiscovery(event)
                dawnAnnouncements += DawnAnnouncement(
                    imageUrl = event.imageUrl ?: ReporterAssets.PUBLIC_SCOOP_ARTICLE_IMAGE_URL,
                    message = "특종입니다! ${target.member.effectiveName}님이 ${revealedJob.name}(이)라는 소식입니다!",
                    priority = DAWN_PRIORITY_REPORTER,
                    targetId = target.member.id,
                    soundPath = REPORTER_SCOOP_SOUND_PATH
                )
            }
            game.publiclyRevealedJobNames += reporter.name
            reporter.hasPublishedArticle = true
        }
        return dawnAnnouncements
    }

    private fun findReporterArticleJob(jobName: String, fallback: Job): Job {
        return JobManager.findByName(jobName) ?: if (jobName == Frog().name) Frog() else fallback
    }

    private fun applyMafiaExecutionFailureEffects(game: Game, mafiaAttack: AttackEvent) {
        val attacker = mafiaAttack.attacker
        val target = mafiaAttack.target

        if (attacker.allAbilities.any { it is Concealment }) {
            game.concealmentForcedQuietNight = true
        }

        if (attacker.allAbilities.any { it is Poisoning }) {
            if (shouldIgnoreHarmfulEffectByMentalStrength(game, target)) return
            target.state.isPoisoned = true
            target.state.poisonedDeathDay = game.dayCount + 1
            game.pendingPoisonNotifications[target.member.id] = attacker.member.id
        }
    }

    fun shouldIgnoreHarmfulEffectByMentalStrength(game: Game, target: PlayerData): Boolean {
        if (target.allAbilities.none { it is MentalStrength }) return false

        cabalNotificationScope.launch {
            runCatching {
                val message = "정신력의 힘으로 해로운 효과를 이겨냈습니다."
                GameReplayLogger.logDirectMessage(game, target, message, "정신력 발동")
                target.member.getDmChannel().createMessage(message)
            }
        }
        return true
    }

    private fun shouldNotifyAtDayStart(event: GameEvent): Boolean {
        val discoveredEvent = event as? GameEvent.JobDiscovered ?: return false
        return discoveredEvent.sourceAbilityName == "암시"
    }

    private fun isPresentedAsDawnAnnouncement(event: GameEvent): Boolean {
        val discoveredEvent = event as? GameEvent.JobDiscovered ?: return false
        return discoveredEvent.isPublicReveal && discoveredEvent.sourceAbilityName == "방탄"
    }

    private fun notifyPendingPoisonEffects(game: Game) {
        val poisonNotifications = game.pendingPoisonNotifications.toMap()
        val calmRemovalNotificationCounts = game.pendingCalmRemovalNotificationCounts.toMap()
        game.pendingPoisonNotifications.clear()
        game.pendingCalmRemovalNotificationCounts.clear()

        poisonNotifications.forEach { (targetId, attackerId) ->
            val target = game.getPlayer(targetId) ?: return@forEach
            val attacker = game.getPlayer(attackerId)

            cabalNotificationScope.launch {
                runCatching {
                    val poisonMessage = "중독 상태가 되었습니다."
                    GameReplayLogger.logDirectMessage(game, target, poisonMessage, "중독 알림")
                    val dmChannel = target.member.getDmChannel()
                    dmChannel.createMessage(poisonMessage)

                    repeat(calmRemovalNotificationCounts[targetId] ?: 0) {
                        val removalMessage = "해로운 효과가 제거되었습니다."
                        GameReplayLogger.logDirectMessage(game, target, removalMessage, "진정 발동")
                        dmChannel.createMessage(removalMessage)
                    }
                }
            }

            if (attacker != null) {
                cabalNotificationScope.launch {
                    runCatching {
                        val message = "${target.member.effectiveName}님이 중독 상태가 되었습니다."
                        GameReplayLogger.logDirectMessage(game, attacker, message, "중독 알림")
                        attacker.member.getDmChannel().createMessage(message)
                    }
                }
            }
        }

        (calmRemovalNotificationCounts.keys - poisonNotifications.keys).forEach { targetId ->
            val target = game.getPlayer(targetId) ?: return@forEach
            val removalCount = calmRemovalNotificationCounts[targetId] ?: return@forEach
            cabalNotificationScope.launch {
                runCatching {
                    val dmChannel = target.member.getDmChannel()
                    repeat(removalCount) {
                        val message = "해로운 효과가 제거되었습니다."
                        GameReplayLogger.logDirectMessage(game, target, message, "진정 발동")
                        dmChannel.createMessage(message)
                    }
                }
            }
        }
    }

    private fun applyMafiaExecutionSuccessEffects(game: Game, mafiaAttack: AttackEvent) {
        val attacker = mafiaAttack.attacker
        val target = mafiaAttack.target

        if (
            attacker.allAbilities.any { it is Exorcism } &&
            target.job !is Evil &&
            target.allAbilities.none { it is EarthboundSpirit }
        ) {
            target.state.isShamaned = true
        }
    }

    private fun applyProbationEffectsAfterGraveRobbing(
        game: Game,
        successfulProbationAttacks: Collection<AttackEvent>
    ) {
        successfulProbationAttacks.forEach { attack ->
            val target = attack.target
            val discoveredJob = target.job ?: return@forEach
            game.nightEvents += GameEvent.JobDiscovered(
                discoverer = attack.attacker,
                target = target,
                actualJob = discoveredJob,
                revealedJob = discoveredJob,
                sourceAbilityName = "수습",
                resolvedAt = DiscoveryStep.NIGHT,
                imageUrl = PROBATION_DISCOVERY_IMAGE_URL
            )
            game.probationOriginalJobsByPlayer[target.member.id] = discoveredJob
        }
    }

    private fun applyProbationCitizenConversion(game: Game) {
        if (game.probationOriginalJobsByPlayer.isEmpty()) return

        game.probationOriginalJobsByPlayer.forEach { (playerId, originalJob) ->
            if (originalJob is Evil) return@forEach
            val player = game.getPlayer(playerId) ?: return@forEach
            player.job = Citizen()
        }
    }

}
