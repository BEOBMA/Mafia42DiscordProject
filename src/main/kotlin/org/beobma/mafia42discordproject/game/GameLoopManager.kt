package org.beobma.mafia42discordproject.game

import dev.kord.common.entity.Overwrite
import dev.kord.common.entity.OverwriteType
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.behavior.channel.edit
import dev.kord.core.behavior.edit
import dev.kord.rest.builder.channel.addMemberOverwrite
import dev.kord.rest.builder.channel.addRoleOverwrite
import io.ktor.client.request.invoke
import io.ktor.http.invoke
import kotlinx.coroutines.delay
import org.beobma.mafia42discordproject.discord.DiscordMessageManager.sendMainChannerMessage
import org.beobma.mafia42discordproject.game.GameLoopManager.resolveDawnPhase
import org.beobma.mafia42discordproject.game.GameLoopManager.resolveNightPhase
import org.beobma.mafia42discordproject.game.GameLoopManager.startNightPhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.DefenseTier
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.game.system.Team
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.evil.Evil

object GameLoopManager {

    suspend fun startNightPhase(game: Game) {
        game.currentPhase = GamePhase.NIGHT
        game.dayCount++

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
        val deathMessage = if (diedLastNight.isEmpty()) "간밤에 아무 일도 일어나지 않았습니다."
        else "간밤에 **${diedLastNight.joinToString { it.member.effectiveName }}**님이 살해당했습니다."

        // game.sendMainChannerMessage(...) 같은 커스텀 함수가 있다면 그것을 사용하셔도 됩니다.
        game.sendMainChannerMessage("🌅 **${game.dayCount}번째 아침이 밝았습니다.**\n$deathMessage\n지금부터 토론을 시작합니다.")

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
            game.sendMainChannerMessage("NightPhase")
            delay(25_000L) // 25초 밤 시간

            resolveNightPhase(game)
            game.sendMainChannerMessage("resolveNightPhase")
//            val nightWinner = checkWinCondition(game)
//            if (nightWinner != null) {
//                endGame(game, nightWinner) // 승리 공지 및 게임 종료 처리
//                break // 코루틴 루프 탈출
//            }

            resolveDawnPhase(game)
            game.sendMainChannerMessage("resolveDawnPhase")
            delay(10_000L) // 낮 정산시간 10초 동안 채팅 못치게


            startDayPhase(game)
            game.sendMainChannerMessage("Day")
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