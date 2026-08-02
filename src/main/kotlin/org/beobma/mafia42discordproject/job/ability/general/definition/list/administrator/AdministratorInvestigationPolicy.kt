package org.beobma.mafia42discordproject.job.ability.general.definition.list.administrator

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.game.system.InvestigationTeam
import org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.Hypocrisy
import org.beobma.mafia42discordproject.job.definition.list.Agent
import org.beobma.mafia42discordproject.job.definition.list.Inspector
import org.beobma.mafia42discordproject.job.definition.list.Police
import org.beobma.mafia42discordproject.job.definition.list.Vigilante
import org.beobma.mafia42discordproject.job.evil.Evil
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import org.beobma.mafia42discordproject.job.evil.list.Villain
import kotlin.reflect.KClass

object AdministratorInvestigationPolicy {
    private val policeLineJobs: Set<KClass<out Job>> = setOf(
        Police::class,
        Inspector::class,
        Agent::class,
        Vigilante::class
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
        if (InvestigationTeam.of(target) == InvestigationTeam.FROG) return false
        return target.allAbilities.any { it is Hypocrisy }
    }
}
