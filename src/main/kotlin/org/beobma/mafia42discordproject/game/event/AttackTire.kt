package org.beobma.mafia42discordproject.game.event

enum class AttackTier(val level: Int) {
    NORMAL(1),      // 일반 공격 (마피아, 자경단원, 용병 등)
    PIERCE(2),      // 강화 공격 (마피아 '승부수' 등 - 일반 힐을 뚫음)
    ABSOLUTE(3)     // 절대 공격 (대부 '말살', 짐승인간 '갈망' - 모든 방어를 뚫음)
}