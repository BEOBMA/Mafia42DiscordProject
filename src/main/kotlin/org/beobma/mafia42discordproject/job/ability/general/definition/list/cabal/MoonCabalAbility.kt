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
import org.beobma.mafia42discordproject.job.evil.list.Thief
import org.beobma.mafia42discordproject.job.evil.list.actualOrStolenJob

class MoonCabalAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "접선"
    override val description: String = "밤마다 플레이어 한 명을 지목해 낮이 될 때 해 비밀결사 여부를 확인한다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(94).webp"
    override val usablePhase: GamePhase = GamePhase.NIGHT

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "밤에만 사용할 수 있습니다.")
        }

        if (caster.state.isDead) {
            return AbilityResult(false, "사망한 상태에서는 사용할 수 없습니다.")
        }

        val cabal = caster.actualOrStolenJob<Cabal>()
            ?: return AbilityResult(false, "비밀결사 또는 접선 능력을 훔친 도둑이 아닙니다.")

        if (caster.job is Thief) {
            if (target == null || target.state.isDead) {
                return AbilityResult(false, "조사할 생존 플레이어를 지정해야 합니다.")
            }
            val effectiveTarget = HackerRedirectManager.resolveTarget(game, target) ?: target
            val result = if (effectiveTarget.job is Cabal) "비밀결사입니다." else "비밀결사가 아닙니다."
            cabal.selectedTargetId = effectiveTarget.member.id
            return AbilityResult(true, "${target.member.effectiveName}님은 $result")
        }

        if (cabal.role != CabalRole.MOON) {
            return AbilityResult(false, "달 비밀결사에게만 주어진 능력입니다.")
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

        return AbilityResult(true, "밀사의 대상을 ${target.member.effectiveName}님으로 지정했습니다.")
    }
}
