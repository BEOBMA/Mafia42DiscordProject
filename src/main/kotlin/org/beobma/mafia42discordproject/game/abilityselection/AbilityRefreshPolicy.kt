package org.beobma.mafia42discordproject.game.abilityselection

import dev.kord.common.entity.Snowflake
import kotlin.random.Random

private const val SOLO_MAFIA_REFRESH_LIMIT = 2
private const val EXTENDED_GAME_REFRESH_LIMIT = 1

internal fun selectMafiaAbilityRefreshLimits(
    playerCount: Int,
    mafiaPlayerIds: List<Snowflake>,
    random: Random = Random.Default
): Map<Snowflake, Int> = when {
    mafiaPlayerIds.size == 1 -> mapOf(mafiaPlayerIds.single() to SOLO_MAFIA_REFRESH_LIMIT)
    playerCount == 9 -> mafiaPlayerIds
        .shuffled(random)
        .take(1)
        .associateWith { EXTENDED_GAME_REFRESH_LIMIT }
    playerCount == 10 -> mafiaPlayerIds.associateWith { EXTENDED_GAME_REFRESH_LIMIT }
    else -> emptyMap()
}
