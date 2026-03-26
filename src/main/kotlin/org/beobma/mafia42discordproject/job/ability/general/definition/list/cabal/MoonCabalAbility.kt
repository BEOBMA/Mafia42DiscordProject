package org.beobma.mafia42discordproject.job.ability.general.definition.list.cabal

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.HackerRedirectManager
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.definition.list.Cabal
import org.beobma.mafia42discordproject.job.definition.list.CabalRole

class MoonCabalAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "접선"
    override val description: String = "해 비밀결사가 달 비밀결사를 찾은 뒤 밤마다 해 비밀결사를 지목한다. 해 비밀결사를 지목하면 날이 밝은 후, 게임에서 승리한다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(5).webp"
    override val usablePhase: GamePhase = GamePhase.NIGHT

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "밤에만 사용할 수 있습니다.")
        }

        if (caster.state.isDead) {
            return AbilityResult(false, "사망한 상태에서는 사용할 수 없습니다.")
        }

        val cabal = caster.job as? Cabal
            ?: return AbilityResult(false, "비밀결사가 아닙니다.")

        if (cabal.role != CabalRole.MOON) {
            return AbilityResult(false, "달 비밀결사에게만 주어진 능력입니다.")
        }

        if (!cabal.wasFoundBySun) {
            return AbilityResult(false, "해 비밀결사가 아직 달 비밀결사를 찾지 못했습니다.")
        }

        if (target == null) {
            cabal.selectedTargetId = null
            cabal.moonMarkedSunTonight = false
            return AbilityResult(true, "접선 대상을 해제했습니다.")
        }

        if (target.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 지목할 수 없습니다.")
        }

        val effectiveTarget = HackerRedirectManager.resolveTarget(game, target) ?: target
        cabal.selectedTargetId = effectiveTarget.member.id
        val isSunTarget = effectiveTarget.member.id == cabal.pairedPlayerId
        cabal.moonMarkedSunTonight = isSunTarget

        return if (isSunTarget) {
            AbilityResult(true, "접선에 성공할 수 있도록 해 비밀결사를 지목했습니다.")
        } else {
            AbilityResult(true, "대상을 지정했습니다. 해 비밀결사를 지목해야 특수 승리가 준비됩니다.")
        }
    }
}
