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
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(126).webp"
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
                        owner.member.getDmChannel().createMessage("검시 능력을 통해 비밀결사의 사망을 확인하였습니다.")
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
