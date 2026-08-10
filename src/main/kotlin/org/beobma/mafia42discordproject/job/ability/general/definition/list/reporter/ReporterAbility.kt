package org.beobma.mafia42discordproject.job.ability.general.definition.list.reporter

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.DiscoveryStep
import org.beobma.mafia42discordproject.game.system.FrogCurseManager
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.game.system.HackerRedirectManager
import org.beobma.mafia42discordproject.game.system.SwindlerManager
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.definition.list.Reporter
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import org.beobma.mafia42discordproject.job.evil.list.Thief
import org.beobma.mafia42discordproject.job.evil.list.actualOrStolenJob

class ReporterAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "특종"
    override val description: String = "밤에 한 명의 플레이어를 선택하여 직업을 알아내고 낮이 될 때 기사를 내어 모든 플레이어에게 해당 사실을 알린다. (1회용)"
    override val image: String = ReporterAssets.SCOOP_ABILITY_IMAGE_URL
    override val usablePhase: GamePhase = GamePhase.NIGHT

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "특종은 밤에만 사용할 수 있습니다.")
        }
        if (caster.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 능력을 사용할 수 없습니다.")
        }
        if (caster.state.isSilenced) {
            return AbilityResult(false, "침묵 상태에서는 특종 대상을 지정할 수 없습니다.")
        }
        if (target == null) {
            return AbilityResult(false, "취재할 대상을 지정해야 합니다.")
        }
        if (target.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 취재 대상으로 지정할 수 없습니다.")
        }

        val reporter = caster.actualOrStolenJob<Reporter>()
            ?: return AbilityResult(false, "기자 또는 특종 능력을 훔친 도둑만 사용할 수 있습니다.")
        val thief = caster.job as? Thief
        val sourceReporter = thief?.stolenSourcePlayerId
            ?.let(game::getPlayer)
            ?.job as? Reporter
        if (sourceReporter?.hasUsedScoop == true) {
            return AbilityResult(false, "원래 기자가 이미 특종을 사용했습니다.")
        }

        val effectiveTarget = HackerRedirectManager.resolveTarget(game, target) ?: target
        val fixedTargetId = reporter.selectedTargetId
        if (fixedTargetId != null && fixedTargetId != effectiveTarget.member.id) {
            return AbilityResult(false, "한 번 정한 취재 대상은 변경할 수 없습니다.")
        }
        if (reporter.hasUsedScoop) {
            return AbilityResult(false, "이미 특종을 사용했습니다.")
        }

        val actualJob = effectiveTarget.job
            ?: return AbilityResult(false, "대상의 직업 정보를 확인할 수 없습니다.")
        val revealedJob = FrogCurseManager.displayedJob(effectiveTarget) ?: actualJob
        val discoveryEvent = GameEvent.JobDiscovered(
            discoverer = caster,
            target = effectiveTarget,
            actualJob = actualJob,
            revealedJob = revealedJob,
            sourceAbilityName = name,
            resolvedAt = DiscoveryStep.NIGHT,
            notifyTarget = false,
            imageUrl = ReporterAssets.PRIVATE_SCOOP_RESULT_IMAGE_URL
        )
        dispatchDiscoveryEvent(game, discoveryEvent)
        SwindlerManager.notifyFooledByDiscovery(discoveryEvent)

        reporter.selectedTargetId = effectiveTarget.member.id
        reporter.discoveredActualJobName = actualJob.name
        reporter.discoveredJobName = discoveryEvent.revealedJob.name
        reporter.discoveredImageUrl = ReporterAssets.PUBLIC_SCOOP_ARTICLE_IMAGE_URL
        reporter.articlePublishDay = if (game.dayCount == 1) 2 else game.dayCount
        reporter.articlePublishAtNightMidpoint = game.dayCount == 1 &&
            caster.allAbilities.any { it is BreakingNews } &&
            actualJob is Mafia
        reporter.hasUsedScoop = true
        return AbilityResult(
            true,
            "특종입니다! ${target.member.effectiveName}님이 ${discoveryEvent.revealedJob.name}(이)라는 소식입니다!\n${ReporterAssets.PRIVATE_SCOOP_RESULT_IMAGE_URL}"
        )
    }

    private fun dispatchDiscoveryEvent(game: Game, event: GameEvent.JobDiscovered) {
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
