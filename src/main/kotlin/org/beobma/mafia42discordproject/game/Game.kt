package org.beobma.mafia42discordproject.game

import dev.kord.core.entity.Guild
import dev.kord.core.entity.channel.TextChannel
import org.beobma.mafia42discordproject.game.system.AttackEvent
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.game.player.PlayerData

data class Game(
    var playerDatas: MutableList<PlayerData>,
    val guild: Guild,
    var mainChannel: TextChannel,
    var mafiaChannel: TextChannel,
    var currentPhase: GamePhase = GamePhase.DAY,
    var isRunnig: Boolean = false,
    ) {
    // Key: 공격 그룹 ("MAFIA_TEAM" 또는 "VIGILANTE_유저ID")
    val nightAttacks: MutableMap<String, AttackEvent> = mutableMapOf()

    // 3-2. 일반 사건 이벤트 큐 (조사 성공, 사망 등 파파라치나 도굴꾼에게 전달될 이벤트들)
    val nightEvents: MutableList<GameEvent> = mutableListOf()

    // 몇 번째 낮/밤인지 추적
    var dayCount: Int = 0

    fun getPlayer(userId: dev.kord.common.entity.Snowflake): PlayerData? {
        return playerDatas.find { it.member.id == userId }
    }
}