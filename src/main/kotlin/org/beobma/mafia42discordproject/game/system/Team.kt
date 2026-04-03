package org.beobma.mafia42discordproject.game.system

import dev.kord.common.Color

enum class Team(
    val displayName: String,
    val embedColor: Color,
    val winMessage: String,
    val winImageUrl: String? = null
) {
    CITIZEN(
        displayName = "시민 팀",
        embedColor = Color(0x3498db), // 파란색
        winMessage = "모든 마피아를 소탕하고 시민 팀이 승리했습니다."
    ),
    MAFIA(
        displayName = "마피아 팀",
        embedColor = Color(0xe74c3c), // 빨간색
        winMessage = "도시를 장악하고 마피아 팀이 승리했습니다."
    ),
    CABAL_SPECIAL(
        displayName = "시민 팀",
        embedColor = Color(0x3498db),
        winMessage = "해 비밀결사와 달 비밀결사가 서로를 찾아내어 시민팀이 승리합니다!",
        winImageUrl = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(9).png"
    ),
    PROPHET_SPECIAL(
        displayName = "시민 팀",
        embedColor = Color(0x3498db),
        winMessage = "예언자의 힘으로 시민 팀이 승리하였습니다.",
        winImageUrl = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(55).webp"
    )
}
