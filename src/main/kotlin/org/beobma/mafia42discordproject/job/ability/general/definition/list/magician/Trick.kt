package org.beobma.mafia42discordproject.job.ability.general.definition.list.magician

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.HackerRedirectManager
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.definition.list.Magician
import org.beobma.mafia42discordproject.job.evil.list.actualOrStolenJob

class Trick : ActiveAbility, JobUniqueAbility {
    override val name: String = "트릭"
    override val description: String =
        "투표 시간마다 생존한 다른 플레이어를 골라 자신이 최후의 반론에 오를 경우 그 플레이어를 대신 최후의 반론에 올린다. 바꿔치기에 성공한 경우 이 능력을 다시는 사용할 수 없으며, 자신의 정체가 모두에게 공개된다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/magician_ability_trick.webp"
    override val usablePhase: GamePhase = GamePhase.VOTE

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase || game.defenseTargetId != null) {
            return AbilityResult(false, "트릭은 투표 시간에만 사용할 수 있습니다.")
        }
        if (caster.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 능력을 사용할 수 없습니다.")
        }
        val magician = caster.actualOrStolenJob<Magician>()
            ?: return AbilityResult(false, "마술사 또는 트릭 능력을 훔친 도둑만 사용할 수 있습니다.")
        if (magician.hasUsedTrick) {
            return AbilityResult(false, "이미 트릭으로 바꿔치기에 성공하여 다시 사용할 수 없습니다.")
        }
        if (target == null) {
            return AbilityResult(false, "바꿔치기할 대상을 지정해야 합니다.")
        }
        if (target.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 트릭 대상으로 지정할 수 없습니다.")
        }
        if (target.member.id == caster.member.id) {
            return AbilityResult(false, "자기 자신은 트릭 대상으로 지정할 수 없습니다.")
        }

        val effectiveTarget = HackerRedirectManager.resolveTarget(game, target) ?: target
        if (effectiveTarget.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 트릭 대상으로 지정할 수 없습니다.")
        }
        if (effectiveTarget.member.id == caster.member.id) {
            return AbilityResult(false, "자기 자신은 트릭 대상으로 지정할 수 없습니다.")
        }

        val previousTargetId = magician.trickTargetId
        magician.trickTargetId = effectiveTarget.member.id
        val action = if (previousTargetId != null && previousTargetId != effectiveTarget.member.id) "변경" else "지정"
        return AbilityResult(true, "${effectiveTarget.member.effectiveName}님을 트릭 대상으로 ${action}했습니다.")
    }
}
