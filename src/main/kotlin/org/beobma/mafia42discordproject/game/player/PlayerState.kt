package org.beobma.mafia42discordproject.game.player

import org.beobma.mafia42discordproject.game.system.DefenseTier

class PlayerState {
    // 영구적/누적 상태 (게임 끝날 때까지 유지)
    var isDead: Boolean = false
    var hasUsedOneTimeAbility: Boolean = false // 자경단원, 군인, 성직자 등의 1회용 소모 여부
    var hasUsedDailyAbility: Boolean = false   // 파파라치 등 하루 1회 소모 여부
    var isTamed: Boolean = false               // 짐승인간 길들여짐 여부
    var isJobPubliclyRevealed: Boolean = false // 모든 플레이어에게 직업이 공개되었는지 여부
    var hasAnnouncedGodfatherContact: Boolean = false // 대부 접선 알림 전송 여부

    // 일시적 상태 (밤/낮이 바뀔 때 초기화 필요)
    var healTier: DefenseTier = DefenseTier.NONE // 현재 받고 있는 힐의 방어 티어
    var isSilenced: Boolean = false              // 마담 유혹 여부
    var isThreatened: Boolean = false            // 건달 협박 여부
    var isShamaned: Boolean = false              // 성불 여부
    var isPoisoned: Boolean = false              // 중독 여부
    var poisonedDeathDay: Int? = null            // 해당 일차 새벽에 중독사 처리

    // 새로운 페이즈가 시작될 때 일회성 상태 초기화
    fun resetForNextPhase() {
        healTier = DefenseTier.NONE
        isSilenced = false
        isThreatened = false
        hasUsedDailyAbility = false
    }
}
