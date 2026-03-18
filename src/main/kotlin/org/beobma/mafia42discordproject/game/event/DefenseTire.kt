package org.beobma.mafia42discordproject.game.event

enum class DefenseTier(val level: Int) {
    NONE(0),        // 방어 없음
    NORMAL(1),      // 일반 의사의 힐 (일반 공격 방어)
    ABSOLUTE(2)     // 접선 간호사 힐, 군인 방탄 (강화 공격 방어)
}