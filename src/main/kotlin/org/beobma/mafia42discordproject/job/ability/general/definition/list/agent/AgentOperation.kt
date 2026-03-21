package org.beobma.mafia42discordproject.job.ability.general.definition.list.agent

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.definition.Definition
import org.beobma.mafia42discordproject.job.definition.list.Agent

class AgentOperation : JobUniqueAbility, PassiveAbility {
    override val name: String = "공작"
    override val description: String = "낮마다 지령을 받아 시민 한 명의 직업을 알아낸다."
    override val image: String = ""

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
            sendDm(owner, "아무런 정보가 도착하지 않았습니다.")
            return
        }

        val discoveredJob = selectedTarget.job ?: run {
            sendDm(owner, "아무런 정보가 도착하지 않았습니다.")
            return
        }

        agentJob.discoveredCitizenTargetIds += selectedTarget.member.id
        agentJob.discoveredCitizenTargetDayById[selectedTarget.member.id] = game.dayCount
        val operationImageUrl = "https://discord.com/channels/1483817958319849616/1483977619258212392/1484982478434209803"

        sendDm(owner, "$operationImageUrl\n${selectedTarget.member.effectiveName}님이 ${discoveredJob.name} 직업이라는 지령이 도착했습니다.")
    }
    private fun sendDm(owner: PlayerData, message: String) {
        agentDmScope.launch {
            runCatching {
                owner.member.getDmChannel().createMessage(message)
            }
        }
    }
}
