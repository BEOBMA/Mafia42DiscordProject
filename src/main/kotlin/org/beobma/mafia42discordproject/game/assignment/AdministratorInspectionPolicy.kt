package org.beobma.mafia42discordproject.game.assignment

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition
import org.beobma.mafia42discordproject.job.definition.list.Administrator
import org.beobma.mafia42discordproject.job.definition.list.MentalPatient

internal object AdministratorInspectionPolicy {
    private const val MAFIA_JOB_NAME = "마피아"
    private const val DOCTOR_JOB_NAME = "의사"
    private const val CITIZEN_JOB_NAME = "시민"

    fun candidates(
        jobs: Sequence<Job>,
        requiredRoleCounts: RequiredRoleCounts
    ): List<Job> {
        val guaranteedJobNames = buildSet {
            if (requiredRoleCounts.mafiaCount > 0) add(MAFIA_JOB_NAME)
            if (requiredRoleCounts.doctorCount > 0) add(DOCTOR_JOB_NAME)
            if (requiredRoleCounts.citizenCount > 0) add(CITIZEN_JOB_NAME)
        }

        return jobs
            .filter { job ->
                job is Definition &&
                    job !is Administrator &&
                    job !is MentalPatient &&
                    job.name !in guaranteedJobNames
            }
            .distinctBy(Job::name)
            .toList()
    }
}
