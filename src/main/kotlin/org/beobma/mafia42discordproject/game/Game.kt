package org.beobma.mafia42discordproject.game

import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.TextChannelThread
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.AttackEvent
import org.beobma.mafia42discordproject.game.system.GameEvent

data class NightResolutionSummary(
    val processedEvents: List<GameEvent> = emptyList(),
    val deaths: List<PlayerData> = emptyList(),
    val blockedAttacks: List<AttackEvent> = emptyList()
)

data class Game(
    var playerDatas: MutableList<PlayerData>,
    val guild: Guild,
    var currentPhase: GamePhase = GamePhase.DAY,
    var isRunning: Boolean = false,
) {
    var dayCount: Int = 0
    var mainChannel: TextChannel? = null
    var mafiaChannel: TextChannelThread? = null
    val nightAttacks: MutableMap<String, AttackEvent> = mutableMapOf()
    val nightDeathCandidates: MutableList<PlayerData> = mutableListOf()
    val nightEvents: MutableList<GameEvent> = mutableListOf()
    var lastNightSummary: NightResolutionSummary = NightResolutionSummary()
    var currentMainVotes: MutableMap<Snowflake, String> = mutableMapOf()
    var currentProsConsVotes: MutableMap<Snowflake, Boolean> = mutableMapOf()

    fun getPlayer(userId: Snowflake): PlayerData? =
        playerDatas.find { it.member.id == userId }
}
