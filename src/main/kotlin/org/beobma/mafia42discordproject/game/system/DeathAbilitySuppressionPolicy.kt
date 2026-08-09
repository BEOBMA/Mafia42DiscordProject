package org.beobma.mafia42discordproject.game.system

internal object DeathAbilitySuppressionPolicy {
    fun shouldAnnounceWill(
        hasStoredWill: Boolean,
        hasCurrentWillAbility: Boolean,
        wasConvertedByProbation: Boolean
    ): Boolean {
        return hasStoredWill && (hasCurrentWillAbility || wasConvertedByProbation)
    }

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
