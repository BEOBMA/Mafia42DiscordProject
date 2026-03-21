package org.beobma.mafia42discordproject.job.ability.general.definition.list.cabal

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.definition.list.Cabal
import org.beobma.mafia42discordproject.job.definition.list.CabalRole

class SunCabalAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "밀사"
    override val description: String = "낮마다 플레이어 한 명을 지목해 밤이 될 때 달 비밀결사 여부를 확인한다."
    override val image: String = ""
    override val usablePhase: GamePhase = GamePhase.DAY

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "낮에만 사용할 수 있습니다.")
        }

        if (caster.state.isDead) {
            return AbilityResult(false, "사망한 상태에서는 사용할 수 없습니다.")
        }

        val cabal = caster.job as? Cabal
            ?: return AbilityResult(false, "비밀결사가 아닙니다.")

        if (cabal.role != CabalRole.SUN) {
            return AbilityResult(false, "해 비밀결사에게만 주어진 능력입니다.")
        }

        if (target == null) {
            cabal.selectedTargetId = null
            return AbilityResult(true, "밀사 대상을 해제했습니다.")
        }

        if (target.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 지목할 수 없습니다.")
        }

        if (target.member.id == caster.member.id) {
            return AbilityResult(false, "자기 자신은 지목할 수 없습니다.")
        }

        cabal.selectedTargetId = target.member.id
        return AbilityResult(true, "${target.member.effectiveName}님을 밀사 대상으로 지정했습니다. 낮 동안 자유롭게 변경할 수 있습니다.")
    }
}
