package org.beobma.mafia42discordproject.job.ability.general.definition.list.vigilante

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.AttackEvent
import org.beobma.mafia42discordproject.game.system.AttackTier
import org.beobma.mafia42discordproject.game.system.DiscoveryStep
import org.beobma.mafia42discordproject.game.system.FrogCurseManager
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.game.system.HackerRedirectManager
import org.beobma.mafia42discordproject.game.system.InvestigationTeam
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.definition.list.Vigilante
import org.beobma.mafia42discordproject.job.evil.Evil
import org.beobma.mafia42discordproject.job.evil.list.Thief

class VigilantePurgeDayAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "숙청"
    override val description: String = "게임 당 한 번, 낮에 플레이어 한 명을 선택해 마피아 여부를 알아낼 수 있으며 밤에 알고 있는 적팀을 처형할 수 있다. (1회용)"
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/vigilante_purge.webp"
    override val usablePhase: GamePhase = GamePhase.DAY

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase && !(caster.job is Thief && game.currentPhase == GamePhase.VOTE)) {
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
        val thief = caster.job as? Thief
        if (vigilante == null && thief == null) {
            return AbilityResult(false, "자경단원 또는 숙청 능력을 훔친 도둑만 사용할 수 있습니다.")
        }

        if (vigilante?.fixedPurgeTargetId != null || thief?.stolenPoliceSearchedTargetIds?.isNotEmpty() == true) {
            return AbilityResult(false, "한번 정한 숙청 대상은 변경할 수 없습니다.")
        }

        val effectiveTarget = HackerRedirectManager.resolveTarget(game, target) ?: target
        val searchEvent = GameEvent.PoliceSearchResolved(
            police = caster,
            target = effectiveTarget,
            isMafia = InvestigationTeam.isMafia(effectiveTarget)
        )
        dispatchPassiveEvent(game, searchEvent)

        vigilante?.fixedPurgeTargetId = effectiveTarget.member.id
        vigilante?.hasDiscoveredMafiaTarget = searchEvent.isMafia
        vigilante?.discoveredMafiaDayCount = if (searchEvent.isMafia) game.dayCount else null
        thief?.stolenPoliceSearchedTargetIds?.add(effectiveTarget.member.id)

        return if (searchEvent.isMafia) {
            dispatchMafiaDiscoveryEvent(game, caster, effectiveTarget)
            AbilityResult(
                true,
                "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(4).webp\n${target.member.effectiveName}님은 마피아 입니다."
            )
        } else {
            AbilityResult(true, "${target.member.effectiveName}님은 마피아가 아닙니다.")
        }
    }

    private fun dispatchMafiaDiscoveryEvent(game: Game, caster: PlayerData, target: PlayerData) {
        val targetJob = target.job ?: return
        val discoveryEvent = GameEvent.JobDiscovered(
            discoverer = caster,
            target = target,
            actualJob = targetJob,
            revealedJob = FrogCurseManager.displayedJob(target) ?: targetJob,
            sourceAbilityName = name,
            resolvedAt = DiscoveryStep.DAY,
            notifyTarget = false,
            imageUrl = image
        )
        dispatchPassiveEvent(game, discoveryEvent)
    }

    private fun dispatchPassiveEvent(game: Game, event: GameEvent) {
        game.playerDatas
            .filter { !it.state.isDead }
            .forEach { player ->
                player.allAbilities
                    .filterIsInstance<PassiveAbility>()
                    .filterNot { FrogCurseManager.shouldSuppressPassive(player) }
                    .sortedByDescending(PassiveAbility::priority)
                    .forEach { passive ->
                        passive.onEventObserved(game, player, event)
                    }
            }
    }
}

class VigilantePurgeNightAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "숙청"
    override val description: String = "게임 당 한 번, 낮에 플레이어 한 명을 선택해 마피아 여부를 알아낼 수 있으며 밤에 알고 있는 적팀을 처형할 수 있다. (1회용)"
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
        val thief = caster.job as? Thief
        if (vigilante == null && thief == null) {
            return AbilityResult(false, "자경단원 또는 숙청 능력을 훔친 도둑만 사용할 수 있습니다.")
        }

        if (vigilante?.hasUsedNightPurge == true || thief?.hasUsedStolenVigilanteNightPurge == true) {
            return AbilityResult(false, "이미 숙청 처형을 사용했습니다.")
        }

        val fixedTarget = vigilante?.fixedPurgeTargetId?.let(game::getPlayer)
        val selectedTarget = target ?: fixedTarget
            ?: return AbilityResult(false, "숙청할 적팀 대상을 지정해야 합니다.")

        if (selectedTarget.state.isDead) {
            return AbilityResult(false, "숙청 대상이 이미 사망했습니다.")
        }

        if (!isKnownEnemyTarget(caster, selectedTarget, vigilante, thief)) {
            return AbilityResult(false, "알고 있는 적팀만 숙청할 수 있습니다.")
        }

        val attackKey = "VIGILANTE_${caster.member.id}"
        game.nightAttacks[attackKey] = AttackEvent(
            attacker = caster,
            target = selectedTarget,
            attackTier = AttackTier.NORMAL
        )
        if (selectedTarget !in game.nightDeathCandidates) {
            game.nightDeathCandidates += selectedTarget
        }

        vigilante?.hasUsedNightPurge = true
        thief?.hasUsedStolenVigilanteNightPurge = true
        return AbilityResult(true, "${selectedTarget.member.effectiveName} 님을 숙청 대상으로 지정했습니다.")
    }

    private fun isKnownEnemyTarget(
        caster: PlayerData,
        target: PlayerData,
        vigilante: Vigilante?,
        thief: Thief?
    ): Boolean {
        val isDiscoveredMafiaTarget = vigilante != null &&
            target.member.id == vigilante.fixedPurgeTargetId &&
            vigilante.hasDiscoveredMafiaTarget &&
            vigilante.discoveredMafiaDayCount != null
        if (isDiscoveredMafiaTarget) return true
        if (
            thief != null &&
            target.member.id in thief.stolenPoliceSearchedTargetIds &&
            target.job !is Evil
        ) {
            return true
        }

        return target.state.isJobPubliclyRevealed && isEnemy(caster, target)
    }

    private fun isEnemy(caster: PlayerData, target: PlayerData): Boolean {
        val targetJob = target.job ?: return false
        return (caster.job is Evil) != (targetJob is Evil)
    }
}
