package org.beobma.mafia42discordproject.job.ability.general.definition.list.gangster

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.HackerRedirectManager
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.definition.list.Gangster

class GangsterAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "공갈"
    override val description: String = "밤마다 플레이어 한 명을 선택하여 다음날 투표시 해당 플레이어의 투표권을 빼앗는다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(148).webp"
    override val usablePhase: GamePhase = GamePhase.NIGHT

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "공갈은 밤에만 사용할 수 있습니다.")
        }
        if (caster.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 능력을 사용할 수 없습니다.")
        }
        if (caster.state.isSilenced) {
            return AbilityResult(false, "매혹 상태에서는 공갈을 사용할 수 없습니다.")
        }
        if (target == null) {
            return AbilityResult(false, "공갈 대상을 지정해야 합니다.")
        }
        if (target.state.isDead) {
            return AbilityResult(false, "이미 사망한 플레이어는 공갈 대상으로 지정할 수 없습니다.")
        }

        val gangster = caster.job as? Gangster
            ?: return AbilityResult(false, "건달만 공갈을 사용할 수 있습니다.")

        if (gangster.remainingThreatUsesTonight <= 0) {
            return AbilityResult(false, "오늘 밤에는 더 이상 공갈을 사용할 수 없습니다.")
        }
        val effectiveTarget = HackerRedirectManager.resolveTarget(game, target) ?: target
        if (effectiveTarget.member.id in gangster.threatenedTargetIdsTonight) {
            return AbilityResult(false, "이미 오늘 밤 공갈 대상으로 지정한 플레이어입니다.")
        }

        gangster.threatenedTargetIdsTonight += effectiveTarget.member.id
        gangster.remainingThreatUsesTonight -= 1

        val canTriggerCombinedAttack =
            caster.allAbilities.any { it is CombinedAttack } &&
                effectiveTarget.member.id in gangster.threatenedTargetIdsLastNight &&
                !effectiveTarget.state.isDead
        if (canTriggerCombinedAttack) {
            gangster.remainingThreatUsesTonight += 1
        }

        return AbilityResult(true, "${target.member.effectiveName}님을 협박했습니다.")
    }
}
