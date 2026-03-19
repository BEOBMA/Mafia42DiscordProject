package org.beobma.mafia42discordproject.game

import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import dev.kord.core.entity.channel.TextChannel
import org.beobma.mafia42discordproject.game.system.AttackEvent
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.game.player.PlayerData

data class Game(
    var playerDatas: MutableList<PlayerData>,
    val guild: Guild,
    var currentPhase: GamePhase = GamePhase.DAY,
    var isRunning: Boolean = false,
    ) {
    var dayCount: Int = 0
    var mainChannel: TextChannel? = null
    var mafiaChannel: TextChannel? = null

    // Key: 공격 그룹 ("MAFIA_TEAM" 또는 "VIGILANTE_유저ID")
    val nightAttacks: MutableMap<String, AttackEvent> = mutableMapOf()

    // 3-2. 일반 사건 이벤트 큐 (조사 성공, 사망 등 파파라치나 도굴꾼에게 전달될 이벤트들)
    val nightEvents: MutableList<GameEvent> = mutableListOf()

    // 투표 데이터 맵
    var currentMainVotes: MutableMap<Snowflake, String> = mutableMapOf()

    // 👍 찬반 투표 기록 (Key: 투표자 ID, Value: 찬성=true, 반대=false)
    var currentProsConsVotes: MutableMap<Snowflake, Boolean> = mutableMapOf()

    fun getPlayer(userId: dev.kord.common.entity.Snowflake): PlayerData? {
        return playerDatas.find { it.member.id == userId }
    }
}