package org.beobma.mafia42discordproject.game

import kotlinx.coroutines.delay
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.DefenseTier
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.game.system.Team
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.evil.Evil

object GameLoopManager {

    suspend fun startNightPhase(game: Game) {
        // ==========================================
        // 1. 상태 전환 (능력 입력 활성화)
        // ==========================================
        game.currentPhase = GamePhase.NIGHT
        game.dayCount++

        // 디스코드 메인 채널에 밤이 되었음을 공지
        // DiscordMessageManager.sendToMainChannel("🌙 밤이 되었습니다. 모두 눈을 감아주세요.")


        // ==========================================
        // 2. 디스코드 채널 권한 제어 (비밀 대화방 오픈)
        // ==========================================
        val alivePlayers = game.playerDatas.filter { !it.state.isDead }

        // TODO: 메인 토론 채널의 권한을 수정하여 아무도 채팅을 칠 수 없게 만듦 (채널 락)
        // muteMainChannel()

        // 살아있는 마피아 팀원들을 찾아 마피아 전용 채널의 채팅 권한을 열어줌
        val mafiaTeam = alivePlayers.filter { it.job is Evil /* 접선한 스파이/짐승인간 포함 로직 필요 */ }
        // openMafiaChannel(mafiaTeam)


        // ==========================================
        // 3. 밤 능력 사용 가능자에게 UI 띄워주기
        // ==========================================
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


        // ==========================================
        // 4. 밤 전용 패시브 발동 (필요 시)
        // ==========================================
        for (player in alivePlayers) {
            val playerJob = player.job ?: continue
            playerJob.abilities.filterIsInstance<PassiveAbility>().forEach { passive ->
                passive.onPhaseChanged(game, player, GamePhase.NIGHT)
            }
        }

        // (이후 메인 루프에서 delay()로 밤 타이머가 흘러가고 resolveNightPhase가 호출됨)
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

        // 게임 페이즈 업데이트
        game.currentPhase = GamePhase.DAY
        // TODO: 디스코드 채널에 아침 브리핑 출력 및 토론 시간 타이머 시작
    }

    suspend fun startDayPhase(game: Game) {
        // 1. 페이즈 전환
        game.currentPhase = GamePhase.DAY

        // 2. 아침 브리핑 구성 (사망자 발표)
        val diedLastNight = game.playerDatas.filter { it.state.isDead /* && 어젯밤 죽었음 플래그 */ }
        // DiscordMessageManager.sendToMainChannel("아침이 밝았습니다. ...")

        // 3. 가장 중요한 디스코드 권한 제어
        // - 죽은 자: 메인 채널 권한 박탈 -> 무덤 채널 권한 부여
        // - 영매: 무덤 채널 읽기 권한 부여
        // - 생존자: 마담에게 유혹당하지 않은(isSilenced == false) 사람만 메인 채널 발언권 부여

        // 4. 낮 시작 패시브 발동 (예언자 등)
        game.playerDatas.filter { !it.state.isDead }.forEach { player ->
            val playerJob = player.job ?: return@forEach
            playerJob.abilities.filterIsInstance<PassiveAbility>().forEach { passive ->
                passive.onPhaseChanged(game, player, GamePhase.DAY)
            }
        }

        // 5. 낮 능력자(해커 등) UI 활성화

        // 6. 토론 시간 타이머 시작 (루프 복귀)
    }

    fun startVotePhase(game: Game) {}

    fun resolveVotePhase(game: Game) {}

    fun startDefensePhase(game: Game, target1: Unit) {}

    fun startProsConsVotePhase(game: Game, target1: Unit) {}

    fun resolveExecutionPhase(game: Game, target1: Unit) {}

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
        game.isRunnig = false // 상태 플래그 변경

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
        while (game.isRunnig) {

            startNightPhase(game)
            delay(25_000L) // 25초 밤 시간

            resolveNightPhase(game)
            val nightWinner = checkWinCondition(game)
            if (nightWinner != null) {
                endGame(game, nightWinner) // 승리 공지 및 게임 종료 처리
                break // 코루틴 루프 탈출
            }
            startDayPhase(game)
            val sec = game.playerDatas.count { !it.state.isDead }
            delay(sec * 15_000L) // 15초 * 살아있는 사람 수 낮 시간

            startVotePhase(game)
            delay(15_000L) // 15초 투표 시간

            val target = resolveVotePhase(game)
            if (target != null) {
                startDefensePhase(game, target)
                delay(15_000L) // 반론 시간

                startProsConsVotePhase(game, target)
                delay(10_000L) // 찬반 투표 시간

                resolveExecutionPhase(game, target)
                val voteWinner = checkWinCondition(game)
                if (voteWinner != null) {
                    endGame(game, voteWinner) // 승리 공지 및 게임 종료 처리
                    break // 코루틴 루프 탈출
                }
            }
        }
    }
}