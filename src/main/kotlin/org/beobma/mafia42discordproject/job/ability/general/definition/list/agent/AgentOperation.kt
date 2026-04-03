package org.beobma.mafia42discordproject.job.ability.general.definition.list.agent

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.SystemImage
import org.beobma.mafia42discordproject.game.system.FrogCurseManager
import org.beobma.mafia42discordproject.game.system.SwindlerManager
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.definition.Definition
import org.beobma.mafia42discordproject.job.definition.list.Agent
import org.beobma.mafia42discordproject.job.evil.list.Swindler

class AgentOperation : JobUniqueAbility, PassiveAbility {
    override val name: String = "공작"
    override val description: String = "낮마다 지령을 받아 시민 한 명의 직업을 알아낸다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(12).png"

    companion object {
        private val agentDmScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    override fun onPhaseChanged(game: Game, owner: PlayerData, newPhase: GamePhase) {
        if (newPhase != GamePhase.DAY || owner.state.isDead) return

        val agentJob = owner.job as? Agent ?: return
        val candidates = game.playerDatas
            .asSequence()
            .filter { !it.state.isDead }
            .filter { it.member.id != owner.member.id }
            .filter { it.job is Definition }
            .filter { !it.state.isJobPubliclyRevealed }
            .filter { it.member.id !in agentJob.discoveredCitizenTargetIds }
            .toList()

        val selectedTarget = candidates.shuffled().firstOrNull()
        if (selectedTarget == null) {
            sendDm(owner, "지령이 도착하지 않았습니다.")
            return
        }

        val discoveredJob = FrogCurseManager.displayedJob(selectedTarget) ?: run {
            sendDm(owner, "지령이 도착하지 않았습니다.")
            return
        }

        agentJob.discoveredCitizenTargetIds += selectedTarget.member.id
        agentJob.discoveredCitizenTargetDayById[selectedTarget.member.id] = game.dayCount
        val operationImageUrl = SystemImage.AGENT_NOTICE.imageUrl

        sendDm(owner, "$operationImageUrl\n${selectedTarget.member.effectiveName}님이 ${discoveredJob.name} 직업이라는 지령이 도착했습니다.")
        if (selectedTarget.job is Swindler && discoveredJob.name != "사기꾼") {
            SwindlerManager.notifyFooled(selectedTarget, owner)
        }
    }
    private fun sendDm(owner: PlayerData, message: String) {
        agentDmScope.launch {
            runCatching {
                owner.member.getDmChannel().createMessage(message)
            }
        }
    }
}
