package org.beobma.mafia42discordproject.game

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.channel.edit
import dev.kord.core.behavior.edit
import dev.kord.core.entity.channel.TextChannel
import dev.kord.rest.builder.channel.addMemberOverwrite
import dev.kord.rest.builder.channel.addRoleOverwrite
import dev.kord.rest.builder.component.actionRow
import dev.kord.rest.builder.component.option
import kotlinx.coroutines.delay
import org.beobma.mafia42discordproject.discord.DiscordMessageManager.sendMainChannelMessageWithImage
import org.beobma.mafia42discordproject.discord.DiscordMessageManager.sendMainChannerMessage
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.AttackEvent
import org.beobma.mafia42discordproject.game.system.DefenseTier
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.game.system.Team
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.evil.Evil

object GameLoopManager {
    private const val NIGHT_DURATION_MS = 25_000L
    private const val DAWN_DURATION_MS = 10_000L
    private const val VOTE_DURATION_MS = 15_000L
    private const val DEFENSE_DURATION_MS = 15_000L
    private const val PROS_CONS_VOTE_DURATION_MS = 10_000L
    private const val TIMER_TICK_MS = 1_000L
    private const val QUIET_NIGHT_IMAGE_URL =
        "https://cdn.discordapp.com/attachments/1483977619258212392/1483980003015397446/d8692f78c3528f76.png?ex=69bc8f93&is=69bb3e13&hm=1378e1b6daba26baddf0cc5d042087b7c5151860d709a3140414b97f774b77a4&"
    private const val DEATH_NIGHT_IMAGE_URL =
        "https://cdn.discordapp.com/attachments/1483977619258212392/1483980246448603146/99cb963d1b44dc2e.png?ex=69bc8fcd&is=69bb3e4d&hm=51de46f9128d899572989dc0deb0717d66fd93097e5feac91386e9db0901461d&"

    fun resetTimeThreadState() = Unit

    private suspend fun runPhaseCountdown(game: Game, label: String, durationMillis: Long) {
        var remainingMillis = durationMillis
        while (remainingMillis > 0) {
            delay(TIMER_TICK_MS)
            remainingMillis = (remainingMillis - TIMER_TICK_MS).coerceAtLeast(0L)
        }
    }

    suspend fun startNightPhase(game: Game) {
        game.currentPhase = GamePhase.NIGHT
        game.dayCount += 1
        game.nightAttacks.clear()
        game.nightDeathCandidates.clear()
        game.nightEvents.clear()
        game.lastNightSummary = NightResolutionSummary()

        game.sendMainChannelMessageWithImage(
            imageLink = "https://cdn.discordapp.com/attachments/1483977619258212392/1483978042673070342/43e6c3860a090af9.png?ex=69be8800&is=69bd3680&hm=1dabf5630544f8f8766c7abbb0793a48e3a11e1364a31d1e4e439fff70539e25&",
            message = "밤이 되었습니다."
        )

        val mainChannel = game.mainChannel ?: return
        val mafiaChannel = game.mafiaChannel ?: return
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

    fun resolveNightPhase(game: Game): NightResolutionSummary {
        val blockedAttacks = mutableListOf<AttackEvent>()
        val playersToDie = linkedSetOf<PlayerData>().apply {
            addAll(game.nightDeathCandidates)
        }

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
            if (victim.state.isDead) return@forEach
            victim.state.isDead = true
            game.nightEvents += GameEvent.PlayerDied(victim)
        }

        val processedEvents = dispatchEvents(game)
        val deaths = playersToDie.toList()
        val summary = NightResolutionSummary(
            processedEvents = processedEvents,
            deaths = deaths,
            blockedAttacks = blockedAttacks.toList(),
            dawnPresentation = buildDawnPresentation(game, deaths)
        )
        game.lastNightSummary = summary

        game.nightAttacks.clear()
        game.nightDeathCandidates.clear()
        game.nightEvents.clear()
        game.playerDatas.forEach { player ->
            player.state.resetForNextPhase()
        }

        return summary
    }

    fun resolveDawnPhase(game: Game, summary: NightResolutionSummary = game.lastNightSummary) {
        game.lastNightSummary = summary
    }

    suspend fun startDayPhase(
        game: Game,
        summary: NightResolutionSummary = game.lastNightSummary
    ) {
        val mainChannel = game.mainChannel ?: return
        val mafiaChannel = game.mafiaChannel ?: return

        // 1. 게임 상태 및 날짜 변경
        game.currentPhase = GamePhase.DAY
        val dawnPresentation = summary.dawnPresentation ?: buildDefaultDawnPresentation(summary.deaths)

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

    suspend fun startVotePhase(game: Game) {
        val mainChannel = game.mainChannel ?: return
        game.currentPhase = GamePhase.VOTE
        game.currentMainVotes.clear()

        val alivePlayers = game.playerDatas.filter { !it.state.isDead }

        mainChannel.createMessage {
            content = "투표 시간입니다. 의심되는 사람을 투표하세요."
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
            runPhaseCountdown(game, "Night ${game.dayCount}", NIGHT_DURATION_MS)

            val nightSummary = resolveNightPhase(game)
            checkWinCondition(game)?.let { winner ->
                endGame(game, winner)
                break
            }

            resolveDawnPhase(game, nightSummary)
            runPhaseCountdown(game, "Dawn ${game.dayCount}", DAWN_DURATION_MS)

            startDayPhase(game, nightSummary)
            val discussionMillis = game.playerDatas.count { !it.state.isDead } * 15_000L
            runPhaseCountdown(game, "Day ${game.dayCount}", discussionMillis.toLong())

            startVotePhase(game)
            runPhaseCountdown(game, "Vote ${game.dayCount}", VOTE_DURATION_MS)

            val target = resolveVotePhase(game)
            if (target != null) {
                startDefensePhase(game, target)
                runPhaseCountdown(game, "Defense ${game.dayCount}", DEFENSE_DURATION_MS)

                startProsConsVotePhase(game, target)
                runPhaseCountdown(game, "Execution Vote ${game.dayCount}", PROS_CONS_VOTE_DURATION_MS)

                resolveExecutionPhase(game, target)
                checkWinCondition(game)?.let { winner ->
                    endGame(game, winner)
                    break
                }
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
        val presentationEvent = GameEvent.ResolveDawnPresentation(
            dayCount = game.dayCount,
            attacks = game.nightAttacks.values.toList(),
            deaths = deaths,
            presentation = buildDefaultDawnPresentation(deaths)
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

    private fun buildDefaultDawnPresentation(deaths: List<PlayerData>): DawnPresentation {
        return if (deaths.isEmpty()) {
            DawnPresentation(
                imageUrl = QUIET_NIGHT_IMAGE_URL,
                message = "조용하게 밤이 지나갔습니다."
            )
        } else {
            DawnPresentation(
                imageUrl = DEATH_NIGHT_IMAGE_URL,
                message = "밤 사이 사망자: ${deaths.joinToString { it.member.effectiveName }}"
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
}
