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
    override val description: String = "밤에 선택한 플레이어가 마피아에게 처형되거나 자신이 마피아에게 선택되면 마피아에게 길들여진다. 길들여진 후 밤에 선택한 대상을 제거할 수 있다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(102).webp"
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

        if (!caster.state.isTamed) {
            val effectiveTarget = HackerRedirectManager.resolveTarget(game, target) ?: target
            if (effectiveTarget.member.id in beastman.markedTargetIds) {
                return AbilityResult(true, "${target.member.effectiveName} 님에게 새긴 표식은 유지됩니다.")
            }

            val maxMarkCount = if (game.dayCount == 1 && caster.allAbilities.any { it is Barbarism }) 2 else 1
            if (beastman.markedTargetIds.size >= maxMarkCount) {
                return AbilityResult(false, "이미 표식을 새겼습니다. 기존 표식은 변경할 수 없습니다.")
            }

            beastman.markedTargetIds += effectiveTarget.member.id
            return AbilityResult(true, "${target.member.effectiveName} 님에게 표식을 새겼습니다.")
        }

        val effectiveTarget = HackerRedirectManager.resolveTarget(game, target) ?: target
        val previousTarget = game.nightAttacks[MAFIA_EXECUTION_KEY]?.target
        if (previousTarget != null && previousTarget != effectiveTarget) {
            game.nightDeathCandidates.remove(previousTarget)
        }

        game.nightAttacks[MAFIA_EXECUTION_KEY] = AttackEvent(
            attacker = caster,
            target = effectiveTarget,
            attackTier = AttackTier.ABSOLUTE
        )

        if (effectiveTarget !in game.nightDeathCandidates) {
            game.nightDeathCandidates += effectiveTarget
        }

        return AbilityResult(true, "${target.member.effectiveName} 님을 제거 대상으로 지정했습니다.")
    }

    companion object {
        const val MAFIA_EXECUTION_KEY = "MAFIA_TEAM"
    }
}
