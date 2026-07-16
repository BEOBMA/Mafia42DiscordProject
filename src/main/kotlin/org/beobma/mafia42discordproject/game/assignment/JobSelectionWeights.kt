package org.beobma.mafia42discordproject.game.assignment

import org.beobma.mafia42discordproject.job.Job

private const val PREFERENCE_SELECTION_WEIGHT = 1
private const val BEST_JOB_SELECTION_MULTIPLIER = 3

internal fun buildJobSelectionWeightByName(
    players: List<AssignmentPlayer>,
    isEligible: (Job) -> Boolean
): Map<String, Int> {
    val weightsByName = mutableMapOf<String, Int>()

    fun addWeight(jobName: String, weight: Int) {
        if (weight <= 0) return
        weightsByName[jobName] = (weightsByName[jobName] ?: 0) + weight
    }

    players.forEach { player ->
        val eligiblePreferenceNames = mutableSetOf<String>()
        player.preferences.forEach { preference ->
            if (isEligible(preference) && eligiblePreferenceNames.add(preference.name)) {
                addWeight(preference.name, PREFERENCE_SELECTION_WEIGHT)
            }
        }

        val bestJob = player.bestJob ?: return@forEach
        if (!isEligible(bestJob)) return@forEach

        val bestJobBonus = if (bestJob.name in eligiblePreferenceNames) {
            PREFERENCE_SELECTION_WEIGHT * (BEST_JOB_SELECTION_MULTIPLIER - 1)
        } else {
            PREFERENCE_SELECTION_WEIGHT * BEST_JOB_SELECTION_MULTIPLIER
        }
        addWeight(bestJob.name, bestJobBonus)
    }

    return weightsByName
}
