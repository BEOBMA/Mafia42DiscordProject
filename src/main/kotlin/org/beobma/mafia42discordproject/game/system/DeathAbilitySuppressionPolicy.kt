package org.beobma.mafia42discordproject.game.system

internal object DeathAbilitySuppressionPolicy {
    fun shouldAnnounceWill(
        hasStoredWill: Boolean,
        hasCurrentWillAbility: Boolean,
        wasConvertedByProbation: Boolean,
        isWillSuppressed: Boolean = false
    ): Boolean {
        if (isWillSuppressed) return false
        return hasCurrentWillAbility || (hasStoredWill && wasConvertedByProbation)
    }

    fun displayedWillContent(storedWill: String?): String = storedWill.orEmpty().ifEmpty { "''" }

    fun shouldSuppressBelongings(wasConvertedByProbation: Boolean): Boolean {
        return wasConvertedByProbation
    }

    fun shouldSuppressWillByExorcism(
        attackerHasExorcism: Boolean,
        targetIsEvil: Boolean,
        targetHasEarthboundSpirit: Boolean
    ): Boolean {
        return attackerHasExorcism && !targetIsEvil && !targetHasEarthboundSpirit
    }
}
