package org.beobma.mafia42discordproject.game.assignment

import kotlin.random.Random

internal fun selectUniformMafiaPlayerIndices(
    playerCount: Int,
    mafiaCount: Int,
    random: Random = Random.Default
): Set<Int> {
    require(playerCount >= 0) { "플레이어 수는 0명 이상이어야 합니다." }
    require(mafiaCount in 0..playerCount) { "마피아 수는 플레이어 수 범위 안이어야 합니다." }

    return (0 until playerCount)
        .shuffled(random)
        .take(mafiaCount)
        .toSet()
}
