package org.beobma.mafia42discordproject.job.ability.general.definition.list.other

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.DiscoveryStep
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.definition.list.Cabal
import org.beobma.mafia42discordproject.job.definition.list.Doctor
import org.beobma.mafia42discordproject.job.definition.list.Nurse
import kotlin.reflect.KClass

class Postmortem : Ability, JobSpecificExtraAbility, PassiveAbility {
    override val name: String = "검시"
    override val description: String = "자신의 능력과 관련된 접선하지 않은 직업을 가진 사람이 사망한 경우, 그 사실을 알게 된다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484604356010446869/69fc4adcdda5b9d3.png?ex=69bed50d&is=69bd838d&hm=cad42bc4a83f774ed0b9357eadcdbfef75cc4d736b46c9bdc3f65a1d39dceb74&"
    override val targetJob: List<KClass<out Job>> = listOf(Nurse::class, Cabal::class)

    companion object {
        private val postmortemDmScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    override fun onEventObserved(game: Game, owner: PlayerData, event: GameEvent) {
        if (event !is GameEvent.PlayerDied) return

        when (val job = owner.job) {
            is Cabal -> {
                val pairId = job.pairedPlayerId ?: return
                if (event.victim.member.id != pairId) return

                val met = job.hasFoundMoon || job.wasFoundBySun
                if (met) return
                if (!job.notifiedPartnerDeathIds.add(pairId)) return

                postmortemDmScope.launch {
                    runCatching {
                        owner.member.getDmChannel().createMessage("검시 발동: 접선 전 상대 비밀결사가 사망했습니다.")
                    }
                }
            }

            is Nurse -> {
                val victim = event.victim
                if (victim.job !is Doctor) return

                if (job.hasContactedDoctor) return

                val victimJob = victim.job ?: return
                game.nightEvents += GameEvent.JobDiscovered(
                    discoverer = owner,
                    target = victim,
                    actualJob = victimJob,
                    revealedJob = victimJob,
                    sourceAbilityName = "검시",
                    resolvedAt = DiscoveryStep.NIGHT,
                    notifyTarget = false
                )
            }
        }
    }
}
