package org.beobma.mafia42discordproject.job.ability.general.definition.list.nurse

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.HackerRedirectManager
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.definition.list.Nurse

class NurseAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "처방"
    override val description: String = "밤마다 플레이어 한 명을 선택해 의사 여부를 조사한다."
    override val image: String = ""
    override val usablePhase: GamePhase = GamePhase.NIGHT

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "밤에만 사용할 수 있습니다.")
        }
        if (caster.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 능력을 사용할 수 없습니다.")
        }
        if (target == null) {
            return AbilityResult(false, "처방 대상을 지정해야 합니다.")
        }
        if (target.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 처방 대상으로 지정할 수 없습니다.")
        }
        if (target.member.id == caster.member.id) {
            return AbilityResult(false, "자기 자신은 처방 대상으로 지정할 수 없습니다.")
        }

        val nurseJob = caster.job as? Nurse
            ?: return AbilityResult(false, "간호사가 아닙니다.")

        val effectiveTarget = HackerRedirectManager.resolveTarget(game, target) ?: target
        val existingTargetId = nurseJob.prescribedTargetId
        if (existingTargetId != null && existingTargetId != effectiveTarget.member.id) {
            return AbilityResult(false, "한 번 정한 처방 대상은 변경할 수 없습니다.")
        }

        nurseJob.prescribedTargetId = effectiveTarget.member.id
        return AbilityResult(true, "${target.member.effectiveName}님을 처방 대상으로 지정했습니다.")
    }
}
