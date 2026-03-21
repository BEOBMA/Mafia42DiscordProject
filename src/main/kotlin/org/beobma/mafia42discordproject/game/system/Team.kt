package org.beobma.mafia42discordproject.game.system

import dev.kord.common.Color

enum class Team(
    val displayName: String,
    val embedColor: Color,
    val winMessage: String
) {
    CITIZEN(
        displayName = "시민 팀",
        embedColor = Color(0x3498db), // 파란색
        winMessage = "모든 마피아를 소탕하고 시민 팀이 승리했습니다! 🎉"
    ),
    MAFIA(
        displayName = "마피아 팀",
        embedColor = Color(0xe74c3c), // 빨간색
        winMessage = "도시를 장악하고 마피아 팀이 승리했습니다! 🔫"
    ),
    CABAL_SPECIAL(
        displayName = "시민 팀",
        embedColor = Color(0x3498db),
        winMessage = "비밀결사의 접선이 완성되어 시민 팀이 특수 승리했습니다! 🌞🌙"
    )
}
