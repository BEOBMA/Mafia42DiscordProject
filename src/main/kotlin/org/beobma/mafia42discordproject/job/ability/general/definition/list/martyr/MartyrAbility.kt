package org.beobma.mafia42discordproject.job.ability.general.definition.list.martyr

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.HackerRedirectManager
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.definition.list.Martyr

class MartyrNightBombAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "자폭"
    override val description: String = "게임당 한 번, 마피아를 지목하고 있는 상태에서 마피아에게 처형당하거나 투표로 인해 처형될 때 적 팀을 지목했다면 대상과 함께 사망한다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1485331562651324536/270b34e4e328a25f.png?ex=69c17a50&is=69c028d0&hm=e3b5c99285275043802cbe3a7cf1fb2c2ad972047bd8ab03eccf325e4e753a58&"
    override val usablePhase: GamePhase = GamePhase.NIGHT

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "자폭은 밤에만 사용할 수 있습니다.")
        }
        if (caster.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 능력을 사용할 수 없습니다.")
        }
        if (target == null) {
            return AbilityResult(false, "지목할 대상을 지정해야 합니다.")
        }
        if (target.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 지목할 수 없습니다.")
        }
        if (target.member.id == caster.member.id) {
            return AbilityResult(false, "자기 자신은 지목할 수 없습니다.")
        }

        val martyr = caster.job as? Martyr
            ?: return AbilityResult(false, "테러리스트만 사용할 수 있습니다.")
        val effectiveTarget = HackerRedirectManager.resolveTarget(game, target) ?: target
        val isChanged = martyr.nightBombTargetId != null && martyr.nightBombTargetId != effectiveTarget.member.id
        martyr.nightBombTargetId = effectiveTarget.member.id

        return AbilityResult(
            true,
            "${target.member.effectiveName}님을 밤 자폭 지목 대상으로 ${if (isChanged) "변경" else "지정"}했습니다."
        )
    }
}

class MartyrDefenseBombAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "산화"
    override val description: String = "최후의 반론 시간에 플레이어 한 명을 지목합니다. 처형이 확정되면 지목한 대상과 함께 사망합니다."
    override val image: String = ""
    override val usablePhase: GamePhase = GamePhase.VOTE

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "산화는 투표 단계에서만 사용할 수 있습니다.")
        }
        if (game.defenseTargetId != caster.member.id) {
            return AbilityResult(false, "최후의 반론 대상자일 때만 산화를 지정할 수 있습니다.")
        }
        if (caster.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 능력을 사용할 수 없습니다.")
        }
        if (target == null) {
            return AbilityResult(false, "지목할 대상을 지정해야 합니다.")
        }
        if (target.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 지목할 수 없습니다.")
        }
        if (target.member.id == caster.member.id) {
            return AbilityResult(false, "자기 자신은 지목할 수 없습니다.")
        }

        val martyr = caster.job as? Martyr
            ?: return AbilityResult(false, "테러리스트만 사용할 수 있습니다.")
        val effectiveTarget = HackerRedirectManager.resolveTarget(game, target) ?: target
        val isChanged = martyr.defenseBombTargetId != null && martyr.defenseBombTargetId != effectiveTarget.member.id
        martyr.defenseBombTargetId = effectiveTarget.member.id

        return AbilityResult(
            true,
            "${target.member.effectiveName}님을 산화 지목 대상으로 ${if (isChanged) "변경" else "지정"}했습니다."
        )
    }
}
