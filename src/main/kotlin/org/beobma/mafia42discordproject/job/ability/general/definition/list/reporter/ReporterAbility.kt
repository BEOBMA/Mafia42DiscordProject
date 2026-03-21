package org.beobma.mafia42discordproject.job.ability.general.definition.list.reporter

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.HackerRedirectManager
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.definition.list.Reporter

class ReporterAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "특종"
    override val description: String = "밤에 한 명의 플레이어를 선택하여 직업을 알아내고 낮에 기사로 공개한다. 한 번 선택한 대상은 변경할 수 없다."
    override val image: String = ""
    override val usablePhase: GamePhase = GamePhase.NIGHT

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "특종은 밤에만 사용할 수 있습니다.")
        }
        if (caster.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 능력을 사용할 수 없습니다.")
        }
        if (caster.state.isSilenced) {
            return AbilityResult(false, "침묵 상태에서는 특종 대상을 지정할 수 없습니다.")
        }
        if (target == null) {
            return AbilityResult(false, "취재할 대상을 지정해야 합니다.")
        }
        if (target.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 취재 대상으로 지정할 수 없습니다.")
        }

        val reporter = caster.job as? Reporter
            ?: return AbilityResult(false, "기자만 특종을 사용할 수 있습니다.")

        val effectiveTarget = HackerRedirectManager.resolveTarget(game, target) ?: target
        val fixedTargetId = reporter.selectedTargetId
        if (fixedTargetId != null && fixedTargetId != effectiveTarget.member.id) {
            return AbilityResult(false, "한 번 정한 취재 대상은 변경할 수 없습니다.")
        }
        if (reporter.hasUsedScoop) {
            return AbilityResult(false, "이미 특종을 사용했습니다.")
        }

        reporter.selectedTargetId = effectiveTarget.member.id
        reporter.hasUsedScoop = true
        return AbilityResult(true, "${effectiveTarget.member.effectiveName}님을 취재 대상으로 지정했습니다.")
    }
}
