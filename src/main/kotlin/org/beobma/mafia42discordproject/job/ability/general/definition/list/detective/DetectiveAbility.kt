package org.beobma.mafia42discordproject.job.ability.general.definition.list.detective

import dev.kord.core.behavior.channel.createMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.HackerRedirectManager
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.definition.list.Detective

class DetectiveAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "추리"
    override val description: String = "밤마다 플레이어 한 명을 선택하여 그 플레이어가 누구에게 능력을 사용하였는지 알아낼 수 있다."
    override val image: String = ""
    override val usablePhase: GamePhase = GamePhase.NIGHT

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "추리는 밤에만 사용할 수 있습니다.")
        }
        if (caster.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 능력을 사용할 수 없습니다.")
        }
        if (target == null) {
            return AbilityResult(false, "추리할 대상을 지정해야 합니다.")
        }
        if (target.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 추리 대상으로 지정할 수 없습니다.")
        }

        val detective = caster.job as? Detective
            ?: return AbilityResult(false, "사립탐정만 사용할 수 있습니다.")

        val effectiveTarget = HackerRedirectManager.resolveTarget(game, target) ?: target
        val existingTargetId = detective.fixedReasoningTargetId
        if (existingTargetId != null && existingTargetId != effectiveTarget.member.id) {
            return AbilityResult(false, "한번 정한 추리 대상은 변경할 수 없습니다.")
        }

        detective.fixedReasoningTargetId = effectiveTarget.member.id
        return AbilityResult(true, "${target.member.effectiveName}님을 추리 대상으로 지정했습니다.")
    }

    companion object {
        private val detectiveDmScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        fun resetNightState(owner: PlayerData) {
            val detective = owner.job as? Detective ?: return
            detective.trapTriggeredTargetIdsThisNight.clear()
        }

        suspend fun notifyTargetSelection(
            game: Game,
            caster: PlayerData,
            selectedTarget: PlayerData,
            usedAbility: ActiveAbility
        ) {
            if (game.currentPhase != GamePhase.NIGHT) return
            if (caster.state.isDead) return

            val aliveDetectives = game.playerDatas.filter { !it.state.isDead && it.job is Detective }
            aliveDetectives.forEach { detectivePlayer ->
                val detectiveJob = detectivePlayer.job as? Detective ?: return@forEach
                if (detectiveJob.fixedReasoningTargetId != caster.member.id) return@forEach

                sendDm(
                    detectivePlayer,
                    "추리를 시작합니다. ${usedAbility.name} 대상을 ${selectedTarget.member.effectiveName}님으로 지정했습니다."
                )

                val hasTrap = detectivePlayer.allAbilities.any { it is Trap }
                if (!hasTrap || selectedTarget.member.id != detectivePlayer.member.id) return@forEach
                if (caster.member.id in detectiveJob.trapTriggeredTargetIdsThisNight) return@forEach

                detectiveJob.trapTriggeredTargetIdsThisNight += caster.member.id
                val casterJobName = caster.job?.name ?: "알 수 없음"
                sendDm(
                    detectivePlayer,
                    "함정을 통해 직업을 알아내었습니다! 직업: $casterJobName"
                )
            }
        }

        private fun sendDm(owner: PlayerData, message: String) {
            detectiveDmScope.launch {
                runCatching {
                    owner.member.getDmChannel().createMessage(message)
                }
            }
        }
    }
}
