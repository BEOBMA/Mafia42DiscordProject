package org.beobma.mafia42discordproject.job.ability.general.definition.list.agent

import dev.kord.core.behavior.channel.createMessage
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Agent
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.player.PlayerData
import kotlin.reflect.KClass

class Humint : Ability, JobSpecificExtraAbility {
    override val name: String = "휴민트"
    override val description: String = "지령 대상이 다음 낮부터 요원에게 능력을 사용할 경우 요원의 정보를 전달한다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484603377789964451/08bd04964fe477d4.png?ex=69bed423&is=69bd82a3&hm=5e92454d48ea6ce205fdd5f70287f2dc451ef80e501eef6a5c0aa3f443545bfe&"
    override val targetJob: List<KClass<out Job>> = listOf(Agent::class)

    companion object {
        suspend fun notifyIfTriggered(game: Game, caster: PlayerData, target: PlayerData, ability: ActiveAbility) {
            val targetAgentJob = target.job as? Agent ?: return
            if (caster.member.id !in targetAgentJob.discoveredCitizenTargetIds) return
            if (target.allAbilities.none { it is Humint }) return
            val discoveredDay = targetAgentJob.discoveredCitizenTargetDayById[caster.member.id] ?: return
            if (discoveredDay >= game.dayCount) return

            val message = "${target.member.effectiveName}님은 요원입니다. (${ability.name} 사용으로 휴민트 발동)"
            runCatching {
                caster.member.getDmChannel().createMessage(message)
            }
        }
    }
}
