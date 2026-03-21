package org.beobma.mafia42discordproject.game

import org.beobma.mafia42discordproject.game.player.PlayerData

enum class GamePhase {
    DAY,    // 낮
    NIGHT,  // 밤
    DAWN,   // 밤 -> 낮 전환(새벽)
    VOTE,   // 투표시간
    END     // 끝
}
