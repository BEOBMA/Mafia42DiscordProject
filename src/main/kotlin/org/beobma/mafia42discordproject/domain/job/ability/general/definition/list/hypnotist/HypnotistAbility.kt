package org.beobma.mafia42discordproject.job.ability.general.definition.list.hypnotist

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.*
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
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1485330690777153716/cdbffc314b3c49c3.png?ex=69c17980&is=69c02800&hm=65b5dae2d98f3a4a4435820be1a70c53fd04d5c714112aa2fa160c12cc092cce&"
    override val usablePhase: GamePhase = GamePhase.NIGHT

    companion object {
        private const val HYPNOTIZE_IMAGE_URL = "https://cdn.discordapp.com/attachments/1483977619258212392/1485045831387320331/In-klCVO6bijQqQBJJD-lm6ERnyRMxlUG9vxkwmDqVnSJ8Q-AnpOdCS_sGOCeR36BpXD9WgvMRkEPznM9kNWblpv435IpN45mGyxWOe111yhpkDJHMfB7dJcAOnLYoJURon9oIVQ63tmYOUoy9BXbg.webp?ex=69c07035&is=69bf1eb5&hm=b0d0efacb0e56c00f8179d99da286da0261dfd6f8ec606f1fe25f1e61b5c1fac&"
    }

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
        return AbilityResult(true, "${effectiveTarget.member.effectiveName}님에게 최면을 겁니다.\n$HYPNOTIZE_IMAGE_URL")
    }
}

class ReleaseHypnosisAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "최면 해제"
    override val description: String = "낮에 최면 상태인 대상들을 모두 깨워 팀 정보를 확인한다."
    override val image: String = ""
    override val usablePhase: GamePhase = GamePhase.DAY

    companion object {
        private val notificationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private const val RELEASE_IMAGE_URL = "https://cdn.discordapp.com/attachments/1483977619258212392/1485045831068418088/NcHJGKAFQM27wpur63DCKTVFPJeRl0YIq9yTtSPocI1wPy-8RN64_ZEJIjbtNmO2YrNpmXRCMxiKfNcRpxajTwx9gMtlG4jEqEv_5X0SoynqMZoFF5LM60jQ1qV6X7os7ke5jOXL33H0ylCIGDOINA.webp?ex=69c07034&is=69bf1eb4&hm=3c29f60715fcc52a9c33975b7bbaaaead33112fb0e47f47ff38dc02313528d25&"
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
        discoveries.forEach { discovery ->
            FrogCurseManager.displayedJob(discovery.target)?.let { shownJob ->
                discovery.revealedJob = shownJob
            }
        }

        hypnotist.hypnotizedTargetIds.clear()
        hypnotist.blockedNightsRemaining = 1

        discoveries.forEach { discovery ->
            game.playerDatas
                .filter { !it.state.isDead }
                .forEach { observer ->
                    observer.allAbilities
                        .filterIsInstance<PassiveAbility>()
                        .filterNot { FrogCurseManager.shouldSuppressPassive(observer) }
                        .sortedByDescending(PassiveAbility::priority)
                        .forEach { passive ->
                            passive.onEventObserved(game, observer, discovery)
                        }
                }
        }

        val releaseMessages = discoveries.map { "${it.target.member.effectiveName}님에게 걸린 최면을 해제합니다." }

        notificationScope.launch {
            JobDiscoveryNotificationManager.notifyDiscoveredTargets(discoveries)

            discoveries.filter { !it.isCancelled }.forEach { discovery ->
                val teamDescription = if (discovery.actualJob is Evil) {
                    discovery.actualJob.name
                } else {
                    "시민"
                }
                runCatching {
                    caster.member.getDmChannel().createMessage(
                        "${discovery.target.member.effectiveName}님은 ${teamDescription}입니다."
                    )
                }
            }

            if (discoveries.none { !it.isCancelled }) {
                runCatching {
                    caster.member.getDmChannel().createMessage("최면 해제 결과: 확인 가능한 생존 대상이 없습니다.")
                }
            }
        }

        val releaseResultMessage = releaseMessages.mapIndexed { index, message ->
            if (index == 0) {
                "$message\n$RELEASE_IMAGE_URL"
            } else {
                message
            }
        }.joinToString("\n")
            .ifBlank { "최면 해제 결과: 해제 가능한 생존 대상이 없습니다." }

        return AbilityResult(true, releaseResultMessage)
    }
}
