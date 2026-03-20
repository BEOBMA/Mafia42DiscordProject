package org.beobma.mafia42discordproject.game.system

import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.job.Job

// 게임 내에서 발생하는 주요 사건들
sealed class GameEvent {
    data class PlayerHealed(
        val healer: PlayerData,
        val target: PlayerData,
        var defenseTier: DefenseTier
    ) : GameEvent()

    // 💡 누군가 직업을 알아냈을 때 발행되는 이벤트
    data class JobDiscovered(
        val discoverer: PlayerData, // 알아낸 사람
        val target: PlayerData,     // 조사 대상
        val discoveredJob: Job      // 알아낸 직업 정보
    ) : GameEvent()

    // 뒤지면 발생되는 이벤트
    data class PlayerDied(
        val victim: PlayerData,
        val isLynch: Boolean = false,
        var isCleanedUp: Boolean = false // 💡 마피아 수습 여부 (기본값 false)
    ) : GameEvent()

    // 정치는 weight 바꿔
    data class CalculateVoteWeight(
        val voter: PlayerData,
        var weight: Int = 1 // 기본값 1표 (가변)
    ) : GameEvent()

    // 찬반 결과 개입
    data class DecideExecution(
        val target: PlayerData,
        var isApproved: Boolean, // 현재까지의 찬성/반대 결과 (가변)
        var overrideReason: String? = null // 판사 개입 등 UI 출력용 사유
    ) : GameEvent()

    // 투표로 뒤질때 나옴
    data class VoteExecution(
        val target: PlayerData,
        var isCancelled: Boolean = false,  // 이 판결이 무효화되었는가? (가변)
        var cancelReason: String? = null   // "정치인의 권력으로 무효화되었습니다" 등 사유
    ) : GameEvent()
}