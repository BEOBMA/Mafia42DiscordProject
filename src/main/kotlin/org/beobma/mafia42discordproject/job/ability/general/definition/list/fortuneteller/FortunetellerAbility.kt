package org.beobma.mafia42discordproject.job.ability.general.definition.list.fortuneteller

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.HackerRedirectManager
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.definition.list.Fortuneteller

class FortunetellerAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "운세"
    override val description: String = "밤마다 한 명을 선택한다. 선택한 플레이어 및 그와 다른 팀인 플레이어의 직업이 제시된다."
    override val image: String = ""
    override val usablePhase: GamePhase = GamePhase.NIGHT

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "운세는 밤에만 사용할 수 있습니다.")
        }
        if (caster.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 능력을 사용할 수 없습니다.")
        }
        if (target == null) {
            return AbilityResult(false, "운세 대상을 지정해야 합니다.")
        }
        if (target.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 운세 대상으로 지정할 수 없습니다.")
        }

        val fortuneteller = caster.job as? Fortuneteller
            ?: return AbilityResult(false, "점쟁이만 사용할 수 있습니다.")

        val effectiveTarget = HackerRedirectManager.resolveTarget(game, target) ?: target
        val fixedTargetId = fortuneteller.fixedFortuneTargetId
        if (fixedTargetId != null && fixedTargetId != effectiveTarget.member.id) {
            return AbilityResult(false, "한번 정한 운세 대상은 변경할 수 없습니다.")
        }

        fortuneteller.fixedFortuneTargetId = effectiveTarget.member.id
        return AbilityResult(true, "${target.member.effectiveName}님을 운세 대상으로 지정했습니다.")
    }
}
