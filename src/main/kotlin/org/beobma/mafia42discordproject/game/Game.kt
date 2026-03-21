package org.beobma.mafia42discordproject.game

import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import dev.kord.core.entity.channel.TextChannel
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.AttackEvent
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.game.system.Team
import org.beobma.mafia42discordproject.job.Job

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
    var prophetSpecialWinScheduledTeam: Team? = null
    var mainChannel: TextChannel? = null
    var mafiaChannel: TextChannel? = null
    var coupleChannel: TextChannel? = null
    var deadChannel: TextChannel? = null

    // Key: 공격 그룹 ("MAFIA_TEAM" 또는 "VIGILANTE_유저ID")
    val nightAttacks: MutableMap<String, AttackEvent> = mutableMapOf()
    val nightDeathCandidates: MutableList<PlayerData> = mutableListOf()
    val nightEvents: MutableList<GameEvent> = mutableListOf()
    var lastNightSummary: NightResolutionSummary = NightResolutionSummary()
    var mafiaAttackFailedPreviousNight: Boolean = false
    var concealmentForcedQuietNight: Boolean = false
    val coupleSacrificeMap: MutableMap<Snowflake, Snowflake> = mutableMapOf()

    // 투표
    var currentMainVotes: MutableMap<Snowflake, String> = mutableMapOf()
    var currentProsConsVotes: MutableMap<Snowflake, Boolean> = mutableMapOf()
    var defenseTargetId: Snowflake? = null
    var unwrittenRuleBlockedTargetIdTonight: Snowflake? = null
    // 건달 공갈(일시) 및 길동무(영구) 투표권 박탈 상태
    var activeThreatenedVoters: MutableMap<Snowflake, Snowflake> = mutableMapOf() // Key: 대상, Value: 건달
    var permanentlyDisenfranchisedVoters: MutableSet<Snowflake> = mutableSetOf()

    val graveRobTargetsByGhoul: MutableMap<Snowflake, Snowflake> = mutableMapOf()
    val ghostTriggeredGhouls: MutableSet<Snowflake> = mutableSetOf()
    val probationOriginalJobsByPlayer: MutableMap<Snowflake, Job> = mutableMapOf()
    val abilityUsersThisPhase: MutableSet<Snowflake> = mutableSetOf()
    val abilityTargetByUserThisPhase: MutableMap<Snowflake, Snowflake> = mutableMapOf()

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

    fun getPlayer(userId: Snowflake): PlayerData? {
        val indexedPlayer = playerById[userId]
        if (indexedPlayer != null) {
            return indexedPlayer
        }

        val fallbackPlayer = playerDatas.firstOrNull { player ->
            player.member.id == userId
        }

        if (fallbackPlayer != null) {
            playerById[userId] = fallbackPlayer
        }

        return fallbackPlayer
    }
}
