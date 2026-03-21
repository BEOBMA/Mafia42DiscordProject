package org.beobma.mafia42discordproject.job.ability.general.definition.list.hypnotist

import dev.kord.core.behavior.channel.createMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.DiscoveryStep
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.game.system.HackerRedirectManager
import org.beobma.mafia42discordproject.game.system.JobDiscoveryNotificationManager
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.definition.list.Citizen
import org.beobma.mafia42discordproject.job.definition.list.Hypnotist
import org.beobma.mafia42discordproject.job.evil.Evil

class HypnotizeAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "최면"
    override val description: String = "밤에 플레이어 한 명을 최면 상태로 만든다. 같은 밤에는 대상을 바꿀 수 없다."
    override val image: String = ""
    override val usablePhase: GamePhase = GamePhase.NIGHT

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "최면은 밤에만 사용할 수 있습니다.")
        }
        if (caster.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 능력을 사용할 수 없습니다.")
        }

        val hypnotist = caster.job as? Hypnotist
            ?: return AbilityResult(false, "최면술사만 사용할 수 있습니다.")

        if (hypnotist.blockedNightsRemaining > 0) {
            return AbilityResult(false, "지난 낮에 최면을 해제하여 오늘 밤에는 최면을 걸 수 없습니다.")
        }

        if (target == null) {
            return AbilityResult(false, "최면을 걸 대상을 지정해야 합니다.")
        }
        if (target.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 최면 대상으로 지정할 수 없습니다.")
        }
        if (target.member.id == caster.member.id) {
            return AbilityResult(false, "자기 자신에게는 최면을 걸 수 없습니다.")
        }

        val effectiveTarget = HackerRedirectManager.resolveTarget(game, target) ?: target
        val selectedTargetIdTonight = hypnotist.selectedTargetIdTonight
        if (selectedTargetIdTonight != null && selectedTargetIdTonight != effectiveTarget.member.id) {
            return AbilityResult(false, "오늘 밤에 선택한 최면 대상은 변경할 수 없습니다.")
        }

        hypnotist.selectedTargetIdTonight = effectiveTarget.member.id
        hypnotist.hypnotizedTargetIds += effectiveTarget.member.id
        return AbilityResult(true, "${effectiveTarget.member.effectiveName}님에게 최면을 걸었습니다.")
    }
}

class ReleaseHypnosisAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "최면 해제"
    override val description: String = "낮에 최면 상태인 대상들을 모두 깨워 팀 정보를 확인한다."
    override val image: String = ""
    override val usablePhase: GamePhase = GamePhase.DAY

    companion object {
        private val notificationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "최면 해제는 낮에만 사용할 수 있습니다.")
        }
        if (caster.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 능력을 사용할 수 없습니다.")
        }

        val hypnotist = caster.job as? Hypnotist
            ?: return AbilityResult(false, "최면술사만 사용할 수 있습니다.")

        if (hypnotist.hypnotizedTargetIds.isEmpty()) {
            return AbilityResult(false, "현재 최면 상태인 대상이 없습니다.")
        }

        val discoveries = mutableListOf<GameEvent.JobDiscovered>()
        hypnotist.hypnotizedTargetIds.toList().forEach { targetId ->
            val foundTarget = game.getPlayer(targetId) ?: return@forEach
            if (foundTarget.state.isDead) return@forEach
            val actualJob = foundTarget.job ?: return@forEach
            val revealedJob = if (actualJob is Evil) actualJob else Citizen()
            discoveries += GameEvent.JobDiscovered(
                discoverer = caster,
                target = foundTarget,
                actualJob = actualJob,
                revealedJob = revealedJob,
                sourceAbilityName = name,
                resolvedAt = DiscoveryStep.DAY,
                notifyTarget = false
            )
        }

        hypnotist.hypnotizedTargetIds.clear()
        hypnotist.blockedNightsRemaining = 1

        discoveries.forEach { discovery ->
            game.playerDatas
                .filter { !it.state.isDead }
                .forEach { observer ->
                    observer.allAbilities
                        .filterIsInstance<PassiveAbility>()
                        .sortedByDescending(PassiveAbility::priority)
                        .forEach { passive ->
                            passive.onEventObserved(game, observer, discovery)
                        }
                }
        }

        notificationScope.launch {
            JobDiscoveryNotificationManager.notifyDiscoveredTargets(discoveries)
            if (discoveries.none { !it.isCancelled }) {
                runCatching {
                    caster.member.getDmChannel().createMessage("최면 해제 결과: 확인 가능한 생존 대상이 없습니다.")
                }
            }
        }

        val knownCount = discoveries.count { !it.isCancelled }
        return AbilityResult(true, "최면을 해제했습니다. 확인된 대상 수: ${knownCount}명")
    }
}
