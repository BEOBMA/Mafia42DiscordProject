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
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(139).webp"
    override val targetJob: List<KClass<out Job>> = listOf(Agent::class)

    companion object {
        suspend fun notifyIfTriggered(game: Game, caster: PlayerData, target: PlayerData, ability: ActiveAbility) {
            val targetAgentJob = target.job as? Agent ?: return
            if (caster.member.id !in targetAgentJob.discoveredCitizenTargetIds) return
            if (target.allAbilities.none { it is Humint }) return
            val discoveredDay = targetAgentJob.discoveredCitizenTargetDayById[caster.member.id] ?: return
            if (discoveredDay >= game.dayCount) return

            val targetMessage = "${caster.member.effectiveName}님에게 당신의 정보를 공유했습니다!"
            val casterMessage = "요원 ${target.member.effectiveName}님이 자신의 정보를 전달 하였습니다."
            runCatching {
                target.member.getDmChannel().createMessage(targetMessage)
                caster.member.getDmChannel().createMessage(casterMessage)
            }
        }
    }
}
