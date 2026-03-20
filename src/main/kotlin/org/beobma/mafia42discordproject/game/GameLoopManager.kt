package org.beobma.mafia42discordproject.game

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.channel.startPublicThread
import dev.kord.core.behavior.channel.edit
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.thread.TextChannelThread
import dev.kord.rest.builder.channel.addMemberOverwrite
import dev.kord.rest.builder.channel.addRoleOverwrite
import dev.kord.rest.builder.component.actionRow
import dev.kord.rest.builder.component.option
import kotlinx.coroutines.delay
import org.beobma.mafia42discordproject.discord.DiscordMessageManager.sendMainChannerImage
import org.beobma.mafia42discordproject.discord.DiscordMessageManager.sendMainChannerMessage
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.DefenseTier
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.game.system.Team
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.evil.Evil

object GameLoopManager {
    private const val NIGHT_DURATION_MS = 25_000L
    private const val DAWN_DURATION_MS = 10_000L
    private const val VOTE_DURATION_MS = 15_000L
    private const val DEFENSE_DURATION_MS = 15_000L
    private const val PROS_CONS_VOTE_DURATION_MS = 10_000L
    private var thread: TextChannelThread? = null
    private var timerMessage: Message? = null

    fun resetTimeThreadState() {
        thread = null
        timerMessage = null
    }

    private suspend fun runPhaseCountdown(game: Game, label: String, durationMillis: Long) {
        val mainChannel = game.mainChannel
        if (mainChannel == null) {
            delay(durationMillis)
            return
        }

        if (thread == null) {
            thread = mainChannel.startPublicThread("시간")
        }

        val targetUnixSeconds = (System.currentTimeMillis() + durationMillis) / 1_000L
        val thread = thread ?: return
        val timerMessage = "📌 단계: **$label**\n⏱️ 남은 시간: <t:$targetUnixSeconds:R>\n(유닉스 시간: `$targetUnixSeconds`)"

        if (this.timerMessage == null) {
            this.timerMessage = thread.createMessage(timerMessage)
        } else {
            this.timerMessage?.edit {
                content = timerMessage
            }
        }

        delay(durationMillis)
    }

    suspend fun startNightPhase(game: Game) {
        game.currentPhase = GamePhase.NIGHT
        game.dayCount++
        game.mainChannel?.edit {
            name = "${game.dayCount}일차 밤"
        }
        game.sendMainChannerImage("https://cdn.discordapp.com/attachments/1483977619258212392/1483978042673070342/43e6c3860a090af9.png?ex=69be8800&is=69bd3680&hm=1dabf5630544f8f8766c7abbb0793a48e3a11e1364a31d1e4e439fff70539e25&")
        game.sendMainChannerMessage("밤이 되었습니다.")


        // ==========================================
        // 2. 디스코드 채널 권한 제어 (비밀 대화방 오픈)
        // ==========================================
        val alivePlayers = game.playerDatas.filter { !it.state.isDead }
        val mainChannel = game.mainChannel ?: return
        val mafiaChannel = game.mafiaChannel ?: return

        // 음성 뮤트
        game.playerDatas.forEach { playerData ->
            playerData.member.edit {
                muted = true
            }
        }

        // 메인 채널 뮤트
        mainChannel.edit {
            addRoleOverwrite(game.guild.id) {
                denied = Permissions(Permission.SendMessages)
                allowed = Permissions(Permission.UseApplicationCommands)
            }
        }

        mafiaChannel.edit {
            addRoleOverwrite(game.guild.id) {
                allowed = Permissions(Permission.SendMessages)
                allowed = Permissions(Permission.UseApplicationCommands)
                denied = Permissions(Permission.ReadMessageHistory)
            }
        }

        for (player in alivePlayers) {
            val playerJob = player.job ?: continue
            // 현재 '밤'에 사용할 수 있는 액티브 능력이 있는지 검사
            val nightAbility = playerJob.abilities
                .filterIsInstance<ActiveAbility>()
                .firstOrNull { it.usablePhase == GamePhase.NIGHT }

            if (nightAbility != null && !player.state.isSilenced) { // 마담에게 유혹당하지 않았다면

                // 💡 생존해 있는 다른 플레이어들의 목록을 추출하여 Select Menu 옵션으로 생성
                val targetOptions = alivePlayers.filter { it != player } // (자신 제외, 직업에 따라 다름)

                // TODO: 해당 플레이어에게만 보이는 에페머럴 메시지로 능력 사용 UI 전송
                /*
                DiscordMessageManager.sendEphemeralSelectMenu(
                    userId = player.member.id,
                    text = "${nightAbility.name} 능력을 사용할 대상을 선택하세요.\n(설명: ${nightAbility.description})",
                    options = targetOptions
                )
                */
            }
        }


        for (player in alivePlayers) {
            val playerJob = player.job ?: continue
            playerJob.abilities.filterIsInstance<PassiveAbility>().forEach { passive ->
                passive.onPhaseChanged(game, player, GamePhase.NIGHT)
            }
        }
    }

    // 밤 시간이 끝나고 낮으로 넘어갈 때 호출되는 파이프라인
    fun resolveNightPhase(game: Game) {

        // 동시성 보장을 위해, 이번 밤에 최종적으로 사망할 플레이어들을 모아두는 Set
        val playersToDie = mutableSetOf<PlayerData>()

        // =========================================================
        // 1. 보조 및 조사 능력 결산
        // =========================================================
        // (의사의 힐은 디스코드 명령어 입력 시점에 이미 state.healTier에 적용되었다고 가정)
        // 예: 경찰의 조사 결과를 판정하고 이벤트로 발행
        // if (경찰 조사 성공) {
        //     game.nightEvents.add(GameEvent.JobDiscovered(경찰, 타겟, 타겟직업))
        // }


        // =========================================================
        // 2. 방어랑 공격 티어 비교함
        // =========================================================
        for ((attackKey, attackEvent) in game.nightAttacks) {
            val target = attackEvent.target
            val attackTier = attackEvent.attackTier

            // 특수 룰: 군인의 방탄 소모 로직
            // 군인이고 방탄이 남아있다면, 이번 판정에 한해 방어 티어를 ABSOLUTE(절대 방어)로 끌어올림
            if (target.job?.name == "군인" && !target.state.hasUsedOneTimeAbility) {
                target.state.healTier = DefenseTier.ABSOLUTE

                // 만약 이 공격을 막아낼 수 있는 수준이라면 방탄을 1회 소모함
                if (target.state.healTier.level >= attackTier.level) {
                    target.state.hasUsedOneTimeAbility = true
                }
            }

            // 타겟의 방어 레벨이 공격 레벨보다 같거나 높으면 생존
            if (target.state.healTier.level >= attackTier.level) {
                // 방어 성공 로직
            } else {
                // 방어 실패 -> 죽을놈 리스트에 추가
                playersToDie.add(target)
            }
        }


        // =========================================================
        // 3. 사망 처리
        // =========================================================
        playersToDie.forEach { victim ->
            victim.state.isDead = true

            // 사망 사실을 시스템 이벤트로 발행 (도굴꾼, 영매 등이 주워갈 수 있도록)
            game.nightEvents.add(GameEvent.PlayerDied(victim))

            // TODO: 디스코드 메시지 큐에 "밤 사이 [victim]님이 살해당했습니다." 추가
        }

        // =========================================================
        // 4. 사후/패시브 이벤트 연쇄 분배
        // =========================================================
        val alivePlayers = game.playerDatas.filter { !it.state.isDead }

        // 큐가 완전히 빌 때까지 계속 반복해서 분배 (이벤트가 이벤트를 낳는 경우 처리)
        while (game.nightEvents.isNotEmpty()) {
            // 현재 큐에 있는 이벤트들만 복사해서 꺼냄 (ConcurrentModificationException 방지)
            val eventsToProcess = game.nightEvents.toList()
            game.nightEvents.clear()

            for (event in eventsToProcess) {
                for (player in alivePlayers) {
                    val playerJob = player.job ?: continue
                    playerJob.abilities.filterIsInstance<PassiveAbility>().forEach { passive ->
                        passive.onEventObserved(game, player, event)
                        // 만약 이 과정에서 새로운 GameEvent가 add 된다면 다음 루프에서 처리
                    }
                }
            }
        }


        // =========================================================
        // 5. 다음날 준비
        // =========================================================
        // 큐 비우기
        game.nightAttacks.clear()
        game.nightEvents.clear()

        // 살아남은 플레이어들의 일회성 상태(힐, 유혹 등) 초기화
        for (player in game.playerDatas) {
            player.state.resetForNextPhase()
        }
    }

    fun resolveDawnPhase(game: Game) {
        val alivePlayers = game.playerDatas.filter { !it.state.isDead }

        // 생존자들의 모든 패시브 능력을 수집하여 우선순위(priority) 내림차순으로 정렬
        val sortedPassives = alivePlayers.flatMap { player ->
            player.allAbilities.filterIsInstance<PassiveAbility>().map { player to it }
        }.sortedByDescending { it.second.priority }

        // 1. 이벤트 연쇄 처리 (while 루프)
        while (game.nightEvents.isNotEmpty()) {
            val eventsToProcess = game.nightEvents.toList()
            game.nightEvents.clear()

            for (event in eventsToProcess) {
                // 정렬된 순서대로(수습 -> 도굴 -> 기타) 이벤트를 던져줌
                for ((player, passive) in sortedPassives) {
                    passive.onEventObserved(game, player, event)
                }
            }
        }

        // 2. 공무원 / 기자 등의 아침 공지용 텍스트 수집 (선택 사항)
        // 새벽에 발동된 특종, 조회 결과 등을 여기서 모아두었다가 startDayPhase에 넘겨줄 수 있습니다.
    }

    suspend fun startDayPhase(game: Game, diedLastNight: List<PlayerData> = emptyList()) {
        val mainChannel = game.mainChannel ?: return

        // 1. 게임 상태 및 날짜 변경
        game.currentPhase = GamePhase.DAY

        // 2. 아침 브리핑 공지
        val alivePlayers = game.playerDatas.filter { !it.state.isDead }
        val deathMessage = if (diedLastNight.isEmpty()) "조용하게 밤이 넘어갔습니다."
        else "**${diedLastNight.joinToString { it.member.effectiveName }}**이(가) 살해당하였습니다."

        if (diedLastNight.isEmpty()) {
            game.sendMainChannerImage("https://cdn.discordapp.com/attachments/1483977619258212392/1483980003015397446/d8692f78c3528f76.png?ex=69bc8f93&is=69bb3e13&hm=1378e1b6daba26baddf0cc5d042087b7c5151860d709a3140414b97f774b77a4&")
        }
        else {
            game.sendMainChannerImage("https://cdn.discordapp.com/attachments/1483977619258212392/1483980246448603146/99cb963d1b44dc2e.png?ex=69bc8fcd&is=69bb3e4d&hm=51de46f9128d899572989dc0deb0717d66fd93097e5feac91386e9db0901461d&")
        }
        game.sendMainChannerMessage(deathMessage)
        delay(3_000L) // 3초 딜레이

        game.mainChannel?.edit {
            name = "${game.dayCount}일차 낮"
        }
        game.sendMainChannerImage("https://cdn.discordapp.com/attachments/1483977619258212392/1483981622096429247/7aace941ae58a6cc.png?ex=69bc9115&is=69bb3f95&hm=fc7255667bb001a0f730a3e42d5d729c8584db33095699bcb02fc4ea4295a613&")
        game.sendMainChannerMessage("날이 밝았습니다.")

        // ==========================================
        // 3. 🎯 텍스트 및 음성 채널 권한 완벽 동기화
        // ==========================================

        // 3-1. 메인 텍스트 채널 전체 잠금 해제 (밤에 막았던 @everyone 권한 원상 복구)
        mainChannel.edit {
            addRoleOverwrite(game.guild.id) {
                allowed = Permissions(Permission.SendMessages)
                denied = Permissions() // 금지 내역 초기화
            }

            // 3-2. 플레이어 개별 예외 처리 (텍스트 채널 개별 덮어쓰기)
            for (player in game.playerDatas) {
                val isSilencedOrDead = player.state.isDead || player.state.isSilenced

                if (isSilencedOrDead) {
                    // 사망자이거나 마담에게 유혹당한 경우: 채팅 치기 강제 금지
                    addMemberOverwrite(player.member.id) {
                        denied = Permissions(Permission.SendMessages)
                    }
                } else {
                    // 정상 생존자: 개별 금지 내역이 혹시 남아있다면 초기화
                    addMemberOverwrite(player.member.id) {
                        denied = Permissions()
                    }
                }
            }
        }

        // 3-3. 디스코드 서버 마이크(음성) 권한 개별 동기화
        for (player in game.playerDatas) {
            val shouldMute = player.state.isDead || player.state.isSilenced

            // 서버 마이크 뮤트 해제 및 설정 (API 호출)
            runCatching {
                player.member.edit {
                    muted = shouldMute
                }
            }

            // 마담에게 유혹당한 생존자에게만 조용히 DM 안내
            if (player.state.isSilenced && !player.state.isDead) {
                runCatching {
                    player.member.getDmChannel().createMessage("💋 마담에게 유혹당해 오늘 하루 텍스트 및 음성 발언이 금지됩니다.")
                }
            }
        }

        // ==========================================
        // 4. 낮 전용 UI 및 패시브 등 후속 처리
        // ==========================================
        // ...
    }

    suspend fun startVotePhase(game: Game) {
        val mainChannel = game.mainChannel ?: return

        game.currentPhase = GamePhase.VOTE
        game.currentMainVotes.clear() // 투표 데이터 초기화

        val alivePlayers = game.playerDatas.filter { !it.state.isDead }

        mainChannel.createMessage {
            content = "🗳️ **투표 시간이 되었습니다.** 처형할 의심자를 선택해주세요.\n*(시간 내에 선택하지 않으면 무효표 처리됩니다.)*"

            actionRow {
                stringSelect("main_vote_select") {
                    placeholder = "처형할 플레이어 선택"

                    // 💡 기권 없이 오직 생존자 목록만 옵션으로 제공
                    for (player in alivePlayers) {
                        option(player.member.effectiveName, player.member.id.toString()) {
                            description = "이 플레이어에게 투표합니다."
                        }
                    }
                }
            }
        }
    }
    suspend fun resolveVotePhase(game: Game): PlayerData? {
        val mainChannel = game.mainChannel ?: return null

        val alivePlayers = game.playerDatas.filter { !it.state.isDead }
        val voteCounts = mutableMapOf<PlayerData, Int>()
        var invalidVoteCount = 0

        // ==========================================
        // 1. 전체 생존자를 순회하며 투표 검사 및 가중치 적용
        // ==========================================
        for (voter in alivePlayers) {

            // 💡 투표 가중치 판정 이벤트 (기본 1표, 정치인 개입 시 3표 등)
            val weightEvent = GameEvent.CalculateVoteWeight(voter, weight = 1)
            voter.allAbilities.filterIsInstance<PassiveAbility>().forEach { passive ->
                passive.onEventObserved(game, voter, weightEvent)
            }

            // 이 유저가 투표한 대상 확인
            val targetIdString = game.currentMainVotes[voter.member.id]

            if (targetIdString == null) {
                // 💡 시간 내에 투표하지 않은 경우 -> 무효표(invalid)에 가중치만큼 누적
                invalidVoteCount += weightEvent.weight
            } else {
                // 💡 누군가에게 투표한 경우 -> 해당 타겟의 득표수에 가중치만큼 누적
                val targetId = Snowflake(targetIdString)
                val target = game.getPlayer(targetId)

                if (target != null) {
                    voteCounts[target] = (voteCounts[target] ?: 0) + weightEvent.weight
                }
            }
        }

        // ==========================================
        // 2. 최다 득표자 도출 및 예외 판정 (무효표 다수, 동률)
        // ==========================================
        val maxVotes = voteCounts.values.maxOrNull() ?: 0

        // 예외 1: 무효표가 누군가가 받은 최다 표와 같거나 더 많을 때 (또는 아무도 표를 못 받았을 때)
        if (invalidVoteCount >= maxVotes || maxVotes == 0) {
            mainChannel.createMessage("⚖️ 무효표가 가장 많거나 같아, 오늘 투표는 **부결**되었습니다.")
            return null
        }

        // 예외 2: 최다 득표자가 여러 명(동률)일 때
        val maxVotedPlayers = voteCounts.filter { it.value == maxVotes }.keys.toList()

        if (maxVotedPlayers.size > 1) {
            mainChannel.createMessage("⚖️ 최다 득표자가 여러 명(동률)이므로, 오늘 투표는 **부결**되었습니다.")
            return null
        }

        // ==========================================
        // 3. 정상 도출
        // ==========================================
        val finalTarget = maxVotedPlayers.first()
        mainChannel.createMessage("⚖️ 투표 결과, **${finalTarget.member.effectiveName}**님이 최다 득표(${maxVotes}표)로 선정되었습니다.")

        return finalTarget
    }
    suspend fun startDefensePhase(game: Game, target: PlayerData) {
        val mainChannel = game.mainChannel ?: return

        mainChannel.createMessage("⚖️ **${target.member.effectiveName}**님이 최다 득표자로 선정되었습니다. 15초 동안 최후의 반론을 시작합니다.")

        // 1. 서버 전체(@everyone) 발언권 박탈
        mainChannel.edit {
            addRoleOverwrite(game.guild.id) {
                denied = Permissions(Permission.SendMessages)
            }

            // 2. 오직 타겟(최다 득표자)에게만 발언권 임시 부여
            // (단, 타겟이 마담에게 유혹당한 상태라면 반론도 할 수 없음!)
            if (!target.state.isSilenced) {
                addMemberOverwrite(target.member.id) {
                    allowed = Permissions(Permission.SendMessages)
                }
            }
        }

        // (타이머 대기 후 찬반 투표로 이동)
    }
    suspend fun startProsConsVotePhase(game: Game, target: PlayerData) {
        val mainChannel = game.mainChannel ?: return

        // 찬반 투표 데이터 초기화
        game.currentPhase = GamePhase.VOTE
        game.currentProsConsVotes.clear()

        mainChannel.createMessage {
            content = "⚖️ 최후의 반론이 종료되었습니다.\n**${target.member.effectiveName}**님을 처형하시겠습니까? (10초)\n*(미투표 시 반대(부결)로 처리됩니다)*"

            // 디스코드 Kord 버튼 UI 추가
            actionRow {
                interactionButton(ButtonStyle.Success, "vote_pros") {
                    label = "찬성"
                    // emoji = DiscordPartialEmoji(name = "👍") // (선택) 이모지 추가
                }
                interactionButton(ButtonStyle.Danger, "vote_cons") {
                    label = "반대"
                    // emoji = DiscordPartialEmoji(name = "👎") // (선택) 이모지 추가
                }
            }
        }
    }
    suspend fun resolveExecutionPhase(game: Game, target: PlayerData) {
        val mainChannel = game.mainChannel ?: return

        // 1. 단순 과반수 합산 (true=찬성, false=반대)
        val prosCount = game.currentProsConsVotes.values.count { it == true }
        val consCount = game.currentProsConsVotes.values.count { it == false }
        var isApproved = prosCount > consCount // 동률이면 부결 처리

        // 2. 판사 등 찬반 결과 강제 조작 직업의 이벤트 개입
        val executionEvent = GameEvent.DecideExecution(target, isApproved)
        val alivePlayers = game.playerDatas.filter { !it.state.isDead }

        for (player in alivePlayers) {
            player.allAbilities.filterIsInstance<PassiveAbility>().forEach { passive ->
                passive.onEventObserved(game, player, executionEvent)
            }
        }

        // 판사 등 누군가가 결과를 조작했다면 사유 출력
        if (executionEvent.overrideReason != null) {
            game.sendMainChannerMessage("🚨 ${executionEvent.overrideReason}")
        }

        // 3. 최종 판정
        if (executionEvent.isApproved) {
            // 처형이 확정된 시점! -> 여기서 정치인의 '처세' 등 처형 회피 이벤트가 한 번 더 발생합니다.
            val voteExecutionEvent = GameEvent.VoteExecution(target)
            for (player in alivePlayers) {
                player.allAbilities.filterIsInstance<PassiveAbility>().forEach { passive ->
                    passive.onEventObserved(game, player, voteExecutionEvent)
                }
            }

            if (voteExecutionEvent.isCancelled) {
                // 정치인 처세 발동 등으로 처형 무효화
                game.sendMainChannerMessage("🛡️ ${voteExecutionEvent.cancelReason}")
            } else {
                // 진짜 최종 사망 처리
                target.state.isDead = true
                game.sendMainChannerMessage("💀 투표 결과 (찬성 $prosCount : 반대 $consCount)로 **${target.member.effectiveName}**님이 처형되었습니다.")

                // 유품을 위한 처형 사망 이벤트 기록
                game.nightEvents.add(GameEvent.PlayerDied(target, isLynch = true))
            }
        } else {
            game.sendMainChannerMessage("🕊️ 투표 결과 (찬성 $prosCount : 반대 $consCount)로 처형이 **부결**되었습니다.")
        }
    }
    fun checkWinCondition(game: Game): Team? {
        val alivePlayers = game.playerDatas.filter { !it.state.isDead }
        val mafiaCount = alivePlayers.count { it.job is Evil }
        val citizenCount = alivePlayers.size - mafiaCount // 교주팀 일단 제외

        return when {
            mafiaCount == 0 -> Team.CITIZEN
            mafiaCount >= citizenCount -> Team.MAFIA
            // 특수승리 알아서 만들어
            else -> null
        }
    }
    
    suspend fun endGame(game: Game, winningTeam: Team) {
        game.isRunning = false // 상태 플래그 변경

        // 디스코드 Embed 메시지로 결과 발표
        /*
        channel.createEmbed {
            title = "게임 종료! ${winningTeam.displayName}의 승리입니다!"
            color = winningTeam.embedColor
            description = winningTeam.winMessage

            // 참가자들의 직업 공개 로직 (선택 사항)
            val playerList = game.playerDatas.joinToString("\n") {
                "- ${it.member.username}: ${it.job?.name}"
            }
            field {
                name = "참가자 명단"
                value = playerList
            }
        }
        */

        // TODO: 봇의 메모리에서 현재 game 인스턴스 삭제 및 음성 채널 권한 원상 복구
    }

    // 코루틴 루프 예시 (GameManager.kt)
    suspend fun runGameLoop(game: Game) {
        while (game.isRunning) {

            startNightPhase(game)
            runPhaseCountdown(game, "${game.dayCount}일차 밤", NIGHT_DURATION_MS)

            resolveNightPhase(game)
//            val nightWinner = checkWinCondition(game)
//            if (nightWinner != null) {
//                endGame(game, nightWinner) // 승리 공지 및 게임 종료 처리
//                break // 코루틴 루프 탈출
//            }

            resolveDawnPhase(game)
            runPhaseCountdown(game, "${game.dayCount}일차 낮 정산", DAWN_DURATION_MS)


            startDayPhase(game)
            val sec = game.playerDatas.count { !it.state.isDead }
            runPhaseCountdown(game, "${game.dayCount}일차 낮", sec * 15_000L)

            startVotePhase(game)
            runPhaseCountdown(game, "${game.dayCount}일차 투표", VOTE_DURATION_MS)

            val target: PlayerData? = resolveVotePhase(game)
            if (target != null) {
                startDefensePhase(game, target)
                runPhaseCountdown(game, "${game.dayCount}일차 최후의 반론", DEFENSE_DURATION_MS)

                startProsConsVotePhase(game, target)
                runPhaseCountdown(game, "${game.dayCount}일차 찬반 투표", PROS_CONS_VOTE_DURATION_MS)

                resolveExecutionPhase(game, target)
//                val voteWinner = checkWinCondition(game)
//                if (voteWinner != null) {
//                    endGame(game, voteWinner) // 승리 공지 및 게임 종료 처리
//                    break // 코루틴 루프 탈출
//
//                }
            }
        }
    }
}
