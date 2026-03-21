package org.beobma.mafia42discordproject.job.ability.general.definition.list.priest

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.HackerRedirectManager
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.definition.list.Priest

class Resurrection : ActiveAbility, JobUniqueAbility {
    override val name: String = "소생"
    override val description: String = "밤에 죽은 플레이어 한 명을 선택해 다음 낮에 부활시킨다. (1회용)"
    override val image: String = ""
    override val usablePhase: GamePhase = GamePhase.NIGHT

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "소생은 밤에만 사용할 수 있습니다.")
        }
        if (caster.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 능력을 사용할 수 없습니다.")
        }
        if (caster.state.hasUsedOneTimeAbility) {
            return AbilityResult(false, "소생 능력은 이미 사용했습니다.")
        }
        if (target == null) {
            return AbilityResult(false, "소생할 대상을 지정해야 합니다.")
        }

        val priestJob = caster.job as? Priest ?: return AbilityResult(false, "")
        val effectiveTarget = HackerRedirectManager.resolveTarget(game, target) ?: target
        if (!effectiveTarget.state.isDead) {
            return AbilityResult(false, "죽은 플레이어만 소생 대상으로 지정할 수 있습니다.")
        }

        priestJob.pendingResurrectionTargetId = effectiveTarget.member.id
        caster.state.hasUsedOneTimeAbility = true

        return AbilityResult(true, "${target.member.effectiveName}님을 소생 대상으로 지정했습니다.")
    }
}
