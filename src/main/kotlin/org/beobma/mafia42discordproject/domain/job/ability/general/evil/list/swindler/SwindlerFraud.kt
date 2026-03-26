package org.beobma.mafia42discordproject.job.ability.general.evil.list.swindler

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
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import org.beobma.mafia42discordproject.job.evil.list.Swindler

class SwindlerFraud : JobUniqueAbility, PassiveAbility {
    override val name: String = "사기"
    override val description: String = "게임 시작 시 시민 한 명의 정체를 알아내고 그 직업으로 변장한다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1485289269777137826/595e6ae33bfcef82.png?ex=69c152ed&is=69c0016d&hm=9da8157e3070226e17a1dac177c376cd29d951f0a616c9e6f998eb1edcc353e5&"

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
            notifyFraudFailed(owner, candidate)
            return
        }

        swindler.disguisedTargetId = candidate.member.id
        swindler.disguisedJobName = candidateJob.name
        notifyFraudSuccess(owner, candidate, candidateJob.name)
    }

    private fun notifyFraudSuccess(swindlerPlayer: PlayerData, targetPlayer: PlayerData, targetJobName: String) {
        dmScope.launch {
            runCatching {
                swindlerPlayer.member.getDmChannel()
                    .createMessage("(${targetPlayer.member.effectiveName})의 (${targetJobName})으로 변장했습니다.")
            }
        }
    }

    private fun notifyFraudFailed(swindlerPlayer: PlayerData, soldierPlayer: PlayerData) {
        dmScope.launch {
            runCatching {
                swindlerPlayer.member.getDmChannel()
                    .createMessage("**사기에 실패했습니다.**\n$SWINDLER_SOLDIER_DETECTED_IMAGE_URL")
            }
            runCatching {
                soldierPlayer.member.getDmChannel()
                    .createMessage("**사기꾼 ${swindlerPlayer.member.effectiveName}님의 정체를 알아냈습니다.**\n$SWINDLER_SOLDIER_DETECTED_IMAGE_URL")
            }
        }
    }

    companion object {
        private val dmScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        const val SWINDLER_SOLDIER_DETECTED_IMAGE_URL =
            "https://cdn.discordapp.com/attachments/1483977619258212392/1485101958561005750/BPW0MHzwutemHCShBg0ZT-eplKvZXUkajGj0sU1D0_vJm5F6NQO4OKdZv1GkJSGf7bMRngaCXF4DUIj8hE4g_f-mjt2mjilcy5TNaPN1HQ5OGbWdMObnuh1x_wa18r74nz2LHVebMSakUmGg-OHhTA.webp?ex=69c0a47a&is=69bf52fa&hm=601879c3b9e0d766c07c2536349d3ec0854b3365ba4370205eddb0e79b449cce&"
    }
}
