package org.beobma.mafia42discordproject.game

import dev.kord.common.entity.ButtonStyle
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.beobma.mafia42discordproject.discord.DiscordMessageManager.sendMainChannelMessageWithImage
import org.beobma.mafia42discordproject.discord.DiscordMessageManager.sendMainChannerMessage
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.*
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.administrator.AdministratorInvestigationPolicy
import org.beobma.mafia42discordproject.job.ability.general.definition.list.detective.DetectiveAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.mentalist.MentalistAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.doctor.DoctorAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.police.Autopsy
import org.beobma.mafia42discordproject.job.ability.general.definition.list.police.Confidential
import org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.Concealment
import org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.Exorcism
import org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.Poisoning
import org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.Probation
import org.beobma.mafia42discordproject.job.ability.general.definition.list.police.Warrant
import org.beobma.mafia42discordproject.job.ability.general.definition.list.doctor.Calm
import org.beobma.mafia42discordproject.job.definition.list.Administrator
import org.beobma.mafia42discordproject.job.definition.list.Cabal
import org.beobma.mafia42discordproject.job.definition.list.CabalRole
import org.beobma.mafia42discordproject.job.definition.list.Citizen
import org.beobma.mafia42discordproject.job.definition.list.Couple
import org.beobma.mafia42discordproject.job.definition.list.CoupleRole
import org.beobma.mafia42discordproject.job.definition.list.Doctor
import org.beobma.mafia42discordproject.job.ability.general.definition.list.fortuneteller.Arcana
import org.beobma.mafia42discordproject.job.ability.general.definition.list.hacker.Synchronization
import org.beobma.mafia42discordproject.job.definition.list.Detective
import org.beobma.mafia42discordproject.job.definition.list.Fortuneteller
import org.beobma.mafia42discordproject.job.definition.list.Gangster
import org.beobma.mafia42discordproject.job.definition.list.Hacker
import org.beobma.mafia42discordproject.job.definition.list.Hypnotist
import org.beobma.mafia42discordproject.job.definition.list.Judge
import org.beobma.mafia42discordproject.job.definition.list.Mercenary
import org.beobma.mafia42discordproject.job.definition.list.Nurse
import org.beobma.mafia42discordproject.job.definition.list.Police
import org.beobma.mafia42discordproject.job.definition.list.Politician
import org.beobma.mafia42discordproject.job.ability.general.definition.list.gangster.CombinedAttack
import org.beobma.mafia42discordproject.job.ability.general.definition.list.gangster.TravelCompanion
import org.beobma.mafia42discordproject.job.ability.general.definition.list.martyr.Explosion
import org.beobma.mafia42discordproject.job.ability.general.definition.list.martyr.Flash
import org.beobma.mafia42discordproject.job.ability.general.definition.list.other.Resolute
import org.beobma.mafia42discordproject.job.evil.Evil
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import org.beobma.mafia42discordproject.job.definition.list.Martyr
import org.beobma.mafia42discordproject.job.definition.list.Mentalist

object GameLoopManager {
    private const val NIGHT_DURATION_MS = 25_000L
    private const val DAWN_DURATION_MS = 10_000L
    private const val VOTE_DURATION_MS = 15_000L
    private const val INITIAL_VOTE_REVEAL_DURATION_MS = 5_000L
    private const val FINAL_VOTE_TALLY_STEP_MS = 500L
    private const val DEFENSE_DURATION_MS = 15_000L
    private const val PROS_CONS_VOTE_DURATION_MS = 10_000L
    private const val TIME_THREAD_NAME = "시간"

    private var timeThreadChannel: ThreadChannel? = null
    private var timeStatusMessage: Message? = null
    private val cabalNotificationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val votePresentationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun resetTimeThreadState() {
        timeThreadChannel = null
        timeStatusMessage = null
    }

    private suspend fun runPhaseCountdown(game: Game, label: String, durationMillis: Long) {
        updateTimeStatusMessage(game, label, durationMillis)
        delay(durationMillis.coerceAtLeast(0L))
        updateTimeStatusMessageAtZero(game, label)
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
        game.currentPhase = GamePhase.NIGHT
        game.dayCount += 1
        game.abilityUsersThisPhase.clear()
        game.abilityTargetByUserThisPhase.clear()
        game.nightAttacks.clear()
        game.nightDeathCandidates.clear()
        game.nightEvents.clear()
        game.concealmentForcedQuietNight = false
        game.coupleSacrificeMap.clear()
        game.activeThreatenedVoters.clear()
        game.probationOriginalJobsByPlayer.clear()
        game.lastNightSummary = NightResolutionSummary()
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
            (player.job as? Detective)?.let {
                DetectiveAbility.resetNightState(player)
            }
            (player.job as? Administrator)?.let { administratorJob ->
                administratorJob.investigationResultPlayerId = null
            }
            (player.job as? Gangster)?.prepareNightThreatSelection()
            (player.job as? Hypnotist)?.selectedTargetIdTonight = null
            (player.job as? Mentalist)?.let {
                MentalistAbility.resetDayState(player)
            }
        }
        resolveCabalSunInvestigation(game)
        applyPoliceConfidentialInvestigation(game)

        game.sendMainChannelMessageWithImage(
            imageLink = "https://cdn.discordapp.com/attachments/1483977619258212392/1483978042673070342/43e6c3860a090af9.png?ex=69be8800&is=69bd3680&hm=1dabf5630544f8f8766c7abbb0793a48e3a11e1364a31d1e4e439fff70539e25&",
            message = "밤이 되었습니다."
        )

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

        mainChannel.edit {
            addRoleOverwrite(game.guild.id) {
                denied = Permissions(Permission.SendMessages, Permission.ReadMessageHistory)
                allowed = Permissions(Permission.SendMessages, Permission.UseApplicationCommands)
            }
        }
        updateMafiaChannelPermissions(game, mafiaChannel, isNight = true)
        updateCoupleChannelPermissions(game, coupleChannel, isNight = true)
        updateDeadChannelPermissions(game, deadChannel)

        mafiaChannel.edit {
            addRoleOverwrite(game.guild.id) {
                allowed = Permissions(Permission.SendMessages, Permission.UseApplicationCommands)
                denied = Permissions(Permission.ReadMessageHistory)
            }
        }

        alivePlayers.forEach { player ->
            player.allAbilities
                .filterIsInstance<PassiveAbility>()
                .sortedByDescending(PassiveAbility::priority)
                .forEach { passive ->
                    passive.onPhaseChanged(game, player, GamePhase.NIGHT)
                }
        }
    }

    suspend fun resolveNightPhase(game: Game): NightResolutionSummary {
        val blockedAttacks = mutableListOf<AttackEvent>()
        val playersToDie = linkedSetOf<PlayerData>().apply {
            addAll(game.nightDeathCandidates)
        }

        resolveGangsterThreats(game)
        resolveNursePrescriptions(game)
        resolveDoctorHeals(game)
        resolveAdministratorInvestigations(game)
        resolveFortunetellerFortunes(game)
        resolveHackerHacks(game)

        game.nightAttacks.values.forEach { attackEvent ->
            val target = attackEvent.target
            if (target.state.isDead) return@forEach

            applyInnateNightDefense(game, target, attackEvent)

            if (target.state.healTier.level >= attackEvent.attackTier.level) {
                blockedAttacks += attackEvent
                playersToDie.remove(target)
            } else {
                playersToDie += target
            }
        }

        resolveMercenaryAttackOrder(game, blockedAttacks, playersToDie)
        resolveMercenaryContractDeaths(game, blockedAttacks, playersToDie)

        resolveMartyrNightExplosions(game, playersToDie)

        val mafiaAttack = game.nightAttacks["MAFIA_TEAM"]
        if (mafiaAttack != null) {
            val targetSurvived = mafiaAttack.target !in playersToDie
            game.mafiaAttackFailedPreviousNight = targetSurvived

            if (targetSurvived) {
                applyMafiaExecutionFailureEffects(game, mafiaAttack)
            } else {
                registerCoupleResentment(game, mafiaAttack)
                applyMafiaExecutionSuccessEffects(game, mafiaAttack)
            }
        } else {
            game.mafiaAttackFailedPreviousNight = false
        }
        applyTravelCompanionPenalty(game, playersToDie, mafiaAttack)

        playersToDie.forEach { victim ->
            game.nightEvents += GameEvent.PlayerDied(victim)
        }

        val processedEvents = dispatchEvents(game)
        JobDiscoveryNotificationManager.notifyDiscoveredTargets(processedEvents)
        val deaths = playersToDie.toList()
        val dawnPresentation = buildDawnPresentation(game, deaths)

        // 아침 이벤트(예: 도굴꾼 JobDiscovered) 해소를 위한 추가 디스패치 파이프라인 보수 및 유실 파기 방지
        val additionalProcessedEvents = dispatchEvents(game)
        JobDiscoveryNotificationManager.notifyDiscoveredTargets(additionalProcessedEvents)

        val summary = NightResolutionSummary(
            processedEvents = processedEvents + additionalProcessedEvents,
            deaths = deaths,
            blockedAttacks = blockedAttacks.toList(),
            dawnPresentation = dawnPresentation
        )
        game.lastNightSummary = summary

        game.nightAttacks.clear()
        game.nightDeathCandidates.clear()
        game.nightEvents.clear()
        game.playerDatas.forEach { player ->
            (player.job as? Doctor)?.currentHealTarget = null
            (player.job as? Nurse)?.currentHealTarget = null
            (player.job as? Gangster)?.finalizeNightThreatSelection()
            (player.job as? Hypnotist)?.let { hypnotist ->
                if (hypnotist.blockedNightsRemaining > 0) {
                    hypnotist.blockedNightsRemaining -= 1
                }
            }
            player.state.resetForNextPhase()
        }

        return summary
    }

    suspend fun resolveDawnPhase(game: Game, summary: NightResolutionSummary = game.lastNightSummary) {
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
                victim.state.isDead = true
                game.nightEvents += GameEvent.PlayerDied(victim)
                applyPoliceAutopsy(game, victim)
            }
        }

        summary.deaths.forEach { victim ->
            if (victim.state.isDead) return@forEach
            victim.state.isDead = true
            game.nightEvents += GameEvent.PlayerDied(victim)
            applyPoliceAutopsy(game, victim)
        }

        announceCoupleSacrificeReveal(game, summary.deaths)

        val processedDawnEvents = dispatchEvents(game)
        resolveCabalSpecialWinReadiness(game)
        val dawnPresentation = buildDawnPresentation(game, summary.deaths)

        game.lastNightSummary = summary.copy(
            processedEvents = summary.processedEvents + processedDawnEvents,
            dawnPresentation = dawnPresentation
        )

        game.nightEvents.clear()
        game.coupleSacrificeMap.clear()
    }

    private fun registerCoupleResentment(game: Game, mafiaAttack: AttackEvent) {
        val victimCouple = mafiaAttack.target.job as? Couple ?: return
        val partnerId = victimCouple.pairedPlayerId ?: return
        val partner = game.getPlayer(partnerId) ?: return
        val partnerCouple = partner.job as? Couple ?: return
        if (partner.state.isDead) return

        partnerCouple.avengedMafiaIds += mafiaAttack.attacker.member.id
    }

    private suspend fun announceCoupleSacrificeReveal(game: Game, deaths: List<PlayerData>) {
        val mainChannel = game.mainChannel ?: return

        deaths.forEach { deadPlayer ->
            val originalTargetId = game.coupleSacrificeMap[deadPlayer.member.id] ?: return@forEach
            val originalTarget = game.getPlayer(originalTargetId) ?: return@forEach

            deadPlayer.state.isJobPubliclyRevealed = true
            originalTarget.state.isJobPubliclyRevealed = true

            val deadRole = (deadPlayer.job as? Couple)?.role
            val originalRole = (originalTarget.job as? Couple)?.role.toDisplayName()
            val deadJobName = deadPlayer.job?.name ?: "알 수 없음"
            val originalJobName = originalTarget.job?.name ?: "알 수 없음"

            // 1. 성별에 따른 이미지 URL 선택
            val imageUrl = when (deadRole) {
                CoupleRole.MALE -> SystemImage.DEATH_MALE_COUPLE.imageUrl
                CoupleRole.FEMALE -> SystemImage.DEATH_WOMAN_COUPLE.imageUrl
                else -> SystemImage.DEATH_BY_MAFIA.imageUrl
            }

            // 2. 메시지 구성
            val message = "연인의 희생이 발동했습니다. ${originalTarget.member.effectiveName}(${originalRole})의 대가로 ${deadPlayer.member.effectiveName}(${deadRole?.toDisplayName() ?: "미정"})가 대신 사망했습니다.\n" +
                    "직업 공개: ${originalTarget.member.effectiveName} - ${originalJobName}, ${deadPlayer.member.effectiveName} - ${deadJobName}"

            // 3. 텍스트 대신 이미지와 함께 전송
            game.sendMainChannelMessageWithImage(
                imageLink = imageUrl,
                message = message
            )
        }
    }

    private fun CoupleRole?.toDisplayName(): String = when (this) {
        CoupleRole.MALE -> "남성"
        CoupleRole.FEMALE -> "여성"
        null -> "미정"
    }

    suspend fun startDayPhase(
        game: Game,
        summary: NightResolutionSummary = game.lastNightSummary
    ) {
        game.unwrittenRuleBlockedTargetIdTonight = null
        val mainChannel = game.mainChannel ?: return
        val mafiaChannel = game.mafiaChannel ?: return
        val coupleChannel = game.coupleChannel ?: return
        val deadChannel = game.deadChannel ?: return

        // 1. 게임 상태 및 날짜 변경
        game.currentPhase = GamePhase.DAY
        game.abilityUsersThisPhase.clear()
        game.abilityTargetByUserThisPhase.clear()
        val dawnPresentation = summary.dawnPresentation ?: buildDefaultDawnPresentation(emptyList(), summary.deaths, game)
        notifyMercenaryContractReception(game)

        game.sendMainChannelMessageWithImage(
            imageLink = dawnPresentation.imageUrl,
            message = dawnPresentation.message
        )
        delay(3_000L)

        game.sendMainChannelMessageWithImage(
            imageLink = SystemImage.DAY_START.imageUrl,
            message = "날이 밝았습니다."
        )

        mainChannel.edit {
            addRoleOverwrite(game.guild.id) {
                allowed = Permissions(Permission.SendMessages)
                denied = Permissions()
            }

            game.playerDatas.forEach { player ->
                if (player.state.isDead || player.state.isSilenced) {
                    addMemberOverwrite(player.member.id) {
                        denied = Permissions(Permission.SendMessages)
                    }
                } else {
                    addMemberOverwrite(player.member.id) {
                        denied = Permissions()
                    }
                }
            }
        }

        game.playerDatas.forEach { player ->
            val shouldMute = player.state.isDead || player.state.isSilenced
            runCatching {
                player.member.edit {
                    muted = shouldMute
                }
            }
        }

        updateMafiaChannelPermissions(game, mafiaChannel, isNight = false)
        updateCoupleChannelPermissions(game, coupleChannel, isNight = false)
        updateDeadChannelPermissions(game, deadChannel)
        AdministratorInvestigationNotificationManager.notifyResults(game)

        game.playerDatas
            .filter { !it.state.isDead }
            .forEach { player ->
                player.allAbilities
                    .filterIsInstance<PassiveAbility>()
                    .sortedByDescending(PassiveAbility::priority)
                    .forEach { passive ->
                        passive.onPhaseChanged(game, player, GamePhase.DAY)
                    }
            }
    }

    private suspend fun updateMafiaChannelPermissions(game: Game, mafiaChannel: TextChannel, isNight: Boolean) {
        mafiaChannel.edit {
            addRoleOverwrite(game.guild.id) {
                denied = Permissions(
                    Permission.ViewChannel,
                    Permission.ReadMessageHistory,
                    Permission.SendMessages
                )
            }

            game.playerDatas.forEach { player ->
                if (player.job is Evil) {
                    val canSend = isNight && !player.state.isDead
                    addMemberOverwrite(player.member.id) {
                        allowed = Permissions(Permission.ViewChannel)
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
        }
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
                if (player.job is Couple) {
                    val canAccess = isNight && !player.state.isDead
                    addMemberOverwrite(player.member.id) {
                        allowed = if (canAccess) {
                            Permissions(Permission.ViewChannel)
                        } else {
                            Permissions()
                        }
                        denied = if (canAccess) {
                            Permissions(Permission.ReadMessageHistory)
                        } else {
                            Permissions(Permission.ViewChannel, Permission.ReadMessageHistory, Permission.SendMessages)
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
        }
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
                if (!player.state.isDead) {
                    addMemberOverwrite(player.member.id) {
                        denied = Permissions(
                            Permission.ViewChannel,
                            Permission.ReadMessageHistory,
                            Permission.SendMessages
                        )
                    }
                    return@forEach
                }

                val canSend = !player.state.isShamaned
                addMemberOverwrite(player.member.id) {
                    allowed = Permissions(Permission.ViewChannel, Permission.ReadMessageHistory)
                    denied = if (canSend) {
                        Permissions()
                    } else {
                        Permissions(Permission.SendMessages)
                    }
                }
            }
        }
    }

    suspend fun startVotePhase(game: Game) {
        val mainChannel = game.mainChannel ?: return
        game.currentPhase = GamePhase.VOTE
        game.currentMainVotes.clear()
        game.defenseTargetId = null

        val alivePlayers = game.playerDatas.filter { !it.state.isDead }

        game.sendMainChannelMessageWithImage(
            imageLink = "https://cdn.discordapp.com/attachments/1483977619258212392/1483981201428709456/bd6d8d833d736bf2.png?ex=69bfdc71&is=69be8af1&hm=ca26cbd8933d3968240055b67202bfec8b35a278559172435a4515ecf3921ddb&",
            message = "투표 시간입니다. 의심되는 사람을 투표하세요."
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
                delay(refreshInterval)
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
        val alivePlayers = game.playerDatas.filter { !it.state.isDead }
        val dictatorshipPolitician = findAliveDictatorshipPolitician(game)
        if (dictatorshipPolitician != null) {
            val politicianVoteTargetId = game.currentMainVotes[dictatorshipPolitician.member.id]
            val politicianTarget = politicianVoteTargetId
                ?.let { targetId -> game.getPlayer(Snowflake(targetId)) }
                ?.takeUnless { it.state.isDead }
            return if (politicianTarget != null) {
                mainChannel.createMessage(
                    "독재가 발동되어 ${dictatorshipPolitician.member.effectiveName}님의 선택으로 ${politicianTarget.member.effectiveName}님이 최후 변론 대상자로 지목되었습니다."
                )
                politicianTarget
            } else {
                game.sendMainChannelMessageWithImage(
                    imageLink = "https://cdn.discordapp.com/attachments/1483977619258212392/1484594233653465122/K5WjViOFIiajx3YUfctCF-wkTWwg-DnerBQ09EXEd5-Jxz6Yy0vAmAuM5XDOMIWqHpYOXk85dCobA6CkwzPxOILsPNTbKJgtpYa1DtnVqhceybFNoLK5kdEtPJr6x7rCpn5F3Au_wTeTK0zWtRNArQ.webp?ex=69becb9f&is=69bd7a1f&hm=95cc33354d29bf53d2a74db6ca5ac622b88ef11bfe5b9e419f6e7b38a6f2a8b4&",
                    message = "독재 상태에서 정치인의 투표가 없어 처형될 대상을 고르지 못했습니다."
                )
                null
            }
        }
        val authorityJudge = findRevealedAliveJudge(game)
        if (authorityJudge != null) {
            val judgeVoteTargetId = game.currentMainVotes[authorityJudge.member.id]
            val judgeTarget = judgeVoteTargetId
                ?.let { targetId -> game.getPlayer(Snowflake(targetId)) }
                ?.takeUnless { it.state.isDead }
            return if (judgeTarget != null) {
                mainChannel.createMessage(
                    "판사의 선고로 ${judgeTarget.member.effectiveName}님이 최후 변론 대상자로 지목되었습니다."
                )
                judgeTarget
            } else {
                game.sendMainChannelMessageWithImage(
                    imageLink = "https://cdn.discordapp.com/attachments/1483977619258212392/1484594233653465122/K5WjViOFIiajx3YUfctCF-wkTWwg-DnerBQ09EXEd5-Jxz6Yy0vAmAuM5XDOMIWqHpYOXk85dCobA6CkwzPxOILsPNTbKJgtpYa1DtnVqhceybFNoLK5kdEtPJr6x7rCpn5F3Au_wTeTK0zWtRNArQ.webp?ex=69becb9f&is=69bd7a1f&hm=95cc33354d29bf53d2a74db6ca5ac622b88ef11bfe5b9e419f6e7b38a6f2a8b4&",
                    message = "판사의 선고가 없어 처형될 대상을 고르지 못했습니다."
                )
                null
            }
        }
        val voteCounts = mutableMapOf<PlayerData, Int>()
        var invalidVoteCount = 0
        val weightedVoteTargets = mutableListOf<PlayerData>()
        val gangsterTransferredVoteWeights = mutableMapOf<Snowflake, Int>()
        game.activeThreatenedVoters.forEach { (threatenedId, gangsterId) ->
            val threatened = game.getPlayer(threatenedId) ?: return@forEach
            val gangster = game.getPlayer(gangsterId) ?: return@forEach
            if (threatened.state.isDead || gangster.state.isDead) return@forEach
            gangsterTransferredVoteWeights[gangsterId] =
                (gangsterTransferredVoteWeights[gangsterId] ?: 0) + 1
        }

        alivePlayers.forEach { voter ->
            if (voter.member.id in game.permanentlyDisenfranchisedVoters) {
                return@forEach
            }
            if (game.activeThreatenedVoters.containsKey(voter.member.id)) {
                return@forEach
            }

            val baseWeight = if (voter.job is Politician) 2 else 1
            val weightEvent = GameEvent.CalculateVoteWeight(voter, weight = baseWeight)
            voter.allAbilities
                .filterIsInstance<PassiveAbility>()
                .sortedByDescending(PassiveAbility::priority)
                .forEach { passive ->
                    passive.onEventObserved(game, voter, weightEvent)
                }
            weightEvent.weight += gangsterTransferredVoteWeights[voter.member.id] ?: 0
            if (weightEvent.weight <= 0) {
                return@forEach
            }

            val targetIdString = game.currentMainVotes[voter.member.id]
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

        if (weightedVoteTargets.isNotEmpty()) {
            delay(1_000L)
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
                            voteCounts = progressiveVoteCounts
                        )
                    }
                }
                delay(FINAL_VOTE_TALLY_STEP_MS)
            }
        }

        val maxVotes = voteCounts.values.maxOrNull() ?: 0
        if (invalidVoteCount >= maxVotes || maxVotes == 0) {
            game.sendMainChannelMessageWithImage(
                imageLink = "https://cdn.discordapp.com/attachments/1483977619258212392/1484594233653465122/K5WjViOFIiajx3YUfctCF-wkTWwg-DnerBQ09EXEd5-Jxz6Yy0vAmAuM5XDOMIWqHpYOXk85dCobA6CkwzPxOILsPNTbKJgtpYa1DtnVqhceybFNoLK5kdEtPJr6x7rCpn5F3Au_wTeTK0zWtRNArQ.webp?ex=69becb9f&is=69bd7a1f&hm=95cc33354d29bf53d2a74db6ca5ac622b88ef11bfe5b9e419f6e7b38a6f2a8b4&",
                message = "처형될 대상을 고르지 못했습니다."
            )
            return null
        }

        val maxVotedPlayers = voteCounts.filter { it.value == maxVotes }.keys.toList()

        if (maxVotedPlayers.size > 1) {
            game.sendMainChannelMessageWithImage(
                imageLink = "https://cdn.discordapp.com/attachments/1483977619258212392/1484594233653465122/K5WjViOFIiajx3YUfctCF-wkTWwg-DnerBQ09EXEd5-Jxz6Yy0vAmAuM5XDOMIWqHpYOXk85dCobA6CkwzPxOILsPNTbKJgtpYa1DtnVqhceybFNoLK5kdEtPJr6x7rCpn5F3Au_wTeTK0zWtRNArQ.webp?ex=69becb9f&is=69bd7a1f&hm=95cc33354d29bf53d2a74db6ca5ac622b88ef11bfe5b9e419f6e7b38a6f2a8b4&",
                message = "처형될 대상을 고르지 못했습니다."
            )
            return null
        }

        val finalTarget = maxVotedPlayers.first()
        return finalTarget
    }

    private fun buildMainVoteStatusContent(
        game: Game,
        alivePlayers: List<PlayerData>,
        isHidden: Boolean
    ): String {
        val currentVoteCounts = mutableMapOf<PlayerData, Int>()
        game.currentMainVotes.values.forEach { targetId ->
            val target = game.getPlayer(Snowflake(targetId)) ?: return@forEach
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
        voteCounts: Map<PlayerData, Int>
    ): String {
        return buildString {
            alivePlayers.forEach { player ->
                appendLine("- ${player.member.effectiveName}: ${voteCounts[player] ?: 0}표")
            }
        }
    }

    suspend fun startDefensePhase(game: Game, target: PlayerData) {
        val mainChannel = game.mainChannel ?: return
        game.defenseTargetId = target.member.id
        (target.job as? Martyr)?.defenseBombTargetId = null
        game.sendMainChannelMessageWithImage(
            imageLink = "https://cdn.discordapp.com/attachments/1483977619258212392/1484595217796567092/b1bb8f82a19e45e3.png?ex=69becc8a&is=69bd7b0a&hm=0facb3df92275cbd87534a5c337cb4c774643de1c0ec93529a105c1573f30f35&",
            message = "${target.member.effectiveName}의 최후의 변론"
        )

        mainChannel.edit {
            addRoleOverwrite(game.guild.id) {
                denied = Permissions(Permission.SendMessages)
            }

            if (!target.state.isSilenced) {
                addMemberOverwrite(target.member.id) {
                    allowed = Permissions(Permission.SendMessages)
                }
            }
        }
    }

    suspend fun startProsConsVotePhase(game: Game, target: PlayerData) {
        val mainChannel = game.mainChannel ?: return
        game.currentPhase = GamePhase.VOTE
        game.currentProsConsVotes.clear()

        mainChannel.createMessage {
            actionRow {
                interactionButton(ButtonStyle.Success, "vote_pros") {
                    label = "찬성"
                }
                interactionButton(ButtonStyle.Danger, "vote_cons") {
                    label = "반대"
                }
            }
        }
    }

    suspend fun resolveExecutionPhase(game: Game, target: PlayerData) {
        val mainChannel = game.mainChannel ?: return
        val deadChannel = game.deadChannel
        val dictatorshipPolitician = findAliveDictatorshipPolitician(game)
        val prosCount = game.currentProsConsVotes
            .filterValues { it }
            .keys
            .sumOf { voterId ->
                val voter = game.getPlayer(voterId)
                when {
                    voter == null || voter.state.isDead -> 0
                    voter.job is Politician -> 2
                    else -> 1
                }
            }
        val consCount = game.currentProsConsVotes
            .filterValues { !it }
            .keys
            .sumOf { voterId ->
                val voter = game.getPlayer(voterId)
                when {
                    voter == null || voter.state.isDead -> 0
                    voter.job is Politician -> 2
                    else -> 1
                }
            } +
            game.playerDatas.count { player ->
                !player.state.isDead &&
                    (player.member.id in game.permanentlyDisenfranchisedVoters ||
                        game.activeThreatenedVoters.containsKey(player.member.id))
            }
        val judgePlayer = findAliveJudge(game)
        val judgeVote = judgePlayer?.let { game.currentProsConsVotes[it.member.id] }
        val aggregateDecision = prosCount > consCount
        val judgeJob = judgePlayer?.job as? Judge
        val shouldRevealJudge = judgePlayer != null &&
            judgeJob != null &&
            !judgeJob.hasRevealedAuthority &&
            judgeVote != null &&
            judgeVote != aggregateDecision

        if (shouldRevealJudge) {
            judgeJob.hasRevealedAuthority = true
            judgePlayer.state.isJobPubliclyRevealed = true
            game.unwrittenRuleBlockedTargetIdTonight = judgePlayer.member.id

            mainChannel.createMessage(
                "판사 ${judgePlayer.member.effectiveName}님이 모습을 드러냈습니다. 선고에 따라 이번 투표는 ${if (judgeVote == true) "찬성" else "반대"}로 결정됩니다."
            )
        }

        notifyJudgeProsVoters(game, target)

        if (findRevealedAliveJudge(game) != null && judgeVote == null) {
            mainChannel.createMessage("판사가 찬반 선고를 하지 않아 이번 처형은 자동으로 반대로 처리됩니다.")
        }

        val finalDecision = when {
            dictatorshipPolitician != null -> game.currentProsConsVotes[dictatorshipPolitician.member.id] ?: false
            findRevealedAliveJudge(game) != null -> judgeVote ?: false
            else -> aggregateDecision
        }

        val executionEvent = GameEvent.DecideExecution(target, finalDecision)
        val alivePlayers = game.playerDatas.filter { !it.state.isDead }

        alivePlayers.forEach { player ->
            player.allAbilities
                .filterIsInstance<PassiveAbility>()
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

        if (target.job is Politician) {
            if (!target.state.isJobPubliclyRevealed) {
                target.state.isJobPubliclyRevealed = true
                game.unwrittenRuleBlockedTargetIdTonight = target.member.id
            }
            mainChannel.createMessage("정치인은 투표로 죽지 않습니다")
            game.defenseTargetId = null
            return
        }

        target.state.isDead = true
        game.nightEvents += GameEvent.PlayerDied(target, isLynch = true)
        applyPoliceAutopsy(game, target)
        resolveMartyrDefenseExplosion(game, target)
        dispatchEvents(game)
        game.nightEvents.clear()
        game.sendMainChannelMessageWithImage(
            imageLink = SystemImage.VOTE_EXECUTION.imageUrl,
            message = "${target.member.effectiveName}님이 투표로 처형당하였습니다."
        )

        if (deadChannel != null) {
            updateDeadChannelPermissions(game, deadChannel)
        }
        game.defenseTargetId = null
    }

    fun checkWinCondition(game: Game): Team? {
        if (isCabalSpecialWinReady(game)) {
            return Team.CABAL_SPECIAL
        }

        val alivePlayers = game.playerDatas.filter { !it.state.isDead }
        val mafiaCount = alivePlayers.count { it.job is Evil }
        val citizenCount = alivePlayers.sumOf { player ->
            if (player.job is Evil) {
                0
            } else {
                when (player.job) {
                    is Gangster -> 3
                    is Politician -> 2
                    else -> 1
                }
            }
        }
        val aliveCabals = alivePlayers.count { it.job is Cabal }

        val activeMercenaryExecution = game.playerDatas.any { player ->
            val mercenary = player.job as? Mercenary ?: return@any false
            mercenary.hasExecutionAuthority
        }

        return when {
            mafiaCount == 0 -> Team.CITIZEN
            mafiaCount >= citizenCount &&
                aliveCabals < 2 &&
                !isRevealedJudgeAlive(game) &&
                !activeMercenaryExecution &&
                findAliveDictatorshipPolitician(game) == null -> Team.MAFIA
            else -> null
        }
    }

    private fun findAliveDictatorshipPolitician(game: Game): PlayerData? {
        val aliveCitizens = game.playerDatas.filter { !it.state.isDead && it.job !is Evil }
        if (aliveCitizens.size != 1) return null
        return aliveCitizens.firstOrNull { it.job is Politician }
    }

    private fun findAliveJudge(game: Game): PlayerData? {
        return game.playerDatas.firstOrNull { !it.state.isDead && it.job is Judge }
    }

    private fun findRevealedAliveJudge(game: Game): PlayerData? {
        return findAliveJudge(game)?.takeIf { player ->
            val judgeJob = player.job as? Judge ?: return@takeIf false
            judgeJob.hasRevealedAuthority
        }
    }

    private fun isRevealedJudgeAlive(game: Game): Boolean {
        return findRevealedAliveJudge(game) != null
    }

    private fun notifyJudgeProsVoters(game: Game, target: PlayerData) {
        val judgePlayer = findAliveJudge(game) ?: return
        val prosVoters = game.currentProsConsVotes
            .filterValues { it }
            .keys
            .mapNotNull { voterId -> game.getPlayer(voterId) }
            .map { voter -> voter.member.effectiveName }

        val prosMessage = if (prosVoters.isEmpty()) {
            "없음"
        } else {
            prosVoters.joinToString(", ")
        }

        cabalNotificationScope.launch {
            runCatching {
                judgePlayer.member.getDmChannel().createMessage(
                    "관권 발동 정보: ${target.member.effectiveName} 처형 찬성 투표자 - $prosMessage"
                )
            }
        }
    }

    private suspend fun resolveMartyrNightExplosions(game: Game, playersToDie: MutableSet<PlayerData>) {
        val mainChannel = game.mainChannel

        game.playerDatas.forEach { player ->
            val martyr = player.job as? Martyr ?: return@forEach
            if (player !in playersToDie) return@forEach

            val selectedTargetId = martyr.nightBombTargetId ?: return@forEach
            val selectedTarget = game.getPlayer(selectedTargetId) ?: return@forEach
            if (selectedTarget.state.isDead) return@forEach

            val mafiaExecutionTarget = game.nightAttacks["MAFIA_TEAM"]?.target
            val isNightBombTriggered = mafiaExecutionTarget == player && selectedTarget.job is Mafia

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

            val hasFlash = player.allAbilities.any { it is Flash }
            if (hasFlash) {
                playersToDie -= player
            }

            player.state.isJobPubliclyRevealed = true
            selectedTarget.state.isJobPubliclyRevealed = true

            mainChannel?.createMessage(
                "테러리스트의 자폭이 발동했습니다. ${player.member.effectiveName}님과 ${selectedTarget.member.effectiveName}님의 정체가 공개됩니다.\n" +
                    "직업 공개: ${player.member.effectiveName} - ${player.job?.name ?: "알 수 없음"}, " +
                    "${selectedTarget.member.effectiveName} - ${selectedTarget.job?.name ?: "알 수 없음"}"
            )
        }
    }

    private suspend fun resolveMartyrDefenseExplosion(game: Game, executedTarget: PlayerData) {
        val martyr = executedTarget.job as? Martyr ?: return
        val selectedTargetId = martyr.defenseBombTargetId ?: return
        val selectedTarget = game.getPlayer(selectedTargetId) ?: return
        if (selectedTarget.state.isDead) return
        if (selectedTarget.member.id == executedTarget.member.id) return

        selectedTarget.state.isDead = true
        game.nightEvents += GameEvent.PlayerDied(selectedTarget, isLynch = true)
        applyPoliceAutopsy(game, selectedTarget)

        executedTarget.state.isJobPubliclyRevealed = true
        selectedTarget.state.isJobPubliclyRevealed = true

        game.mainChannel?.createMessage(
            "테러리스트의 산화가 발동했습니다. ${executedTarget.member.effectiveName}님과 ${selectedTarget.member.effectiveName}님이 함께 사망합니다.\n" +
                "직업 공개: ${executedTarget.member.effectiveName} - ${executedTarget.job?.name ?: "알 수 없음"}, " +
                "${selectedTarget.member.effectiveName} - ${selectedTarget.job?.name ?: "알 수 없음"}"
        )
    }

    suspend fun endGame(game: Game, winningTeam: Team) {
        game.isRunning = false
        game.currentPhase = GamePhase.END
        val resultMessage = "${winningTeam.displayName} 승리: ${winningTeam.winMessage}"

        if (winningTeam.winImageUrl != null) {
            game.sendMainChannelMessageWithImage(
                imageLink = winningTeam.winImageUrl,
                message = resultMessage
            )
        } else {
            // 이미지가 없다면 기존처럼 텍스트만 전송
            game.sendMainChannerMessage(resultMessage)
        }
    }

    suspend fun runGameLoop(game: Game) {
        while (game.isRunning) {
            startNightPhase(game)
            runPhaseCountdown(game, "밤", NIGHT_DURATION_MS)

            val nightSummary = resolveNightPhase(game)

            resolveDawnPhase(game, nightSummary)
            runPhaseCountdown(game, "새벽", DAWN_DURATION_MS)
            checkWinCondition(game)?.let { winner ->
                endGame(game, winner)
                break
            }

            startDayPhase(game, nightSummary)
            val discussionMillis = game.playerDatas.count { !it.state.isDead } * 15_000L
            runPhaseCountdown(game, "낮", discussionMillis)

            startVotePhase(game)
            runPhaseCountdown(game, "투표", VOTE_DURATION_MS)

            val target = resolveVotePhase(game)
            if (target != null) {
                startDefensePhase(game, target)
                runPhaseCountdown(game, "변론", DEFENSE_DURATION_MS)

                startProsConsVotePhase(game, target)
                runPhaseCountdown(game, "찬반 투표", PROS_CONS_VOTE_DURATION_MS)

                resolveExecutionPhase(game, target)
            }

            checkWinCondition(game)?.let { winner ->
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

            val selectedTargetId = sunCabal.selectedTargetId ?: return@forEach
            val selectedTarget = game.getPlayer(selectedTargetId)

            val isMoon = selectedTarget?.job is Cabal &&
                (selectedTarget.job as? Cabal)?.role == CabalRole.MOON &&
                selectedTarget.member.id == sunCabal.pairedPlayerId

            if (isMoon) {
                sunCabal.hasFoundMoon = true
                val moonCabal = selectedTarget?.job as? Cabal
                moonCabal?.wasFoundBySun = true
                sendCabalDm(
                    sunPlayer,
                    "밀사 결과: 맞다. ${selectedTarget.member.effectiveName}님이 달 비밀결사입니다."
                )
            } else {
                sendCabalDm(sunPlayer, "밀사 결과: 아니다.")
            }
        }
    }

    private fun resolveCabalSpecialWinReadiness(game: Game) {
        val aliveOrDeadCabals = game.playerDatas
            .mapNotNull { player ->
                val cabal = player.job as? Cabal ?: return@mapNotNull null
                player to cabal
            }
        val sun = aliveOrDeadCabals.firstOrNull { (_, cabal) -> cabal.role == CabalRole.SUN } ?: return
        val moon = aliveOrDeadCabals.firstOrNull { (_, cabal) -> cabal.role == CabalRole.MOON } ?: return

        val sunPlayer = sun.first
        val sunCabal = sun.second
        val moonPlayer = moon.first
        val moonCabal = moon.second

        val rolesStillCabal = sunPlayer.job is Cabal && moonPlayer.job is Cabal
        val moonMarkedSun = moonCabal.moonMarkedSunTonight && moonCabal.selectedTargetId == sunPlayer.member.id
        val canTrigger = rolesStillCabal && sunCabal.hasFoundMoon && moonCabal.wasFoundBySun && moonMarkedSun

        sunCabal.cabalSpecialWinReady = canTrigger
        moonCabal.cabalSpecialWinReady = canTrigger
    }

    private fun isCabalSpecialWinReady(game: Game): Boolean {
        return game.playerDatas.any { player ->
            val cabal = player.job as? Cabal ?: return@any false
            cabal.cabalSpecialWinReady
        }
    }

    private fun sendCabalDm(target: PlayerData, message: String) {
        cabalNotificationScope.launch {
            runCatching {
                target.member.getDmChannel().createMessage(message)
            }
        }
    }

    private fun buildDawnPresentation(game: Game, deaths: List<PlayerData>): DawnPresentation {
        if (game.concealmentForcedQuietNight) {
            return DawnPresentation(
                imageUrl = SystemImage.QUIET_NIGHT.imageUrl,
                message = "조용하게 밤이 넘어갔습니다."
            )
        }

        val attacks = game.nightAttacks.values.toList()
        val presentationEvent = GameEvent.ResolveDawnPresentation(
            dayCount = game.dayCount,
            attacks = attacks,
            deaths = deaths,
            presentation = buildDefaultDawnPresentation(attacks, deaths, game)
        )

        game.playerDatas
            .filter { !it.state.isDead }
            .forEach { player ->
                player.allAbilities
                    .filterIsInstance<PassiveAbility>()
                    .sortedByDescending(PassiveAbility::priority)
                    .forEach { passive ->
                        passive.onEventObserved(game, player, presentationEvent)
                    }
            }

        return presentationEvent.presentation
    }

    private fun buildDefaultDawnPresentation(
        attacks: List<AttackEvent>,
        deaths: List<PlayerData>,
        game: Game // Game 파라미터 추가 필요 (호출부에도 game을 넘겨주어야 함)
    ): DawnPresentation {

        // 연인이 희생해서 죽은 사람이 있는지 확인
        val hasCoupleSacrifice = deaths.any { it.member.id in game.coupleSacrificeMap }

        if (hasCoupleSacrifice) {
            // 이미 announceCoupleSacrificeReveal에서 화려하게 이미지를 띄웠으므로
            // 여기서는 조용히 넘어가거나, 아주 간략한 요약만 반환하게 합니다.
            return DawnPresentation(imageUrl = "", message = "")
        }

        // 기존 마피아 킬 로직
        val mafiaKillVictim = attacks
            .firstOrNull { it.attacker.job?.name == "마피아" }
            ?.target
            ?.takeIf { it in deaths }

        return if (mafiaKillVictim == null) {
            DawnPresentation(
                imageUrl = SystemImage.QUIET_NIGHT.imageUrl,
                message = "조용하게 밤이 넘어갔습니다."
            )
        } else {
            DawnPresentation(
                imageUrl = SystemImage.DEATH_BY_MAFIA.imageUrl,
                message = "${mafiaKillVictim.member.effectiveName}이(가) 살해당했습니다."
            )
        }
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
                        .sortedByDescending(PassiveAbility::priority)
                    if (passives.isEmpty()) null else player to passives
                }

            eventsToProcess.forEach { event ->
                applyNurseDoctorInheritanceOnDeath(game, event)
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
            if (policePlayer.member.id == victim.member.id) return@forEach
            if (policePlayer.allAbilities.none { it is Autopsy }) return@forEach

            val policeJob = policePlayer.job as? Police ?: return@forEach
            policeJob.eavesdroppingTargetId = victim.member.id
            policeJob.searchedTargets += victim.member.id

            votePresentationScope.launch {
                runCatching {
                    policePlayer.member.getDmChannel().createMessage(
                        "[부검] ${victim.member.effectiveName}님은 ${if (victim.job is Evil) "마피아 팀" else "시민 팀"}입니다."
                    )
                }
            }
        }
    }

    private fun applyPoliceConfidentialInvestigation(game: Game) {
        if (game.dayCount != 2) return

        game.playerDatas.forEach { policePlayer ->
            if (policePlayer.state.isDead) return@forEach
            if (policePlayer.allAbilities.none { it is Confidential }) return@forEach

            val policeJob = policePlayer.job as? Police ?: return@forEach
            if (policeJob.hasUsedConfidential) return@forEach

            val candidates = game.playerDatas.filter {
                !it.state.isDead && it.member.id != policePlayer.member.id
            }
            val selectedTarget = candidates.randomOrNull() ?: return@forEach

            policeJob.hasUsedConfidential = true
            policeJob.searchedTargets += selectedTarget.member.id

            votePresentationScope.launch {
                runCatching {
                    policePlayer.member.getDmChannel().createMessage(
                        "[기밀] ${selectedTarget.member.effectiveName}님 자동 조사 결과: ${if (selectedTarget.job is Evil) "마피아 팀" else "시민 팀"}"
                    )
                }
            }
        }
    }

    private fun resolveMercenaryAttackOrder(
        game: Game,
        blockedAttacks: List<AttackEvent>,
        playersToDie: MutableSet<PlayerData>
    ) {
        val mafiaAttack = game.nightAttacks["MAFIA_TEAM"] ?: return
        if (mafiaAttack in blockedAttacks) return

        val cancelledAttackKeys = mutableListOf<String>()
        val mercenaryAttackEntries = game.nightAttacks
            .filterKeys { it.startsWith("MERCENARY_") }
            .toList()
        if (mercenaryAttackEntries.isEmpty()) return

        mercenaryAttackEntries.forEach { (attackKey, mercenaryAttack) ->
            val mercenaryAttacker = mercenaryAttack.attacker
            if (mafiaAttack.target != mercenaryAttacker) return@forEach
            if (mercenaryAttack in blockedAttacks) return@forEach

            val hasResolute = mercenaryAttacker.allAbilities.any { it is Resolute }
            if (hasResolute) return@forEach

            cancelledAttackKeys += attackKey
            val target = mercenaryAttack.target
            val hasOtherUnblockedAttack = game.nightAttacks.any { (otherKey, otherAttack) ->
                otherKey != attackKey &&
                    otherAttack.target == target &&
                    otherAttack !in blockedAttacks
            }
            if (!hasOtherUnblockedAttack) {
                playersToDie.remove(target)
            }
        }

        cancelledAttackKeys.forEach { attackKey ->
            val cancelledAttack = game.nightAttacks.remove(attackKey) ?: return@forEach
            game.nightDeathCandidates.remove(cancelledAttack.target)
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

            val killingAttack = unblockedAttacks.firstOrNull { it.target == client } ?: return@forEach
            mercenary.hasExecutionAuthority = true
            mercenary.clientKilledByPlayerId = killingAttack.attacker.member.id
        }
    }

    private fun notifyMercenaryContractReception(game: Game) {
        game.playerDatas.forEach { mercenaryPlayer ->
            val mercenary = mercenaryPlayer.job as? Mercenary ?: return@forEach
            if (mercenary.hasReceivedContract) return@forEach
            if (mercenaryPlayer.state.isDead) return@forEach

            val clientId = mercenary.clientPlayerId ?: return@forEach
            val client = game.getPlayer(clientId) ?: return@forEach
            if (client.state.isDead) return@forEach

            mercenary.hasReceivedContract = true
            sendCabalDm(
                mercenaryPlayer,
                "의뢰를 받았습니다. 의뢰인은 ${client.member.effectiveName}님입니다."
            )
        }
    }

    private suspend fun resolveNursePrescriptions(game: Game) {
        val doctorPlayer = game.playerDatas.firstOrNull { it.job is Doctor } ?: return
        val doctorJob = doctorPlayer.job as? Doctor ?: return

        game.playerDatas.forEach { nursePlayer ->
            if (nursePlayer.state.isDead) return@forEach
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

            val contactedByNurseTarget = target.member.id == doctorPlayer.member.id
            val contactedByDoctorTarget = doctorJob.currentHealTarget == nursePlayer.member.id
            if (contactedByNurseTarget || contactedByDoctorTarget) {
                val firstContact = !nurseJob.hasContactedDoctor
                nurseJob.hasContactedDoctor = true
                nurseJob.contactedDoctorId = doctorPlayer.member.id
                doctorJob.hasContactedNurse = true

                if (firstContact) {
                    runCatching {
                        nursePlayer.member.getDmChannel().createMessage("접선에 성공했습니다. 의사의 치료가 절대 치료로 강화됩니다.")
                    }
                    runCatching {
                        doctorPlayer.member.getDmChannel().createMessage("간호사와 접선했습니다. 치료가 절대 치료로 강화됩니다.")
                    }
                }
            }
        }
    }

    private fun resolveDoctorHeals(game: Game) {
        val healers = game.playerDatas.filter { player ->
            val isDoctor = player.job is Doctor
            val isInheritedNurse = (player.job as? Nurse)?.canUseInheritedHeal == true
            isDoctor || isInheritedNurse
        }

        healers.forEach { player ->
            if (player.state.isDead) return@forEach

            val doctorJob = player.job as? Doctor
            val nurseJob = player.job as? Nurse
            val targetId = doctorJob?.currentHealTarget ?: nurseJob?.currentHealTarget ?: return@forEach
            val target = game.getPlayer(targetId) ?: run {
                doctorJob?.currentHealTarget = null
                nurseJob?.currentHealTarget = null
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
                    ?.forEach { passive ->
                        passive.onEventObserved(game, player, healEvent)
                    }
            }

            target.state.healTier = maxOf(target.state.healTier, healEvent.defenseTier)

            if (player.allAbilities.any { it is Calm }) {
                // NOTE: 현재는 마피아의 독살(중독)만 해로운 효과로 구현되어 있어 해당 상태만 해제한다.
                // 이후 해로운 효과(예: 저주, 봉인, 추가 상태이상 등)가 확장되면 여기에서 함께 정리한다.
                target.state.isPoisoned = false
                target.state.poisonedDeathDay = null
                target.state.isThreatened = false
                game.activeThreatenedVoters.remove(target.member.id)
                game.playerDatas.forEach { gangsterOwner ->
                    val gangsterJob = gangsterOwner.job as? Gangster ?: return@forEach
                    gangsterJob.threatenedTargetIdsTonight.remove(target.member.id)
                }
            }

            game.nightEvents += healEvent
            doctorJob?.currentHealTarget = null
            nurseJob?.currentHealTarget = null
        }
    }

    private fun resolveGangsterThreats(game: Game) {
        game.activeThreatenedVoters.clear()
        game.playerDatas.forEach { player ->
            val gangster = player.job as? Gangster ?: return@forEach
            gangster.threatenedTargetIdsTonight.forEach { targetId ->
                val target = game.getPlayer(targetId) ?: return@forEach
                if (target.state.isDead) return@forEach
                target.state.isThreatened = true
                game.activeThreatenedVoters[targetId] = player.member.id
            }
        }
    }

    private fun applyTravelCompanionPenalty(
        game: Game,
        playersToDie: Set<PlayerData>,
        mafiaAttack: AttackEvent?
    ) {
        val attack = mafiaAttack ?: return
        if (attack.target !in playersToDie) return

        val deadGangster = attack.target
        val gangsterJob = deadGangster.job as? Gangster ?: return
        if (deadGangster.allAbilities.none { it is TravelCompanion }) return

        val killerId = attack.attacker.member.id
        if (killerId !in gangsterJob.threatenedTargetIdsTonight) return

        game.permanentlyDisenfranchisedVoters += killerId
        game.activeThreatenedVoters.remove(killerId)
    }

    private fun resolveAdministratorInvestigations(game: Game) {
        game.playerDatas.forEach { player ->
            val administratorJob = player.job as? Administrator ?: return@forEach
            val selectedJobName = administratorJob.selectedInvestigationJobName ?: return@forEach
            val selectedJob = org.beobma.mafia42discordproject.job.JobManager.findByName(selectedJobName) ?: run {
                administratorJob.investigationResultPlayerId = null
                return@forEach
            }

            val alivePlayers = game.playerDatas.filter { !it.state.isDead }
            val spoofedTarget = alivePlayers.firstOrNull { candidate ->
                AdministratorInvestigationPolicy.shouldApplyHypocrisySpoof(game.dayCount, selectedJob, candidate)
            }

            val target = spoofedTarget ?: alivePlayers.firstOrNull { candidate ->
                candidate.job?.name == selectedJob.name
            }
            administratorJob.investigationResultPlayerId = target?.member?.id
        }
    }


    private suspend fun resolveFortunetellerFortunes(game: Game) {
        game.playerDatas.forEach { player ->
            if (player.state.isDead) return@forEach

            val fortuneteller = player.job as? Fortuneteller ?: return@forEach
            val fixedTargetId = fortuneteller.fixedFortuneTargetId ?: return@forEach
            val target = game.getPlayer(fixedTargetId) ?: return@forEach

            if (target.state.isDead) {
                FortunetellerNotificationManager.notifyUnavailableTarget(player, target)
                return@forEach
            }

            val targetJobName = target.job?.name ?: return@forEach
            val gameJobNames = game.playerDatas
                .mapNotNull { it.job?.name }
                .distinct()

            val decoyPool = gameJobNames.filter { it != targetJobName }
            val decoyJobName = (decoyPool.ifEmpty { gameJobNames }).randomOrNull() ?: return@forEach
            val shownJobs = listOf(targetJobName, decoyJobName).shuffled()

            val arcanaTargets = if (player.allAbilities.any { it is Arcana }) {
                selectArcanaTargets(
                    game = game,
                    fortuneteller = player,
                    fixedTarget = target,
                    shownJobs = shownJobs,
                    targetJobName = targetJobName
                )
            } else {
                emptyList()
            }

            FortunetellerNotificationManager.notifyFortuneResult(
                fortuneteller = player,
                target = target,
                shownJobs = shownJobs,
                arcanaTargets = arcanaTargets
            )
        }
    }

    private fun resolveHackerHacks(game: Game) {
        game.playerDatas.forEach { player ->
            if (player.state.isDead) return@forEach

            val hacker = player.job as? Hacker ?: return@forEach
            if (hacker.hasResolvedHackDiscovery) return@forEach

            val hackedTargetId = hacker.hackedTargetId ?: return@forEach
            val target = game.getPlayer(hackedTargetId) ?: return@forEach
            if (target.state.isDead) return@forEach

            val targetJob = target.job ?: return@forEach
            val shouldNotifyTarget =
                player.allAbilities.any { it is Synchronization } &&
                    targetJob !is Evil

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

    private fun selectArcanaTargets(
        game: Game,
        fortuneteller: PlayerData,
        fixedTarget: PlayerData,
        shownJobs: List<String>,
        targetJobName: String
    ): List<PlayerData> {
        val candidates = game.playerDatas.filter { it.member.id != fixedTarget.member.id }
        if (candidates.isEmpty()) return emptyList()

        val complementaryRole = shownJobs.firstOrNull { it != targetJobName }

        val complementaryCandidates = candidates.filter { it.job?.name == complementaryRole }.shuffled()
        val shownJobCandidates = candidates.filter { candidate ->
            val jobName = candidate.job?.name
            jobName != null && jobName in shownJobs
        }.shuffled()
        val nonShownJobCandidates = candidates.filter { candidate ->
            val jobName = candidate.job?.name
            jobName == null || jobName !in shownJobs
        }.shuffled()

        val selected = mutableListOf<PlayerData>()

        complementaryCandidates.firstOrNull { it.member.id != fortuneteller.member.id }
            ?.let { selected += it }
            ?: complementaryCandidates.firstOrNull()?.let { selected += it }
            ?: shownJobCandidates.firstOrNull { it.member.id != fortuneteller.member.id }?.let { selected += it }
            ?: shownJobCandidates.firstOrNull()?.let { selected += it }

        nonShownJobCandidates.firstOrNull { it !in selected && it.member.id != fortuneteller.member.id }
            ?.let { selected += it }
            ?: nonShownJobCandidates.firstOrNull { it !in selected }?.let { selected += it }

        if (selected.size < 2) {
            candidates
                .filter { it !in selected }
                .shuffled()
                .take(2 - selected.size)
                .forEach { selected += it }
        }

        return selected.take(2)
    }

    private fun applyMafiaExecutionFailureEffects(game: Game, mafiaAttack: AttackEvent) {
        val attacker = mafiaAttack.attacker
        val target = mafiaAttack.target

        if (attacker.allAbilities.any { it is Concealment }) {
            game.concealmentForcedQuietNight = true
        }

        if (attacker.allAbilities.any { it is Poisoning }) {
            target.state.isPoisoned = true
            target.state.poisonedDeathDay = game.dayCount + 1
        }
    }

    private fun applyMafiaExecutionSuccessEffects(game: Game, mafiaAttack: AttackEvent) {
        val attacker = mafiaAttack.attacker
        val target = mafiaAttack.target

        if (attacker.allAbilities.any { it is Exorcism } && target.job !is Evil) {
            target.state.isShamaned = true
        }

        if (attacker.allAbilities.any { it is Probation }) {
            val originalJob = target.job ?: return
            game.nightEvents += GameEvent.JobDiscovered(
                discoverer = attacker,
                target = target,
                actualJob = originalJob,
                revealedJob = originalJob,
                sourceAbilityName = "수습",
                resolvedAt = DiscoveryStep.NIGHT
            )
            game.probationOriginalJobsByPlayer[target.member.id] = originalJob
            target.job = Citizen()
        }
    }

    private fun resolvePoliceSearches(game: Game) {
        game.playerDatas.forEach { player ->
            val policeJob = player.job as? Police ?: return@forEach
            val targetId = policeJob.currentSearchTarget ?: return@forEach
            val target = game.getPlayer(targetId) ?: run {
                policeJob.currentSearchTarget = null
                return@forEach
            }

            val isRepeatedSearch = targetId in policeJob.searchedTargets
            game.nightEvents += GameEvent.PoliceSearchResolved(
                police = player,
                target = target,
                isMafia = target.job is Mafia,
                isRepeatedSearch = isRepeatedSearch
            )

            val warrant = player.allAbilities.filterIsInstance<Warrant>().firstOrNull()
            if (warrant?.shouldRevealJob(targetId, policeJob.searchedTargets) == true) {
                val actualJob = target.job
                if (actualJob != null) {
                    game.nightEvents += GameEvent.PoliceJobRevealed(
                        police = player,
                        target = target,
                        actualJob = actualJob,
                        revealedJob = actualJob,
                        resolvedAt = DiscoveryStep.NIGHT
                    )
                }
            }

            policeJob.searchedTargets += targetId
            policeJob.currentSearchTarget = null
        }
    }

    private fun applyInnateNightDefense(game: Game, target: PlayerData, attackEvent: AttackEvent) {
        // 1. 피격 직전(BeforeAttackEvaluated) 이벤트를 생성합니다.
        val event = GameEvent.BeforeAttackEvaluated(attackEvent)

        // 2. 타겟이 가진 패시브 능력들에게 이벤트를 전파하여 '방탄' 등이 스스로 방어(healTier 상승)하도록 합니다.
        target.allAbilities
            .filterIsInstance<PassiveAbility>()
            .sortedByDescending(PassiveAbility::priority)
            .forEach { passive ->
                passive.onEventObserved(game, target, event)
            }
    }
}
