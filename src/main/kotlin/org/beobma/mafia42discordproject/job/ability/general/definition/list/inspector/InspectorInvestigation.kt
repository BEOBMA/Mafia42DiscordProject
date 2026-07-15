package org.beobma.mafia42discordproject.job.ability.general.definition.list.inspector

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.replay.GameReplayLogger
import org.beobma.mafia42discordproject.game.system.DiscoveryStep
import org.beobma.mafia42discordproject.game.system.FrogCurseManager
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.game.system.HackerRedirectManager
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.definition.Definition
import org.beobma.mafia42discordproject.job.definition.list.Inspector

class InspectorInvestigation : ActiveAbility, JobUniqueAbility {
    override val name: String = "수사"
    override val description: String = "밤에 한 명을 지목한다. 밤이 끝날 때 해당 플레이어가 같은 팀이었다면 직업을 알아내고 자신의 직업을 전송한다. (1회용)"
    override val image: String = INSPECTOR_ABILITY_IMAGE_URL
    override val usablePhase: GamePhase = GamePhase.NIGHT

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "수사는 밤에만 사용할 수 있습니다.")
        }
        if (caster.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 능력을 사용할 수 없습니다.")
        }
        if (caster.state.isSilenced) {
            return AbilityResult(false, "유혹 상태에서는 수사 대상을 지정할 수 없습니다.")
        }

        val inspector = caster.job as? Inspector
            ?: return AbilityResult(false, "형사만 수사 능력을 사용할 수 있습니다.")

        if (inspector.hasUsedInvestigation) {
            return AbilityResult(false, "이미 수사 능력을 사용했습니다.")
        }

        if (target == null) {
            return AbilityResult(false, "수사할 대상을 지정해야 합니다.")
        }
        if (target.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 수사 대상으로 지정할 수 없습니다.")
        }

        val effectiveTarget = HackerRedirectManager.resolveTarget(game, target) ?: target
        inspector.hasUsedInvestigation = true

        if (caster.allAbilities.any { it is Emergency }) {
            return resolveEmergencyInvestigation(game, caster, effectiveTarget)
        }

        inspector.pendingInvestigationTargetId = effectiveTarget.member.id
        return AbilityResult(true, "수사 대상을 결정했습니다.")
    }

    private fun resolveEmergencyInvestigation(game: Game, caster: PlayerData, target: PlayerData): AbilityResult {
        if (target.state.isDead) {
            return AbilityResult(true, "${target.member.effectiveName}님 수사에 실패했습니다.")
        }

        val actualJob = target.job
            ?: return AbilityResult(true, "${target.member.effectiveName}님은 시민 팀이 아닙니다.")
        if (actualJob !is Definition) {
            return AbilityResult(true, "${target.member.effectiveName}님은 시민 팀이 아닙니다.")
        }

        val revealedJob = FrogCurseManager.displayedJob(target) ?: actualJob
        val event = GameEvent.JobDiscovered(
            discoverer = caster,
            target = target,
            actualJob = actualJob,
            revealedJob = revealedJob,
            sourceAbilityName = name,
            resolvedAt = DiscoveryStep.NIGHT,
            notifyTarget = false,
            imageUrl = INSPECTOR_SUCCESS_IMAGE_URL
        )
        dispatchDiscoveryEvent(game, event)
        game.pendingDayStartDiscoveries += event.copy(notifyTarget = true)
        return AbilityResult(true, "그 사람의 직업은 ${event.revealedJob.name}.")
    }

    private fun dispatchDiscoveryEvent(game: Game, event: GameEvent.JobDiscovered) {
        FrogCurseManager.displayedJob(event.target)?.let { event.revealedJob = it }
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

    companion object {
        private const val INSPECTOR_ABILITY_IMAGE_URL =
            "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/Inspector_ability_1.webp"
        private const val INSPECTOR_SUCCESS_IMAGE_URL =
            "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/Inspector_ability_1_image.webp"

        suspend fun resolveNightInvestigations(game: Game, deadPlayers: Set<PlayerData>) {
            game.playerDatas.forEach { inspectorPlayer ->
                val inspector = inspectorPlayer.job as? Inspector ?: return@forEach
                val targetId = inspector.pendingInvestigationTargetId ?: return@forEach
                inspector.pendingInvestigationTargetId = null

                val target = game.getPlayer(targetId) ?: return@forEach
                if (inspectorPlayer in deadPlayers || inspectorPlayer.state.isDead) {
                    return@forEach
                }
                if (target in deadPlayers || target.state.isDead) {
                    sendDm(game, inspectorPlayer, "${target.member.effectiveName}님 수사에 실패했습니다.", "형사 수사 실패")
                    return@forEach
                }

                val actualJob = target.job
                if (actualJob !is Definition) {
                    sendDm(game, inspectorPlayer, "${target.member.effectiveName}님은 시민 팀이 아닙니다.", "형사 수사 결과")
                    return@forEach
                }

                val revealedJob = FrogCurseManager.displayedJob(target) ?: actualJob
                game.nightEvents += GameEvent.JobDiscovered(
                    discoverer = inspectorPlayer,
                    target = target,
                    actualJob = actualJob,
                    revealedJob = revealedJob,
                    sourceAbilityName = "수사",
                    resolvedAt = DiscoveryStep.NIGHT,
                    imageUrl = INSPECTOR_SUCCESS_IMAGE_URL,
                    notifyTarget = true
                )
            }
        }

        private suspend fun sendDm(game: Game, player: PlayerData, message: String, title: String) {
            runCatching {
                GameReplayLogger.logDirectMessage(game, player, message, title)
                player.member.getDmChannel().createMessage(message)
            }
        }
    }
}
