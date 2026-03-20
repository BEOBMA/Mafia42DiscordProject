package org.beobma.mafia42discordproject.job.ability.general.definition.list.doctor

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.DiscoveryStep
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.definition.list.Doctor
import kotlin.reflect.KClass

class Screening : Ability, JobSpecificExtraAbility, PassiveAbility {
    override val name: String = "검진"
    override val description: String = "치료에 성공한 플레이어의 직업을 알아낸다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484606764669473000/395705b1bb0dc379.png?ex=69bed74b&is=69bd85cb&hm=407aec6e19d87e5858581c933d42ec9b597d49f8f710c7ca61a8927083a0f0eb&"
    override val targetJob: List<KClass<out Job>> = listOf(Doctor::class)

    override fun onEventObserved(game: Game, owner: PlayerData, event: GameEvent) {
        val healSucceededEvent = event as? GameEvent.HealSucceeded ?: return
        if (healSucceededEvent.healer != owner) return

        val targetJob = healSucceededEvent.target.job ?: return
        game.nightEvents += GameEvent.JobDiscovered(
            discoverer = owner,
            target = healSucceededEvent.target,
            actualJob = targetJob,
            revealedJob = targetJob,
            sourceAbilityName = name,
            resolvedAt = DiscoveryStep.DAWN
        )
    }
}
