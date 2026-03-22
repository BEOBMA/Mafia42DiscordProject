package org.beobma.mafia42discordproject.job.ability.general.definition.list.vigilante

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.AttackEvent
import org.beobma.mafia42discordproject.game.system.AttackTier
import org.beobma.mafia42discordproject.game.system.HackerRedirectManager
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.definition.list.Vigilante
import org.beobma.mafia42discordproject.job.evil.Evil

class VigilantePurgeDayAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "숙청"
    override val description: String = "게임 당 한 번, 낮에 플레이어 한 명을 선택해 마피아 여부를 알아낼 수 있으며 밤에 마피아를 처형할 수 있다. (1회용)"
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1485335685874319571/0dd66cee3c895a3b.png?ex=69c17e27&is=69c02ca7&hm=b40b73ecb6b9b11e96704a1730b1591492be3f9d7653b73b858d497fe20d5559&"
    override val usablePhase: GamePhase = GamePhase.DAY

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "숙청은 낮에만 사용할 수 있습니다.")
        }
        if (caster.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 능력을 사용할 수 없습니다.")
        }
        if (target == null) {
            return AbilityResult(false, "확인할 대상을 지정해야 합니다.")
        }
        if (target.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 대상으로 지정할 수 없습니다.")
        }
        if (target.member.id == caster.member.id) {
            return AbilityResult(false, "자기 자신은 대상으로 지정할 수 없습니다.")
        }

        val vigilante = caster.job as? Vigilante
            ?: return AbilityResult(false, "자경단원만 숙청 능력을 사용할 수 있습니다.")

        if (vigilante.fixedPurgeTargetId != null) {
            return AbilityResult(false, "한번 정한 숙청 대상은 변경할 수 없습니다.")
        }

        val effectiveTarget = HackerRedirectManager.resolveTarget(game, target) ?: target
        vigilante.fixedPurgeTargetId = effectiveTarget.member.id
        vigilante.hasDiscoveredMafiaTarget = effectiveTarget.job is Evil
        vigilante.discoveredMafiaDayCount = if (vigilante.hasDiscoveredMafiaTarget) game.dayCount else null

        return if (vigilante.hasDiscoveredMafiaTarget) {
            AbilityResult(true, "https://cdn.discordapp.com/attachments/1483977619258212392/1485082451805208676/aksOkxuJUtWZGkfpGyP0L7hIsxVe4sWckEIp9cB6PoO0SmfBOVaBQMdikO-qQ244nZVVz4r6ZINVGL8J1CyU5T-bEpDF3xMbPaWKonTVcZNXT-K8ejtqhAkc9YjxCiQQuvg7kzO0rzOZA8JdCoBdwA.webp?ex=69c09250&is=69bf40d0&hm=c329d1bc08a98011a6d4471968f820efe5ad9fed36647d5a5e3aaeecd04b61a9&\n${target.member.effectiveName}님은 마피아 입니다.")
        } else {
            AbilityResult(true, "${target.member.effectiveName}님은 마피아가 아닙니다.")
        }
    }
}

class VigilantePurgeNightAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "숙청"
    override val description: String = "낮에 찾아낸 마피아를 밤에 다시 숙청해 처형한다."
    override val image: String = ""
    override val usablePhase: GamePhase = GamePhase.NIGHT

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "숙청 처형은 밤에만 사용할 수 있습니다.")
        }
        if (caster.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 능력을 사용할 수 없습니다.")
        }
        if (caster.state.isSilenced) {
            return AbilityResult(false, "침묵 상태에서는 숙청 대상을 지정할 수 없습니다.")
        }

        val vigilante = caster.job as? Vigilante
            ?: return AbilityResult(false, "자경단원만 숙청 능력을 사용할 수 있습니다.")

        if (vigilante.hasUsedNightPurge) {
            return AbilityResult(false, "이미 숙청 처형을 사용했습니다.")
        }

        val fixedTargetId = vigilante.fixedPurgeTargetId
            ?: return AbilityResult(false, "낮에 먼저 숙청 대상을 선택해야 합니다.")

        if (!vigilante.hasDiscoveredMafiaTarget) {
            return AbilityResult(false, "낮에 마피아를 발견한 경우에만 밤 숙청 처형을 사용할 수 있습니다.")
        }

        val discoveredDayCount = vigilante.discoveredMafiaDayCount
        if (discoveredDayCount == null || discoveredDayCount != game.dayCount) {
            return AbilityResult(false, "숙청 처형은 마피아를 찾아낸 그날 밤에만 사용할 수 있습니다.")
        }

        val fixedTarget = game.getPlayer(fixedTargetId)
            ?: return AbilityResult(false, "숙청 대상 정보를 확인할 수 없습니다.")

        if (fixedTarget.state.isDead) {
            return AbilityResult(false, "숙청 대상이 이미 사망했습니다.")
        }

        if (target != null && target.member.id != fixedTargetId) {
            return AbilityResult(false, "낮에 지정한 숙청 대상에게만 사용할 수 있습니다.")
        }

        val attackKey = "VIGILANTE_${caster.member.id}"
        game.nightAttacks[attackKey] = AttackEvent(
            attacker = caster,
            target = fixedTarget,
            attackTier = AttackTier.NORMAL
        )
        if (fixedTarget !in game.nightDeathCandidates) {
            game.nightDeathCandidates += fixedTarget
        }

        vigilante.hasUsedNightPurge = true
        return AbilityResult(true, "${fixedTarget.member.effectiveName} 님을 숙청 대상으로 지정했습니다.")
    }
}
