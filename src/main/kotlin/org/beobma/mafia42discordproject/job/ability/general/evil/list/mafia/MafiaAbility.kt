package org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.AttackEvent
import org.beobma.mafia42discordproject.game.system.AttackTier
import org.beobma.mafia42discordproject.game.system.HackerRedirectManager
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.definition.list.Agent
import org.beobma.mafia42discordproject.job.definition.list.Couple
import org.beobma.mafia42discordproject.job.definition.list.Detective
import org.beobma.mafia42discordproject.job.definition.list.Police
import org.beobma.mafia42discordproject.job.definition.list.Vigilante
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import org.beobma.mafia42discordproject.job.evil.list.Thief

class MafiaAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "처형"
    override val description: String = "밤마다 한 명의 플레이어를 죽일 수 있으며 마피아끼리 대화가 가능하다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484619430263521330/930fdad0076c57b6.png?ex=69bee317&is=69bd9197&hm=a0fb01e23944eb5f0cab3ad595509ef41f6716f9a897e2f1a204de8c083a7d3e&"
    override val usablePhase: GamePhase = GamePhase.NIGHT

    private val attackKey = "MAFIA_TEAM"

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "처형은 밤에만 사용할 수 있습니다.")
        }
        if (caster.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 능력을 사용할 수 없습니다.")
        }
        if (target == null) {
            return AbilityResult(false, "처형할 대상을 지정해야 합니다.")
        }
        if (target.state.isDead) {
            return AbilityResult(false, "이미 사망한 플레이어는 처형 대상으로 지정할 수 없습니다.")
        }

        val casterJob = caster.job
            ?: return AbilityResult(false, "시전자 직업 정보를 확인할 수 없습니다.")
        var attackTier = AttackTier.NORMAL

        if (caster.allAbilities.any { it is Outlaw } && isPoliceLine(target)) {
            attackTier = AttackTier.PIERCE
        }

        if (caster.allAbilities.any { it is Sniper } && game.mafiaAttackFailedPreviousNight) {
            attackTier = AttackTier.PIERCE
        }

        if (casterJob.abilities.any { it::class == WinOrDead::class } && canUseWinOrDead(game, caster)) {
            attackTier = AttackTier.PIERCE
            caster.state.hasUsedOneTimeAbility = true
        }
        if (casterJob is Thief && casterJob.hasSuccessor() && game.playerDatas.none { !it.state.isDead && it.job is Mafia }) {
            attackTier = AttackTier.ABSOLUTE
        }

        val hasNightRaid = caster.allAbilities.any { it.name == "야습" }
        if (game.dayCount == 1 && target.job is org.beobma.mafia42discordproject.job.definition.list.Doctor && hasNightRaid) {
            attackTier = AttackTier.ABSOLUTE
        }

        val effectiveTarget = HackerRedirectManager.resolveTarget(game, target) ?: target
        val redirectedTarget = resolveCoupleRedirectTarget(game, effectiveTarget)
        if (redirectedTarget != target) {
            game.coupleSacrificeMap[redirectedTarget.member.id] = target.member.id
        }

        val previousTarget = game.nightAttacks[attackKey]?.target
        if (previousTarget != null && previousTarget != redirectedTarget) {
            game.nightDeathCandidates.remove(previousTarget)
            game.coupleSacrificeMap.remove(previousTarget.member.id)
        }

        game.nightAttacks[attackKey] = AttackEvent(
            attacker = caster,
            target = redirectedTarget,
            attackTier = attackTier
        )
        if (redirectedTarget !in game.nightDeathCandidates) {
            game.nightDeathCandidates += redirectedTarget
        }

        return AbilityResult(true, "${target.member.effectiveName} 님을 처형 대상으로 지정했습니다.")
    }

    private fun resolveCoupleRedirectTarget(game: Game, originalTarget: PlayerData): PlayerData {
        val targetCouple = originalTarget.job as? Couple ?: return originalTarget
        val partnerId = targetCouple.pairedPlayerId ?: return originalTarget
        val partner = game.getPlayer(partnerId) ?: return originalTarget
        if (partner.state.isDead) return originalTarget
        return partner
    }

    private fun isPoliceLine(target: PlayerData): Boolean {
        val targetJob = target.job ?: return false
        return targetJob is Police || targetJob is Detective || targetJob is Agent || targetJob is Vigilante
    }

    private fun canUseWinOrDead(game: Game, caster: PlayerData): Boolean {
        if (caster.job !is Mafia) return false
        if (caster.state.hasUsedOneTimeAbility) return false

        val aliveMafias = game.playerDatas.filter { !it.state.isDead && it.job is Mafia }
        return aliveMafias.size == 1 && aliveMafias.first() == caster
    }
}
