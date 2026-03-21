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
import org.beobma.mafia42discordproject.discord.DiscordMessageManager.sendMainChannelMessageWithImage
import org.beobma.mafia42discordproject.discord.DiscordMessageManager.sendMainChannerMessage
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.*
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.administrator.AdministratorInvestigationPolicy
import org.beobma.mafia42discordproject.job.ability.general.definition.list.police.Warrant
import org.beobma.mafia42discordproject.job.definition.list.Administrator
import org.beobma.mafia42discordproject.job.definition.list.Doctor
import org.beobma.mafia42discordproject.job.definition.list.Police
import org.beobma.mafia42discordproject.job.evil.Evil
import org.beobma.mafia42discordproject.job.evil.list.Mafia

object GameLoopManager {
    private const val NIGHT_DURATION_MS = 25_000L
    private const val DAWN_DURATION_MS = 10_000L
    private const val VOTE_DURATION_MS = 15_000L
    private const val DEFENSE_DURATION_MS = 15_000L
    private const val PROS_CONS_VOTE_DURATION_MS = 10_000L
    private const val QUIET_NIGHT_IMAGE_URL =
        "https://cdn.discordapp.com/attachments/1483977619258212392/1483980003015397446/d8692f78c3528f76.png?ex=69bc8f93&is=69bb3e13&hm=1378e1b6daba26baddf0cc5d042087b7c5151860d709a3140414b97f774b77a4&"
    private const val DEATH_NIGHT_IMAGE_URL =
        "https://cdn.discordapp.com/attachments/1483977619258212392/1483980246448603146/99cb963d1b44dc2e.png?ex=69bc8fcd&is=69bb3e4d&hm=51de46f9128d899572989dc0deb0717d66fd93097e5feac91386e9db0901461d&"

    private const val TIME_THREAD_NAME = "시간"

    private var timeThreadChannel: ThreadChannel? = null
    private var timeStatusMessage: Message? = null

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
        game.lastNightSummary = NightResolutionSummary()
        game.playerDatas.forEach { player ->
            (player.job as? Police)?.let { policeJob ->
                policeJob.currentSearchTarget = null
                policeJob.hasUsedSearchThisNight = false
            }
            (player.job as? Administrator)?.let { administratorJob ->
                administratorJob.investigationResultPlayerId = null
            }
        }

        game.sendMainChannelMessageWithImage(
            imageLink = "https://cdn.discordapp.com/attachments/1483977619258212392/1483978042673070342/43e6c3860a090af9.png?ex=69be8800&is=69bd3680&hm=1dabf5630544f8f8766c7abbb0793a48e3a11e1364a31d1e4e439fff70539e25&",
            message = "밤이 되었습니다."
        )

        val mainChannel = game.mainChannel ?: return
        val mafiaChannel = game.mafiaChannel ?: return
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

            applyInnateNightDefense(target, attackEvent)

            if (target.state.healTier.level >= attackEvent.attackTier.level) {
                blockedAttacks += attackEvent
                playersToDie.remove(target)
            } else {
                playersToDie += target
            }
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
        summary.deaths.forEach { victim ->
            if (victim.state.isDead) return@forEach
            victim.state.isDead = true
            game.nightEvents += GameEvent.PlayerDied(victim)
        }

        val processedDawnEvents = dispatchEvents(game)
        val dawnPresentation = buildDawnPresentation(game, summary.deaths)

        game.lastNightSummary = summary.copy(
            processedEvents = summary.processedEvents + processedDawnEvents,
            dawnPresentation = dawnPresentation
        )

        game.nightEvents.clear()
    }

    suspend fun startDayPhase(
        game: Game,
        summary: NightResolutionSummary = game.lastNightSummary
    ) {
        val mainChannel = game.mainChannel ?: return
        val mafiaChannel = game.mafiaChannel ?: return
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
    }

    suspend fun resolveVotePhase(game: Game): PlayerData? {
        val alivePlayers = game.playerDatas.filter { !it.state.isDead }
        val voteCounts = mutableMapOf<PlayerData, Int>()
        var invalidVoteCount = 0

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
        val alivePlayers = game.playerDatas.filter { !it.state.isDead }
        val mafiaCount = alivePlayers.count { it.job is Evil }
        val citizenCount = alivePlayers.size - mafiaCount

        return when {
            mafiaCount == 0 -> Team.CITIZEN
            mafiaCount >= citizenCount -> Team.MAFIA
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
//            checkWinCondition(game)?.let { winner ->
//                endGame(game, winner)
//                break
//            }

            resolveDawnPhase(game, nightSummary)
            runPhaseCountdown(game, "새벽", DAWN_DURATION_MS)

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
//                checkWinCondition(game)?.let { winner ->
//                    endGame(game, winner)
//                    break
//                }
            }
        }
    }

    private fun applyInnateNightDefense(target: PlayerData, attackEvent: AttackEvent) {
        if (target.job?.name != "군인") return
        if (target.state.hasUsedOneTimeAbility) return

        target.state.healTier = maxOf(target.state.healTier, DefenseTier.ABSOLUTE)
        if (target.state.healTier.level >= attackEvent.attackTier.level) {
            target.state.hasUsedOneTimeAbility = true
        }
    }

    private fun buildDawnPresentation(game: Game, deaths: List<PlayerData>): DawnPresentation {
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
}
