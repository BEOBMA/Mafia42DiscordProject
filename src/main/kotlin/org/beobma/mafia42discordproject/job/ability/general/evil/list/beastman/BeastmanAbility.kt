package org.beobma.mafia42discordproject.job.ability.general.evil.list.beastman

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.AttackEvent
import org.beobma.mafia42discordproject.game.system.AttackTier
import org.beobma.mafia42discordproject.game.system.HackerRedirectManager
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.evil.list.Beastman

class BeastmanAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "갈망"
    override val description: String = "밤에 선택한 플레이어가 마피아에게 처형되면 마피아에게 길들여진다. 길들여진 후 마피아의 일반 처형과 동일한 판정으로 플레이어를 제거할 수 있다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(50).webp"
    override val usablePhase: GamePhase = GamePhase.NIGHT

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "갈망은 밤에만 사용할 수 있습니다.")
        }
        if (caster.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 능력을 사용할 수 없습니다.")
        }
        if (caster.state.isSilenced) {
            return AbilityResult(false, "침묵 상태에서는 능력을 사용할 수 없습니다.")
        }
        if (target == null) {
            return AbilityResult(false, "대상을 지정해야 합니다.")
        }
        if (target.state.isDead) {
            return AbilityResult(false, "이미 사망한 플레이어는 대상으로 지정할 수 없습니다.")
        }
        if (target.member.id == caster.member.id) {
            return AbilityResult(false, "자기 자신은 대상으로 지정할 수 없습니다.")
        }

        val beastman = caster.job as? Beastman
            ?: return AbilityResult(false, "짐승인간만 갈망을 사용할 수 있습니다.")

        val effectiveTarget = HackerRedirectManager.resolveTarget(game, target) ?: target
        if (!caster.state.isTamed) {
            beastman.cravingTargetIdTonight = effectiveTarget.member.id
            return AbilityResult(true, "${target.member.effectiveName} 님을 갈망 대상으로 지정했습니다.")
        }

        val attackKey = "$BEASTMAN_ATTACK_KEY_PREFIX${caster.member.id.value}"
        val previousTarget = game.nightAttacks[attackKey]?.target
        if (previousTarget != null && previousTarget != effectiveTarget) {
            val hasOtherAttack = game.nightAttacks.any { (otherKey, attack) ->
                otherKey != attackKey && attack.target == previousTarget
            }
            if (!hasOtherAttack) {
                game.nightDeathCandidates.remove(previousTarget)
            }
        }

        game.nightAttacks[attackKey] = AttackEvent(
            attacker = caster,
            target = effectiveTarget,
            attackTier = TAMED_EXECUTION_ATTACK_TIER
        )

        if (effectiveTarget !in game.nightDeathCandidates) {
            game.nightDeathCandidates += effectiveTarget
        }

        return AbilityResult(true, "${target.member.effectiveName} 님을 제거 대상으로 지정했습니다.")
    }

    companion object {
        const val BEASTMAN_ATTACK_KEY_PREFIX = "BEASTMAN_"
        internal val TAMED_EXECUTION_ATTACK_TIER = AttackTier.NORMAL
    }
}

class BeastmanAgility : JobUniqueAbility {
    override val name: String = "민첩"
    override val description: String = "마피아의 공격으로부터 죽지 않는다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/beastman_ability_2.webp"
}
