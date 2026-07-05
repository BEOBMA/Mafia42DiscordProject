package org.beobma.mafia42discordproject.game.assignment

import dev.kord.common.entity.Snowflake
import org.beobma.mafia42discordproject.job.Job

internal data class RequiredRoleCounts(
    val mafiaCount: Int,
    val assistantCount: Int,
    val doctorCount: Int,
    val policeCount: Int,
    val citizenCount: Int = 0
)

internal data class AssignmentPlayer(
    val memberId: Snowflake? = null,
    val name: String,
    val preferences: List<Job>,
    val bestJob: Job? = null,
    var assignedJob: Job? = null
)

internal data class AssignmentTrace(
    val lines: MutableList<String> = mutableListOf()
) {
    fun add(message: String) {
        lines += message
    }
}

data class JobAssignmentSimulationResult(
    val lines: List<String>,
    val assignedJobCountByName: Map<String, Int>
)
