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
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484619163698860113/d8e4dda05a4ee88a.png?ex=69bee2d7&is=69bd9157&hm=4168923b5a3fc683c9dc587a04b7486c1c6557ca252650b9d57198cc6dc9d83e&"
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
