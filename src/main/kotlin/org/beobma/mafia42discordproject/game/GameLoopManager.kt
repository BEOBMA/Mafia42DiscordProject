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
import org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.Concealment
import org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.Exorcism
import org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.Poisoning
import org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.Probation
import org.beobma.mafia42discordproject.job.ability.general.definition.list.police.Warrant
import org.beobma.mafia42discordproject.job.definition.list.Administrator
import org.beobma.mafia42discordproject.job.definition.list.Cabal
import org.beobma.mafia42discordproject.job.definition.list.CabalRole
import org.beobma.mafia42discordproject.job.definition.list.Citizen
import org.beobma.mafia42discordproject.job.definition.list.Couple
import org.beobma.mafia42discordproject.job.definition.list.CoupleRole
import org.beobma.mafia42discordproject.job.definition.list.Doctor
import org.beobma.mafia42discordproject.job.definition.list.Detective
import org.beobma.mafia42discordproject.job.definition.list.Police
import org.beobma.mafia42discordproject.job.evil.Evil
import org.beobma.mafia42discordproject.job.evil.list.Mafia

object GameLoopManager {
    private const val NIGHT_DURATION_MS = 25_000L
    private const val DAWN_DURATION_MS = 10_000L
    private const val VOTE_DURATION_MS = 15_000L
    private const val INITIAL_VOTE_REVEAL_DURATION_MS = 5_000L
    private const val FINAL_VOTE_TALLY_STEP_MS = 500L
    private const val DEFENSE_DURATION_MS = 15_000L
    private const val PROS_CONS_VOTE_DURATION_MS = 10_000L
    private const val QUIET_NIGHT_IMAGE_URL =
        "https://cdn.discordapp.com/attachments/1483977619258212392/1483980003015397446/d8692f78c3528f76.png?ex=69bc8f93&is=69bb3e13&hm=1378e1b6daba26baddf0cc5d042087b7c5151860d709a3140414b97f774b77a4&"
    private const val DEATH_NIGHT_IMAGE_URL =
        "https://cdn.discordapp.com/attachments/1483977619258212392/1483980246448603146/99cb963d1b44dc2e.png?ex=69bc8fcd&is=69bb3e4d&hm=51de46f9128d899572989dc0deb0717d66fd93097e5feac91386e9db0901461d&"

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
        game.nightAttacks.clear()
        game.nightDeathCandidates.clear()
        game.nightEvents.clear()
        game.concealmentForcedQuietNight = false
        game.coupleSacrificeMap.clear()
        game.lastNightSummary = NightResolutionSummary()
        game.playerDatas.forEach { player ->
            (player.job as? Cabal)?.let { cabalJob ->
                cabalJob.moonMarkedSunTonight = false
                cabalJob.cabalSpecialWinReady = false
            }
            (player.job as? Police)?.let { policeJob ->
                policeJob.currentSearchTarget = null
                policeJob.hasUsedSearchThisNight = false
                policeJob.eavesdroppingTargetId = null
            }
            (player.job as? Detective)?.let {
                DetectiveAbility.resetNightState(player)
            }
            (player.job as? Administrator)?.let { administratorJob ->
                administratorJob.investigationResultPlayerId = null
            }
        }
        resolveCabalSunInvestigation(game)

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

        resolveDoctorHeals(game)
        resolveAdministratorInvestigations(game)

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
            player.state.resetForNextPhase()
        }

        return summary
    }

    fun resolveDawnPhase(game: Game, summary: NightResolutionSummary = game.lastNightSummary) {
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
            }
        }

        summary.deaths.forEach { victim ->
            if (victim.state.isDead) return@forEach
            victim.state.isDead = true
            game.nightEvents += GameEvent.PlayerDied(victim)
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

    private fun announceCoupleSacrificeReveal(game: Game, deaths: List<PlayerData>) {
        val mainChannel = game.mainChannel ?: return

        deaths.forEach { deadPlayer ->
            val originalTargetId = game.coupleSacrificeMap[deadPlayer.member.id] ?: return@forEach
            val originalTarget = game.getPlayer(originalTargetId) ?: return@forEach

            deadPlayer.state.isJobPubliclyRevealed = true
            originalTarget.state.isJobPubliclyRevealed = true

            val deadRole = (deadPlayer.job as? Couple)?.role.toDisplayName()
            val originalRole = (originalTarget.job as? Couple)?.role.toDisplayName()
            val deadJobName = deadPlayer.job?.name ?: "알 수 없음"
            val originalJobName = originalTarget.job?.name ?: "알 수 없음"

            runCatching {
                mainChannel.createMessage(
                    "연인의 희생이 발동했습니다. ${originalTarget.member.effectiveName}(${originalRole})의 대가로 ${deadPlayer.member.effectiveName}(${deadRole})가 대신 사망했습니다.\n" +
                        "직업 공개: ${originalTarget.member.effectiveName} - ${originalJobName}, ${deadPlayer.member.effectiveName} - ${deadJobName}"
                )
            }
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
        val mainChannel = game.mainChannel ?: return
        val mafiaChannel = game.mafiaChannel ?: return
        val coupleChannel = game.coupleChannel ?: return
        val deadChannel = game.deadChannel ?: return

        // 1. 게임 상태 및 날짜 변경
        game.currentPhase = GamePhase.DAY
        val dawnPresentation = summary.dawnPresentation ?: buildDefaultDawnPresentation(emptyList(), summary.deaths)

        game.sendMainChannelMessageWithImage(
            imageLink = dawnPresentation.imageUrl,
            message = dawnPresentation.message
        )
        delay(3_000L)

        game.sendMainChannelMessageWithImage(
            imageLink = "https://cdn.discordapp.com/attachments/1483977619258212392/1483981622096429247/7aace941ae58a6cc.png?ex=69bc9115&is=69bb3f95&hm=fc7255667bb001a0f730a3e42d5d729c8584db33095699bcb02fc4ea4295a613&",
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
        val voteCounts = mutableMapOf<PlayerData, Int>()
        var invalidVoteCount = 0
        val weightedVoteTargets = mutableListOf<PlayerData>()

        alivePlayers.forEach { voter ->
            val weightEvent = GameEvent.CalculateVoteWeight(voter, weight = 1)
            voter.allAbilities
                .filterIsInstance<PassiveAbility>()
                .sortedByDescending(PassiveAbility::priority)
                .forEach { passive ->
                    passive.onEventObserved(game, voter, weightEvent)
                }

            val targetIdString = game.currentMainVotes[voter.member.id]
            if (targetIdString == null) {
                invalidVoteCount += weightEvent.weight
                return@forEach
            }

            val target = game.getPlayer(Snowflake(targetIdString)) ?: return@forEach
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
        val prosCount = game.currentProsConsVotes.values.count { it }
        val consCount = game.currentProsConsVotes.values.count { !it }

        val executionEvent = GameEvent.DecideExecution(target, prosCount > consCount)
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
                imageLink = "https://cdn.discordapp.com/attachments/1483977619258212392/1484594233653465122/K5WjViOFIiajx3YUfctCF-wkTWwg-DnerBQ09EXEd5-Jxz6Yy0vAmAuM5XDOMIWqHpYOXk85dCobA6CkwzPxOILsPNTbKJgtpYa1DtnVqhceybFNoLK5kdEtPJr6x7rCpn5F3Au_wTeTK0zWtRNArQ.webp?ex=69becb9f&is=69bd7a1f&hm=95cc33354d29bf53d2a74db6ca5ac622b88ef11bfe5b9e419f6e7b38a6f2a8b4&",
                message = buildString {
                    executionEvent.overrideReason?.let { reason ->
                        appendLine(reason)
                    }
                    append("${target.member.effectiveName}님의 처형이 부결되었습니다.")
                }
            )
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
            return
        }

        target.state.isDead = true
        game.nightEvents += GameEvent.PlayerDied(target, isLynch = true)
        dispatchEvents(game)
        game.nightEvents.clear()
        game.sendMainChannelMessageWithImage(
            imageLink = "https://cdn.discordapp.com/attachments/1483977619258212392/1484594233288691895/22SIfKIG4sgmfsgKpScS00MYCCNg70dZoYW9wB3zjuIlnN7d56sqkmFViOFPYrPnPJixJ-BEj5f_mVUp2wcYAzYpHKjyZDuoQyzfp3efnGqc1UYKkMLrk0w5QxCV5tlorhBipi2-c69B7eSYhppyIA.webp?ex=69becb9f&is=69bd7a1f&hm=0b3d5473bbaebb91f2335ef3d07cf315043fde1889930328ea4211c486e792df&",
            message = "${target.member.effectiveName}님이 투표로 처형당하였습니다."
        )

        if (deadChannel != null) {
            updateDeadChannelPermissions(game, deadChannel)
        }
    }

    fun checkWinCondition(game: Game): Team? {
        if (isCabalSpecialWinReady(game)) {
            return Team.CABAL_SPECIAL
        }

        val alivePlayers = game.playerDatas.filter { !it.state.isDead }
        val mafiaCount = alivePlayers.count { it.job is Evil }
        val citizenCount = alivePlayers.size - mafiaCount
        val aliveCabals = alivePlayers.count { it.job is Cabal }

        return when {
            mafiaCount == 0 -> Team.CITIZEN
            mafiaCount >= citizenCount && aliveCabals < 2 -> Team.MAFIA
            else -> null
        }
    }

    suspend fun endGame(game: Game, winningTeam: Team) {
        game.isRunning = false
        game.currentPhase = GamePhase.END
        game.sendMainChannerMessage("${winningTeam.displayName} 승리: ${winningTeam.winMessage}")
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
                imageUrl = QUIET_NIGHT_IMAGE_URL,
                message = "조용하게 밤이 넘어갔습니다."
            )
        }

        val attacks = game.nightAttacks.values.toList()
        val presentationEvent = GameEvent.ResolveDawnPresentation(
            dayCount = game.dayCount,
            attacks = attacks,
            deaths = deaths,
            presentation = buildDefaultDawnPresentation(attacks, deaths)
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
        deaths: List<PlayerData>
    ): DawnPresentation {
        val mafiaKillVictim = attacks
            .firstOrNull { it.attacker.job?.name == "마피아" }
            ?.target
            ?.takeIf { it in deaths }

        return if (mafiaKillVictim == null) {
            DawnPresentation(
                imageUrl = QUIET_NIGHT_IMAGE_URL,
                message = "조용하게 밤이 넘어갔습니다."
            )
        } else {
            DawnPresentation(
                imageUrl = DEATH_NIGHT_IMAGE_URL,
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
                observers.forEach { (player, passives) ->
                    passives.forEach { passive ->
                        passive.onEventObserved(game, player, event)
                    }
                }
            }
        }

        return processedEvents
    }

    private fun resolveDoctorHeals(game: Game) {
        game.playerDatas.forEach { player ->
            val doctorJob = player.job as? Doctor ?: return@forEach
            val targetId = doctorJob.currentHealTarget ?: return@forEach
            val target = game.getPlayer(targetId) ?: run {
                doctorJob.currentHealTarget = null
                return@forEach
            }

            val healEvent = GameEvent.PlayerHealed(
                healer = player,
                target = target,
                defenseTier = DefenseTier.NORMAL
            )

            player.job?.abilities
                ?.filterIsInstance<PassiveAbility>()
                ?.forEach { passive ->
                    passive.onEventObserved(game, player, healEvent)
                }

            target.state.healTier = maxOf(target.state.healTier, healEvent.defenseTier)
            game.nightEvents += healEvent
            doctorJob.currentHealTarget = null
        }
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
