package org.beobma.mafia42discordproject.job.ability.general.definition.list.hacker

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.HackerRedirectManager
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.definition.list.Hacker

class HackerAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "해킹"
    override val description: String = "낮에 플레이어 한 명을 골라 밤이 될 때 직업을 알아낸다."
    override val image: String = ""
    override val usablePhase: GamePhase = GamePhase.DAY

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "해킹은 낮에만 사용할 수 있습니다.")
        }
        if (caster.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 능력을 사용할 수 없습니다.")
        }
        if (target == null) {
            return AbilityResult(false, "해킹할 대상을 지정해야 합니다.")
        }
        if (target.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 해킹 대상으로 지정할 수 없습니다.")
        }
        if (target.member.id == caster.member.id) {
            return AbilityResult(false, "자기 자신은 해킹 대상으로 지정할 수 없습니다.")
        }

        val hacker = caster.job as? Hacker
            ?: return AbilityResult(false, "해커만 사용할 수 있습니다.")

        val effectiveTarget = HackerRedirectManager.resolveTarget(game, target) ?: target
        val existingTargetId = hacker.hackedTargetId
        if (existingTargetId != null && existingTargetId != effectiveTarget.member.id) {
            return AbilityResult(false, "한 번 정한 해킹 대상은 변경할 수 없습니다.")
        }

        hacker.hackedTargetId = effectiveTarget.member.id
        return AbilityResult(true, "${target.member.effectiveName}님을 해킹 대상으로 지정했습니다.")
    }
}
