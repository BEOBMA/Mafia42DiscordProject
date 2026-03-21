package org.beobma.mafia42discordproject.job.ability.general.definition.list.hypnotist

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.DiscoveryStep
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.definition.list.Hypnotist
import kotlin.reflect.KClass

class Hint : Ability, JobSpecificExtraAbility, PassiveAbility {
    override val name: String = "암시"
    override val description: String = "최면 대상이 사망할 경우, 직업을 알아낸다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484608906784280717/bc7389fe0d0a070b.png?ex=69bed94a&is=69bd87ca&hm=2ef2f74259e68b0d6fecf17a96695bbeb8d2d487aefbc9d8d780cde5a63106f8&"
    override val targetJob: List<KClass<out Job>> = listOf(Hypnotist::class)
    override val priority: Int = 20

    override fun onEventObserved(game: Game, owner: PlayerData, event: GameEvent) {
        if (event !is GameEvent.PlayerDied) return
        if (event.isLynch) return
        if (owner.state.isDead) return

        val hypnotist = owner.job as? Hypnotist ?: return
        if (event.victim.member.id !in hypnotist.hypnotizedTargetIds) return

        val actualJob = event.victim.job ?: return
        hypnotist.hypnotizedTargetIds.remove(event.victim.member.id)

        game.nightEvents += GameEvent.JobDiscovered(
            discoverer = owner,
            target = event.victim,
            actualJob = actualJob,
            revealedJob = actualJob,
            sourceAbilityName = name,
            resolvedAt = DiscoveryStep.DAWN,
            notifyTarget = false
        )
    }
}
