package org.beobma.mafia42discordproject.game.abilityselection

import dev.kord.common.entity.Snowflake
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability

internal data class AbilitySelectionSession(
    val playerJob: Job,
    val availablePool: MutableList<Ability>,
    val maxRefreshes: Int = 0,
    val selected: MutableList<Ability> = mutableListOf(),
    var currentOptions: List<Ability> = emptyList(),
    var completedRounds: Int = 0,
    var refreshesUsed: Int = 0
)

internal data class AbilityCommandGuide(
    val abilityName: String,
    val timing: String,
    val command: String?,
    val summary: String
)

data class AbilityPickButtonPayload(
    val ownerUserId: Snowflake,
    val pickNumber: Int,
    val isRefresh: Boolean = false
)

data class AbilitySelectionSnapshot(
    val guideMessage: String,
    val optionCount: Int
)
