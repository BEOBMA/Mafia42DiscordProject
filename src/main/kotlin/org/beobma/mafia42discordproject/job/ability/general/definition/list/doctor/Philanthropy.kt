package org.beobma.mafia42discordproject.job.ability.general.definition.list.doctor

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.DefenseTier
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.definition.list.Doctor
import org.beobma.mafia42discordproject.job.definition.list.Police
import kotlin.reflect.KClass

class Philanthropy : PassiveAbility, JobSpecificExtraAbility {
    override val name: String = "박애"
    override val description: String = "경찰계열 직업과 의사를 제외한 플레이어를 치료할 경우 모든 부가 능력을 무시하고 성공한다."
    override val targetJob: List<KClass<out Job>> = listOf(Doctor::class)

    override fun onEventObserved(game: Game, owner: PlayerData, event: GameEvent) {
        if (event is GameEvent.PlayerHealed && event.healer == owner) {

            val isTargetSelf = event.target == owner
            val isTargetPolice = event.target.job is Police

            // 박애 적용
            if (!isTargetSelf && !isTargetPolice) {
                event.defenseTier = DefenseTier.ABSOLUTE
            }
        }
    }
}