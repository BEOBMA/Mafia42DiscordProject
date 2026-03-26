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
        winImageUrl = "https://discord.com/channels/1483817958319849616/1483977619258212392/1484986092489937039"
    ),
    PROPHET_SPECIAL(
        displayName = "시민 팀",
        embedColor = Color(0x3498db),
        winMessage = "예언자의 힘으로 시민 팀이 승리하였습니다.",
        winImageUrl = "https://cdn.discordapp.com/attachments/1483977619258212392/1485480468223623240/CGMZuPytyCcm8wMxKUOJlpnncYc8NSnooMaafS-LDUSszfkN7u6B7BRd1-SdPX4vAIXgySrq9sT8xwT-TiVznq5rxlkJl_LhpsVnhU8S0oE_Oz_fpKIwCOZMSr_j8KtCvSjoBQ9OWMoOutcwIC-9vg.webp?ex=69c204fe&is=69c0b37e&hm=605de13b4bf059e5d1dc101c231e1c999ac5d49209c6cef663b7bcdfeeaede18&"
    )
}
