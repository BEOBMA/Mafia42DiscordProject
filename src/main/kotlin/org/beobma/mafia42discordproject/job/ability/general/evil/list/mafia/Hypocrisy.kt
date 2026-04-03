package org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.definition.list.Doctor
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import kotlin.reflect.KClass

class Hypocrisy : PassiveAbility, JobSpecificExtraAbility {
    override val name: String = "위선"
    override val description: String = "첫번째 밤에 시민팀에 의해 직업을 조사받을 경우, 의사 직업으로 판정된다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(158).webp"
    override val targetJob: List<KClass<out Job>> = listOf(Mafia::class)

    override fun onEventObserved(game: Game, owner: PlayerData, event: GameEvent) {
        if (game.dayCount != 1) return

        when (event) {
            is GameEvent.PoliceSearchResolved -> {
                if (event.target != owner) return
                event.isMafia = false
            }

            is GameEvent.PoliceJobRevealed -> {
                if (event.target != owner) return
                event.revealedJob = Doctor()
                event.isFalsified = true
                event.note = ""
            }
            is GameEvent.JobDiscovered -> {
                if (event.target != owner) return
                if (event.sourceAbilityName != "이슈") return
                event.revealedJob = Doctor()
                event.isFalsified = true
                event.note = ""
            }

            else -> Unit
        }
    }
}
