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
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484606515183616142/6076d5c9913f0cc0.png?ex=69bed70f&is=69bd858f&hm=e8ab2113889c7486dcdee73d63185980ada2cc1598a9b29d5bac1f6224e52747&"
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