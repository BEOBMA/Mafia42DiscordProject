package org.beobma.mafia42discordproject.job.ability.general.definition.list.mercenary

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.AttackEvent
import org.beobma.mafia42discordproject.game.system.AttackTier
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.game.system.HackerRedirectManager
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.mercenary.Tracking
import org.beobma.mafia42discordproject.job.definition.list.Mercenary

class MercenaryAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "의뢰"
    override val description: String = "의뢰인이 밤에 살해되면 밤마다 플레이어 한 명을 처형할 수 있다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(18).webp"
    override val usablePhase: GamePhase = GamePhase.NIGHT

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "의뢰는 밤에만 사용할 수 있습니다.")
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
        if (target.member.id == caster.member.id) {
            return AbilityResult(false, "자기 자신은 처형 대상으로 지정할 수 없습니다.")
        }

        val mercenary = caster.job as? Mercenary
            ?: return AbilityResult(false, "용병만 의뢰 능력을 사용할 수 있습니다.")

        if (!mercenary.hasExecutionAuthority) {
            return AbilityResult(false, "아직 처형 권한이 없습니다.")
        }

        val effectiveTarget = HackerRedirectManager.resolveTarget(game, target) ?: target
        val attackKey = "MERCENARY_${caster.member.id}"
        val previousTarget = game.nightAttacks[attackKey]?.target
        if (previousTarget != null && previousTarget != effectiveTarget) {
            game.nightDeathCandidates.remove(previousTarget)
        }

        game.nightAttacks[attackKey] = AttackEvent(
            attacker = caster,
            target = effectiveTarget,
            attackTier = AttackTier.NORMAL
        )
        if (effectiveTarget !in game.nightDeathCandidates) {
            game.nightDeathCandidates += effectiveTarget
        }

        if (mercenary.firstTrackedTargetId == null) {
            mercenary.firstTrackedTargetId = effectiveTarget.member.id
            val hasTracking = caster.allAbilities.any { it is Tracking }
            val killerId = mercenary.clientKilledByPlayerId
            if (hasTracking && killerId != null && killerId == effectiveTarget.member.id) {
                val targetJob = effectiveTarget.job ?: return AbilityResult(
                    true,
                    "${target.member.effectiveName} 님을 처형 대상으로 지정했습니다."
                )
                game.nightEvents += GameEvent.JobDiscovered(
                    discoverer = caster,
                    target = effectiveTarget,
                    actualJob = targetJob,
                    revealedJob = targetJob,
                    sourceAbilityName = "추적",
                    resolvedAt = org.beobma.mafia42discordproject.game.system.DiscoveryStep.NIGHT,
                    notifyTarget = false
                )
            }
        }

        return AbilityResult(true, "${target.member.effectiveName} 님을 처형 대상으로 지정했습니다.")
    }
}
