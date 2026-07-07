package org.beobma.mafia42discordproject.job.ability.general.definition.list.agent

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.replay.GameReplayLogger
import org.beobma.mafia42discordproject.game.system.DiscoveryStep
import org.beobma.mafia42discordproject.game.system.FrogCurseManager
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.game.system.SystemImage
import org.beobma.mafia42discordproject.game.system.SwindlerManager
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.Hypocrisy
import org.beobma.mafia42discordproject.job.definition.Definition
import org.beobma.mafia42discordproject.job.definition.list.Agent
import org.beobma.mafia42discordproject.job.definition.list.Doctor
import org.beobma.mafia42discordproject.job.evil.list.Swindler

class AgentOperation : JobUniqueAbility, PassiveAbility {
    override val name: String = "공작"
    override val description: String = "밤이 끝날 때마다 지령을 받아 시민 한 명의 직업을 알아낸다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/agent_ability_1.png"

    companion object {
        private val agentDmScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        fun resolveNightEndOperations(game: Game) {
            game.playerDatas.forEach { owner ->
                resolveNightEndOperation(game, owner)
            }
        }

        private fun resolveNightEndOperation(game: Game, owner: PlayerData) {
            if (owner.state.isDead) return
            if (FrogCurseManager.shouldSuppressPassive(owner)) return

            val agentJob = owner.job as? Agent ?: return
            val candidates = game.playerDatas
                .asSequence()
                .filter { !it.state.isDead }
                .filter { it.member.id != owner.member.id }
                .filter { it.job is Definition || shouldApplyHypocrisyToOperation(game, it) }
                .filter { !it.state.isJobPubliclyRevealed }
                .filter { it.member.id !in agentJob.discoveredCitizenTargetIds }
                .toList()

            val selectedTarget = candidates.shuffled().firstOrNull()
            if (selectedTarget == null) {
                sendDm(game, owner, "지령이 도착하지 않았습니다.")
                return
            }

            val actualJob = selectedTarget.job ?: run {
                sendDm(game, owner, "지령이 도착하지 않았습니다.")
                return
            }
            val operationImageUrl = SystemImage.AGENT_NOTICE.imageUrl
            val discoveryEvent = GameEvent.JobDiscovered(
                discoverer = owner,
                target = selectedTarget,
                actualJob = actualJob,
                revealedJob = FrogCurseManager.displayedJob(selectedTarget) ?: actualJob,
                sourceAbilityName = "공작",
                resolvedAt = DiscoveryStep.NIGHT,
                notifyTarget = false,
                imageUrl = operationImageUrl
            )

            applyHypocrisySpoofIfNeeded(game, discoveryEvent)
            dispatchDiscoveryEvent(game, discoveryEvent)

            agentJob.discoveredCitizenTargetIds += selectedTarget.member.id
            agentJob.discoveredCitizenTargetDayById[selectedTarget.member.id] = game.dayCount
            sendDm(
                game,
                owner,
                "$operationImageUrl\n${selectedTarget.member.effectiveName}님이 ${discoveryEvent.revealedJob.name} 직업이라는 지령이 도착했습니다."
            )
            if (selectedTarget.job is Swindler && discoveryEvent.revealedJob.name != "사기꾼") {
                SwindlerManager.notifyFooled(selectedTarget, owner)
            }
        }

        private fun shouldApplyHypocrisyToOperation(game: Game, target: PlayerData): Boolean {
            if (game.dayCount != 1) return false
            if (FrogCurseManager.shouldSuppressPassive(target)) return false
            return target.allAbilities.any { it is Hypocrisy }
        }

        private fun applyHypocrisySpoofIfNeeded(game: Game, event: GameEvent.JobDiscovered) {
            if (!shouldApplyHypocrisyToOperation(game, event.target)) return
            event.revealedJob = Doctor()
            event.isFalsified = true
            event.note = ""
        }

        private fun dispatchDiscoveryEvent(game: Game, event: GameEvent.JobDiscovered) {
            game.playerDatas
                .filter { !it.state.isDead }
                .forEach { player ->
                    player.allAbilities
                        .filterIsInstance<PassiveAbility>()
                        .filterNot { FrogCurseManager.shouldSuppressPassive(player) }
                        .sortedByDescending(PassiveAbility::priority)
                        .forEach { passive ->
                            passive.onEventObserved(game, player, event)
                        }
                }
        }

        private fun sendDm(game: Game, owner: PlayerData, message: String) {
            agentDmScope.launch {
                runCatching {
                    GameReplayLogger.logDirectMessage(game, owner, message, "요원 지령")
                    owner.member.getDmChannel().createMessage(message)
                }
            }
        }
    }
}
