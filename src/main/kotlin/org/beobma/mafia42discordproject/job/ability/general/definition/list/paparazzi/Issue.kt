package org.beobma.mafia42discordproject.job.ability.general.definition.list.paparazzi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.game.system.JobDiscoveryNotificationManager
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.definition.Definition
import org.beobma.mafia42discordproject.job.definition.list.Paparazzi
import kotlin.reflect.KClass

class Issue : Ability, JobSpecificExtraAbility, PassiveAbility {
    override val name: String = "이슈"
    override val description: String = "하루에 한 번 시민 팀이 다른 사람의 직업을 알아낼 경우, 그 정보를 공유받는다."
    override val image: String =
        "https://cdn.discordapp.com/attachments/1483977619258212392/1485257876695023647/F-kyMiMJma9PeOAoICSIbPsT2DWonhkr3jLQaZgz6vhn6v21l8jFDHyg11RRzd_w_e2eEY_OTv3LyMcfQzxlL3TooyTcFywRt8T6QxcTnUXy_ThxR1HHgGc676kTwX9gccQGv4xCCJKwOf2KomdNOQ.webp?ex=69c135b0&is=69bfe430&hm=a894f77e46d163ad75413cde94aaa33e55d01468c80f676a9158acb6d292d137&"
    override val targetJob: List<KClass<out Job>> = listOf(Paparazzi::class)

    companion object {
        private val issueNotificationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    override fun onEventObserved(game: Game, owner: PlayerData, event: GameEvent) {
        val discovery = event as? GameEvent.JobDiscovered ?: return
        if (owner.state.isDead) return
        if (discovery.isCancelled || discovery.isPublicReveal || discovery.sharedByPaparazzi) return
        if (discovery.discoverer == discovery.target) return
        if (discovery.discoverer.job !is Definition) return
        if (owner.state.lastPaparazziIssueDay == game.dayCount) return

        val discovererJob = discovery.discoverer.job ?: return
        val triggeredByTact = owner.allAbilities.any { it is Tact } && discovery.target == owner

        val sharedEvent = if (triggeredByTact) {
            GameEvent.JobDiscovered(
                discoverer = owner,
                target = discovery.discoverer,
                actualJob = discovererJob,
                revealedJob = discovererJob,
                sourceAbilityName = name,
                resolvedAt = discovery.resolvedAt,
                sharedByPaparazzi = true,
                triggeredByTact = true,
                notifyTarget = false,
                imageUrl = image
            )
        } else {
            GameEvent.JobDiscovered(
                discoverer = owner,
                target = discovery.target,
                actualJob = discovery.actualJob,
                revealedJob = discovery.revealedJob,
                sourceAbilityName = name,
                resolvedAt = discovery.resolvedAt,
                sharedByPaparazzi = true,
                notifyTarget = false,
                imageUrl = image
            )
        }

        owner.state.lastPaparazziIssueDay = game.dayCount
        issueNotificationScope.launch {
            JobDiscoveryNotificationManager.notifyDiscoveredTargets(listOf(sharedEvent), game)
        }
    }
}
