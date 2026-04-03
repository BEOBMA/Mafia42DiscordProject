package org.beobma.mafia42discordproject.job.ability.general.definition.list.administrator

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.job.JobManager
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.definition.list.Administrator

class AdministratorAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "조회"
    override val description: String = "경찰 계열을 제외한 시민팀 직업 중 하나를 지목하여 그 직업을 가진 사람을 알아낸다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(140).webp"
    override val usablePhase: GamePhase = GamePhase.NIGHT

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        return AbilityResult(false, "공무원 조회는 직업 이름을 지정해서 사용해야 합니다.")
    }

    fun activateWithJobName(game: Game, caster: PlayerData, selectedJobName: String?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "밤에만 사용할 수 있습니다.")
        }
        if (caster.state.isDead) {
            return AbilityResult(false, "죽은 상태에서는 사용할 수 없습니다.")
        }
        val administrator = caster.job as? Administrator
            ?: return AbilityResult(false, "공무원만 사용할 수 있습니다.")

        if (selectedJobName.isNullOrBlank()) {
            administrator.selectedInvestigationJobName = null
            return AbilityResult(true, "이번 밤의 조회 대상을 해제했습니다.")
        }

        val selectedJob = JobManager.findByName(selectedJobName)
            ?: return AbilityResult(false, "선택한 직업을 찾을 수 없습니다.")

        val hasCooperation = caster.allAbilities.any { it is Cooperation }
        val hasIdentification = caster.allAbilities.any { it is Identification }
        if (!AdministratorInvestigationPolicy.isJobSelectable(selectedJob, hasCooperation, hasIdentification)) {
            return AbilityResult(false, "현재 보유한 능력으로는 해당 직업을 조회할 수 없습니다.")
        }

        administrator.selectedInvestigationJobName = selectedJob.name
        return AbilityResult(true, "${selectedJob.name} 직업을 조회 대상으로 선택했습니다.")
    }
}
