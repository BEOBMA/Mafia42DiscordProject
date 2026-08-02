package org.beobma.mafia42discordproject.job.ability.general.evil.list.swindler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.replay.GameReplayLogger
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.game.system.SwindlerManager
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.definition.Definition
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import org.beobma.mafia42discordproject.job.evil.list.Swindler

class SwindlerFraud : JobUniqueAbility, PassiveAbility {
    override val name: String = "사기"
    override val description: String = "게임 시작 시 시민 한 명의 정체를 알아내고 그 직업으로 변장한다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(164).webp"

    override fun onPhaseChanged(game: Game, owner: PlayerData, newPhase: GamePhase) {
        if (newPhase != GamePhase.NIGHT) return
        if (owner.state.isDead) return
        if (game.dayCount != 1) return

        val swindler = owner.job as? Swindler ?: return
        if (swindler.hasAttemptedFraud) return
        swindler.hasAttemptedFraud = true

        val candidate = game.playerDatas
            .filter { !it.state.isDead }
            .filter { it.member.id != owner.member.id }
            .filter { it.job is Definition }
            .shuffled()
            .firstOrNull()
            ?: return

        val candidateJob = candidate.job ?: return
        if (candidateJob is Soldier) {
            notifyFraudFailed(game, owner, candidate)
            return
        }

        swindler.disguisedTargetId = candidate.member.id
        swindler.disguisedJobName = candidateJob.name
        notifyFraudSuccess(game, owner, candidate, candidateJob.name)
    }

    override fun onEventObserved(game: Game, owner: PlayerData, event: GameEvent) {
        val discovery = event as? GameEvent.JobDiscovered ?: return
        if (discovery.target.member.id != owner.member.id) return
        if (discovery.discoverer.member.id == owner.member.id) return

        val disguisedJob = SwindlerManager.disguisedJobOf(owner) ?: return
        discovery.revealedJob = disguisedJob
        discovery.isFalsified = true
    }

    private fun notifyFraudSuccess(game: Game, swindlerPlayer: PlayerData, targetPlayer: PlayerData, targetJobName: String) {
        dmScope.launch {
            runCatching {
                GameReplayLogger.logDirectMessage(
                    game = game,
                    recipient = swindlerPlayer,
                    body = "(${targetPlayer.member.effectiveName})의 (${targetJobName})으로 변장했습니다.",
                    title = "사기 결과"
                )
                swindlerPlayer.member.getDmChannel()
                    .createMessage("(${targetPlayer.member.effectiveName})의 (${targetJobName})으로 변장했습니다.")
            }
        }
    }

    private fun notifyFraudFailed(game: Game, swindlerPlayer: PlayerData, soldierPlayer: PlayerData) {
        dmScope.launch {
            runCatching {
                GameReplayLogger.logDirectMessage(
                    game = game,
                    recipient = swindlerPlayer,
                    body = "**사기에 실패했습니다.**\n$SWINDLER_SOLDIER_DETECTED_IMAGE_URL",
                    title = "사기 실패"
                )
                swindlerPlayer.member.getDmChannel()
                    .createMessage("**사기에 실패했습니다.**\n$SWINDLER_SOLDIER_DETECTED_IMAGE_URL")
            }
            runCatching {
                GameReplayLogger.logDirectMessage(
                    game = game,
                    recipient = soldierPlayer,
                    body = "**사기꾼 ${swindlerPlayer.member.effectiveName}님의 정체를 알아냈습니다.**\n$SWINDLER_SOLDIER_DETECTED_IMAGE_URL",
                    title = "사기 감지"
                )
                soldierPlayer.member.getDmChannel()
                    .createMessage("**사기꾼 ${swindlerPlayer.member.effectiveName}님의 정체를 알아냈습니다.**\n$SWINDLER_SOLDIER_DETECTED_IMAGE_URL")
            }
        }
    }

    companion object {
        private val dmScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        const val SWINDLER_SOLDIER_DETECTED_IMAGE_URL =
            "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(15).webp"
    }
}
