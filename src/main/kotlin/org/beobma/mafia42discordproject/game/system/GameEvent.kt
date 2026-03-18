package org.beobma.mafia42discordproject.game.system

import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.job.Job

// 게임 내에서 발생하는 주요 사건들
sealed class GameEvent {
    // 💡 누군가 직업을 알아냈을 때 발행되는 이벤트
    data class JobDiscovered(
        val discoverer: PlayerData, // 알아낸 사람
        val target: PlayerData,     // 조사 대상
        val discoveredJob: Job      // 알아낸 직업 정보
    ) : GameEvent()

    // 누군가 사망했을 때 (영매, 도굴꾼 등이 활용 가능)
    data class PlayerDied(val victim: PlayerData) : GameEvent()
}