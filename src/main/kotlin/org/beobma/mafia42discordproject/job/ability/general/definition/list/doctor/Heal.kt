package org.beobma.mafia42discordproject.job.ability.general.definition.list.doctor

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.DefenseTier
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility

class Heal : ActiveAbility, JobUniqueAbility {
    override val name: String = "치료"
    override val description: String = "밤에 한 명을 치료해 일반 공격으로부터 보호합니다."
    override val usablePhase: GamePhase = GamePhase.NIGHT

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "치료는 밤에만 사용할 수 있습니다.")
        }
        if (caster.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 능력을 사용할 수 없습니다.")
        }
        if (caster.state.isSilenced) {
            return AbilityResult(false, "침묵 상태에서는 치료를 사용할 수 없습니다.")
        }
        if (caster.state.hasUsedDailyAbility) {
            return AbilityResult(false, "이번 밤에는 이미 치료 대상을 지정했습니다.")
        }
        if (target == null) {
            return AbilityResult(false, "치료할 대상을 지정해야 합니다.")
        }
        if (target.state.isDead) {
            return AbilityResult(false, "이미 사망한 플레이어는 치료할 수 없습니다.")
        }

        target.state.healTier = maxOf(target.state.healTier, DefenseTier.NORMAL)
        caster.state.hasUsedDailyAbility = true
        game.nightEvents += GameEvent.PlayerHealed(
            healer = caster,
            target = target,
            defenseTier = target.state.healTier
        )

        return AbilityResult(true, "${target.member.effectiveName}님을 치료 대상으로 지정했습니다.")
    }
}
