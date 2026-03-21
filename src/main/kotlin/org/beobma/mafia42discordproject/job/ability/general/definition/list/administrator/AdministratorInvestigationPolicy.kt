package org.beobma.mafia42discordproject.job.ability.general.definition.list.administrator

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.Hypocrisy
import org.beobma.mafia42discordproject.job.definition.list.Agent
import org.beobma.mafia42discordproject.job.definition.list.Detective
import org.beobma.mafia42discordproject.job.definition.list.Gangster
import org.beobma.mafia42discordproject.job.definition.list.Ghoul
import org.beobma.mafia42discordproject.job.definition.list.Martyr
import org.beobma.mafia42discordproject.job.definition.list.Nurse
import org.beobma.mafia42discordproject.job.definition.list.Police
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import org.beobma.mafia42discordproject.job.evil.Evil
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import org.beobma.mafia42discordproject.job.evil.list.Villain
import kotlin.reflect.KClass

object AdministratorInvestigationPolicy {
    private val policeLineJobs: Set<KClass<out Job>> = setOf(
        Police::class,
        Agent::class
    )

    fun isJobSelectable(job: Job, hasCooperation: Boolean, hasIdentification: Boolean): Boolean {
        if (!hasIdentification && job is Evil && job !is Villain && job !is Mafia) {
            return false
        }
        if (job is Mafia) {
            return false
        }
        if (!hasCooperation && job::class in policeLineJobs) {
            return false
        }
        return true
    }

    fun shouldApplyHypocrisySpoof(gameDay: Int, selectedJob: Job, target: org.beobma.mafia42discordproject.game.player.PlayerData): Boolean {
        if (gameDay != 1) return false
        if (selectedJob::class != org.beobma.mafia42discordproject.job.definition.list.Doctor::class) return false
        return target.allAbilities.any { it is Hypocrisy }
    }
}
