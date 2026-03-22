package org.beobma.mafia42discordproject.job.ability.general.evil.list.godfather

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.AttackEvent
import org.beobma.mafia42discordproject.game.system.AttackTier
import org.beobma.mafia42discordproject.game.system.HackerRedirectManager
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility

class GodfatherAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "말살"
    override val description: String = "접선 후 밤마다 다른 플레이어의 능력을 무시하고 처형할 수 있다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484619430263521330/930fdad0076c57b6.png?ex=69bee317&is=69bd9197&hm=a0fb01e23944eb5f0cab3ad595509ef41f6716f9a897e2f1a204de8c083a7d3e&"
    override val usablePhase: GamePhase = GamePhase.NIGHT

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "말살은 밤에만 사용할 수 있습니다.")
        }
        if (caster.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 능력을 사용할 수 없습니다.")
        }
        if (!GodfatherContactPolicy.canUseExecution(game, caster)) {
            return AbilityResult(false, "아직 마피아와 접선하지 않아 말살을 사용할 수 없습니다.")
        }
        if (target == null) {
            return AbilityResult(false, "처형할 대상을 지정해야 합니다.")
        }
        if (target.state.isDead) {
            return AbilityResult(false, "이미 사망한 플레이어는 처형 대상으로 지정할 수 없습니다.")
        }

        val effectiveTarget = HackerRedirectManager.resolveTarget(game, target) ?: target
        val attackKey = "GODFATHER_${caster.member.id.value}"
        val previousTarget = game.nightAttacks[attackKey]?.target
        if (previousTarget != null && previousTarget != effectiveTarget) {
            game.nightDeathCandidates.remove(previousTarget)
        }

        game.nightAttacks[attackKey] = AttackEvent(
            attacker = caster,
            target = effectiveTarget,
            attackTier = AttackTier.ABSOLUTE
        )
        if (effectiveTarget !in game.nightDeathCandidates) {
            game.nightDeathCandidates += effectiveTarget
        }

        return AbilityResult(true, "${target.member.effectiveName} 님을 말살 대상으로 지정했습니다.")
    }
}
