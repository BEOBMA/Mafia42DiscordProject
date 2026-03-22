package org.beobma.mafia42discordproject.game.player

import dev.kord.common.entity.Snowflake
import org.beobma.mafia42discordproject.game.system.DefenseTier
import org.beobma.mafia42discordproject.game.system.GameEvent

class PlayerState {
    // 영구적/누적 상태 (게임 끝날 때까지 유지)
    var isDead: Boolean = false
    var hasUsedOneTimeAbility: Boolean = false // 자경단원, 군인, 성직자 등의 1회용 소모 여부
    var hasUsedDailyAbility: Boolean = false   // 파파라치 등 하루 1회 소모 여부
    var isTamed: Boolean = false               // 짐승인간 길들여짐 여부
    var isJobPubliclyRevealed: Boolean = false // 모든 플레이어에게 직업이 공개되었는지 여부
    var hasAnnouncedGodfatherContact: Boolean = false // 대부 접선 알림 전송 여부
    var hasAnnouncedHitmanContact: Boolean = false // 청부업자 접선 알림 전송 여부
    var hasAnnouncedMadScientistContact: Boolean = false // 과학자 접선 알림 전송 여부
    var hasAnnouncedThiefContact: Boolean = false // 도둑 접선 알림 전송 여부
    var hasContactedMafiaByInformant: Boolean = false // 밀정 능력으로 접선 여부
    var hasContactedMafiaOnDeath: Boolean = false // 과학자 사망 유착 여부
    var hasUsedMadScientistRegeneration: Boolean = false // 과학자 재생 사용 여부
    var pendingMadScientistRevivalNight: Int? = null // 과학자 재생 예정 밤(일차)
    var pendingMadScientistPublicRevealNight: Int? = null // 왜곡 재생 공개 예정 밤(일차)
    var isMadScientistDistortionHidden: Boolean = false // 왜곡 재생으로 아직 공개되지 않은 상태
    var madScientistLynchedVoteTargetId: Snowflake? = null // 투표 사망 당시 투표 대상
    var madScientistAnalysisEligibleDay: Int? = null // 분석 가중치가 유효한 낮 일차
    var hasUsedMadScientistAnalysis: Boolean = false // 분석 가중치 사용 여부
    var lastPaparazziIssueDay: Int? = null // 파파라치 이슈 발동 일차(밤/낮 합산 1회 제한)
    var pendingPaparazziIssuePriority: Int? = null // 같은 일차 내 이슈 우선순위 비교용
    var pendingPaparazziIssueEvent: GameEvent.JobDiscovered? = null // 같은 일차 내 교체 가능한 이슈 이벤트

    // 일시적 상태 (밤/낮이 바뀔 때 초기화 필요)
    var healTier: DefenseTier = DefenseTier.NONE // 현재 받고 있는 힐의 방어 티어
    var isSilenced: Boolean = false              // 마담 유혹 여부
    var isThreatened: Boolean = false            // 건달 협박 여부
    var isShamaned: Boolean = false              // 성불 여부
    var isPoisoned: Boolean = false              // 중독 여부
    var poisonedDeathDay: Int? = null            // 해당 일차 새벽에 중독사 처리
    var isFrogCursed: Boolean = false            // 마녀 저주(개구리화) 여부
    var frogCurseExpiresAfterDay: Int? = null    // 저주 해제 시점(해당 일차 낮 종료 후)

    // 새로운 페이즈가 시작될 때 일회성 상태 초기화
    fun resetForNextPhase() {
        healTier = DefenseTier.NONE
        isSilenced = false
        isThreatened = false
        hasUsedDailyAbility = false
    }
}
