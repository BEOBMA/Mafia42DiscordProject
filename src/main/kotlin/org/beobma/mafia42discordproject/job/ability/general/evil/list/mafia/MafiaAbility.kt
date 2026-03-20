package org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.AttackEvent
import org.beobma.mafia42discordproject.game.system.AttackTier
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility

class MafiaAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "처형"
    override val description: String = "밤에 대상을 지정해 사망 후보 리스트에 올립니다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484619430263521330/930fdad0076c57b6.png?ex=69bee317&is=69bd9197&hm=a0fb01e23944eb5f0cab3ad595509ef41f6716f9a897e2f1a204de8c083a7d3e&"
    override val usablePhase: GamePhase = GamePhase.NIGHT

    private val attackKey = "MAFIA_TEAM"

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "처형은 밤에만 사용할 수 있습니다.")
        }
        if (caster.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 능력을 사용할 수 없습니다.")
        }
        if (caster.state.isSilenced) {
            return AbilityResult(false, "침묵 상태에서는 처형 대상을 지정할 수 없습니다.")
        }
        if (target == null) {
            return AbilityResult(false, "처형할 대상을 지정해야 합니다.")
        }
        if (target.state.isDead) {
            return AbilityResult(false, "이미 사망한 플레이어는 처형 대상으로 지정할 수 없습니다.")
        }

        val casterJob = caster.job
            ?: return AbilityResult(false, "시전자 직업 정보를 확인할 수 없습니다.")
        val attackTier = if (casterJob.abilities.any { it::class == WinOrDead::class }) {
            AttackTier.PIERCE
        } else {
            AttackTier.NORMAL
        }

        val previousTarget = game.nightAttacks[attackKey]?.target
        if (previousTarget != null && previousTarget != target) {
            game.nightDeathCandidates.remove(previousTarget)
        }

        game.nightAttacks[attackKey] = AttackEvent(
            attacker = caster,
            target = target,
            attackTier = attackTier
        )
        if (target !in game.nightDeathCandidates) {
            game.nightDeathCandidates += target
        }

        return AbilityResult(true, "${target.member.effectiveName} 님을 처형 대상으로 지정했습니다.")
    }
}
