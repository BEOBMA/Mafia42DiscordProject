package org.beobma.mafia42discordproject.game

import org.beobma.mafia42discordproject.game.player.PlayerData

enum class GamePhase {
    DAY,    // 낮
    NIGHT,  // 밤
    VOTE,   // 투표시간
    END     // 끝
}