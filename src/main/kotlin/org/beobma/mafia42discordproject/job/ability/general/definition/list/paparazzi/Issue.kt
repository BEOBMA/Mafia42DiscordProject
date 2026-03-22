package org.beobma.mafia42discordproject.job.ability.general.definition.list.paparazzi

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.GameEvent
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

    override fun onEventObserved(game: Game, owner: PlayerData, event: GameEvent) {
        val discovery = event as? GameEvent.JobDiscovered ?: return
        if (owner.state.isDead) return
        if (discovery.isCancelled || discovery.isPublicReveal || discovery.sharedByPaparazzi) return
        if (discovery.discoverer == discovery.target) return
        if (discovery.discoverer.job !is Definition) return
        if (owner.state.lastPaparazziIssueDay != game.dayCount) {
            owner.state.pendingPaparazziIssuePriority = null
            owner.state.pendingPaparazziIssueEvent = null
        }

        val discovererJob = discovery.discoverer.job ?: return
        val triggeredByTact = owner.allAbilities.any { it is Tact } && discovery.target == owner
        val priority = resolveIssuePriority(game, discovery, triggeredByTact)

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

        val existingPriority = owner.state.pendingPaparazziIssuePriority
        if (existingPriority != null) {
            if (priority >= existingPriority) return
            owner.state.pendingPaparazziIssueEvent?.isCancelled = true
        }

        owner.state.lastPaparazziIssueDay = game.dayCount
        owner.state.pendingPaparazziIssuePriority = priority
        owner.state.pendingPaparazziIssueEvent = sharedEvent
        game.nightEvents += sharedEvent
    }

    private fun resolveIssuePriority(
        game: Game,
        discovery: GameEvent.JobDiscovered,
        triggeredByTact: Boolean
    ): Int {
        val ability = discovery.sourceAbilityName ?: return 900

        if (game.dayCount == 1 && discovery.resolvedAt == org.beobma.mafia42discordproject.game.system.DiscoveryStep.NIGHT) {
            return when (ability) {
                "불침번", "처방" -> 110
                "특종" -> 120
                else -> 190
            }
        }

        return when {
            discovery.resolvedAt == org.beobma.mafia42discordproject.game.system.DiscoveryStep.NIGHT -> when (ability) {
                "해킹" -> 210
                "특종", "성불", "영장", "신앙", "추적", "청부", "휴민트" -> 270
                "불침번", "처방", "수색" -> 275
                else -> 290
            }
            discovery.resolvedAt == org.beobma.mafia42discordproject.game.system.DiscoveryStep.DAWN -> when (ability) {
                "도굴" -> 320
                "검진", "검시", "트릭", "투시", "암시" -> 350
                else -> 390
            }
            else -> when (ability) {
                "공작" -> if (triggeredByTact) 330 else 335
                "불침번" -> 410
                "암시" -> 420
                else -> 490
            }
        }
    }
}
