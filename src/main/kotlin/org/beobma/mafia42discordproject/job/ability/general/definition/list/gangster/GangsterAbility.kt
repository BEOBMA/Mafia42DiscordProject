package org.beobma.mafia42discordproject.job.ability.general.definition.list.gangster

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.definition.list.Gangster

class GangsterAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "공갈"
    override val description: String = "밤마다 플레이어 한 명을 선택하여 다음날 투표시 해당 플레이어의 투표권을 빼앗는다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484607046031368254/5e5eb5f7c6d3aef1.png?ex=69bed78f&is=69bd860f&hm=f06b2a4f03b4c01713fdbf1cc2bff66131a5f2517ab8af6fba0c27868c3e6f2d&"
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
        if (target.member.id in gangster.threatenedTargetIdsTonight) {
            return AbilityResult(false, "이미 오늘 밤 공갈 대상으로 지정한 플레이어입니다.")
        }

        gangster.threatenedTargetIdsTonight += target.member.id
        gangster.remainingThreatUsesTonight -= 1

        val canTriggerCombinedAttack =
            caster.allAbilities.any { it is CombinedAttack } &&
                target.member.id in gangster.threatenedTargetIdsLastNight &&
                !target.state.isDead
        if (canTriggerCombinedAttack) {
            gangster.remainingThreatUsesTonight += 1
        }

        val remainingUseText = if (gangster.remainingThreatUsesTonight > 0) {
            " (추가 사용 가능 횟수: ${gangster.remainingThreatUsesTonight}회)"
        } else {
            ""
        }

        return AbilityResult(true, "${target.member.effectiveName}님을 공갈 대상으로 지정했습니다.$remainingUseText")
    }
}
