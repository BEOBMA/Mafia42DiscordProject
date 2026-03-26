package org.beobma.mafia42discordproject.game.system

import org.beobma.mafia42discordproject.game.player.PlayerData

data class AttackEvent(
    val attacker: PlayerData,
    val target: PlayerData,
    val attackTier: AttackTier = AttackTier.NORMAL // 기본은 일반 공격
)