package org.beobma.mafia42discordproject.game

import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import dev.kord.core.entity.channel.TextChannel
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.AttackEvent
import org.beobma.mafia42discordproject.game.system.GameEvent

data class DawnPresentation(
    val imageUrl: String,
    val message: String
)

data class NightResolutionSummary(
    val processedEvents: List<GameEvent> = emptyList(),
    val deaths: List<PlayerData> = emptyList(),
    val blockedAttacks: List<AttackEvent> = emptyList(),
    val dawnPresentation: DawnPresentation? = null
)

data class Game(
    var playerDatas: MutableList<PlayerData>,
    val guild: Guild,
    var currentPhase: GamePhase = GamePhase.DAY,
    var isRunning: Boolean = false,
) {
    private val playerById: MutableMap<Snowflake, PlayerData> = mutableMapOf()

    init {
        rebuildPlayerIndex()
    }

    var dayCount: Int = 0
    var mainChannel: TextChannel? = null
    var mafiaChannel: TextChannel? = null

    // Key: 공격 그룹 ("MAFIA_TEAM" 또는 "VIGILANTE_유저ID")
    val nightAttacks: MutableMap<String, AttackEvent> = mutableMapOf()
    val nightDeathCandidates: MutableList<PlayerData> = mutableListOf()
    val nightEvents: MutableList<GameEvent> = mutableListOf()
    var lastNightSummary: NightResolutionSummary = NightResolutionSummary()

    // 투표
    var currentMainVotes: MutableMap<Snowflake, String> = mutableMapOf()
    var currentProsConsVotes: MutableMap<Snowflake, Boolean> = mutableMapOf()

    fun replacePlayers(players: MutableList<PlayerData>) {
        playerDatas = players
        rebuildPlayerIndex()
    }

    fun rebuildPlayerIndex() {
        playerById.clear()
        playerDatas.forEach { player ->
            playerById[player.member.id] = player
        }
    }

    fun getPlayer(userId: Snowflake): PlayerData? =
        playerById[userId]
}
