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
    override val description: String = "밤에 한 명의 플레이어를 선택하여 직업을 알아내고 첫 번째를 제외한 낮이 될 때 기사를 내어 모든 플레이어에게 해당 사실을 알린다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1485334734438531072/1f52d36dd92a43a6.png?ex=69c17d44&is=69c02bc4&hm=fa7c1c68ac505b3eb51602a0ced33a8bc34a987023dbce244be7f7fa79052d3b&"
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
