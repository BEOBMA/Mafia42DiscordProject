package org.beobma.mafia42discordproject.game.abilityselection

import dev.kord.common.entity.Snowflake
import kotlin.random.Random

internal enum class AbilityRefreshGrantReason(
    val refreshCount: Int,
    val notificationMessage: String?
) {
    PREVIOUS_PUBLIC_TARGET(1, "전판 퍼블 대상이었기에 새로고침 기회가 1회 증가합니다."),
    SOLO_MAFIA(2, "마피아로 혼자 시작하기에 새로고침 기회가 2회 증가합니다."),
    EXTENDED_GAME_MAFIA(1, null)
}

internal data class AbilityRefreshGrant(
    val refreshCount: Int,
    val reasons: Set<AbilityRefreshGrantReason>
)

internal fun selectAbilityRefreshGrants(
    playerCount: Int,
    mafiaPlayerIds: List<Snowflake>,
    previousPublicTargetId: Snowflake?,
    random: Random = Random.Default
): Map<Snowflake, AbilityRefreshGrant> {
    val reasonsByPlayerId = linkedMapOf<Snowflake, MutableSet<AbilityRefreshGrantReason>>()

    fun grant(playerId: Snowflake, reason: AbilityRefreshGrantReason) {
        reasonsByPlayerId.getOrPut(playerId, ::linkedSetOf).add(reason)
    }

    when {
        mafiaPlayerIds.size == 1 -> grant(mafiaPlayerIds.single(), AbilityRefreshGrantReason.SOLO_MAFIA)
        playerCount == 9 -> mafiaPlayerIds
            .shuffled(random)
            .take(1)
            .forEach { grant(it, AbilityRefreshGrantReason.EXTENDED_GAME_MAFIA) }
        playerCount == 10 -> mafiaPlayerIds
            .forEach { grant(it, AbilityRefreshGrantReason.EXTENDED_GAME_MAFIA) }
    }

    previousPublicTargetId?.let {
        grant(it, AbilityRefreshGrantReason.PREVIOUS_PUBLIC_TARGET)
    }

    return reasonsByPlayerId.mapValues { (_, reasons) ->
        AbilityRefreshGrant(
            refreshCount = reasons.sumOf(AbilityRefreshGrantReason::refreshCount),
            reasons = reasons.toSet()
        )
    }
}
