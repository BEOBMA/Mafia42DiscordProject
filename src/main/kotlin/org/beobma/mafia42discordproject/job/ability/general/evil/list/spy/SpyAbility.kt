package org.beobma.mafia42discordproject.job.ability.general.evil.list.spy

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GameLoopManager
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.FrogCurseManager
import org.beobma.mafia42discordproject.game.system.HackerRedirectManager
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import org.beobma.mafia42discordproject.job.evil.list.Spy

class SpyAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "첩보"
    override val description: String = "밤마다 플레이어 한 명을 선택하여 직업을 알 수 있으며 마피아라면 접선한다. 마피아와 접선할 경우, 한 번 더 능력을 사용할 수 있다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1485290520623317272/eee6f191ce4f18c1.png?ex=69c15417&is=69c00297&hm=e74e7a2ce5c8d60fd994f790561fffb9b7738d182b61ea06a19d6584fcf4887c&"
    override val usablePhase: GamePhase = GamePhase.NIGHT

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "첩보는 밤에만 사용할 수 있습니다.")
        }
        if (caster.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 첩보를 사용할 수 없습니다.")
        }

        val spy = caster.job as? Spy ?: return AbilityResult(false, "스파이만 사용할 수 있습니다.")
        if (spy.remainingIntelUsesTonight <= 0) {
            return AbilityResult(false, "이번 밤에는 더 이상 첩보를 사용할 수 없습니다.")
        }

        if (target == null) {
            return AbilityResult(false, "첩보 대상을 지정해야 합니다.")
        }
        if (target.member.id == caster.member.id) {
            return AbilityResult(false, "자기 자신은 조사할 수 없습니다.")
        }
        if (target.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 조사할 수 없습니다.")
        }

        val effectiveTarget = HackerRedirectManager.resolveTarget(game, target) ?: target
        if (effectiveTarget.member.id == caster.member.id) {
            return AbilityResult(false, "자기 자신은 조사할 수 없습니다.")
        }
        if (effectiveTarget.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 조사할 수 없습니다.")
        }

        spy.remainingIntelUsesTonight -= 1
        spy.lastInvestigatedTargetId = effectiveTarget.member.id

        if (effectiveTarget.job is Soldier) {
            notifySoldierDetected(caster, effectiveTarget)
            return AbilityResult(true, "${target.member.effectiveName}님의 직업을 확인했습니다.")
        }

        if (effectiveTarget.job is Mafia) {
            if (!spy.hasContactedMafia) {
                spy.hasContactedMafia = true
                spy.remainingIntelUsesTonight += 1
                notifySpyContact(game)
            }
            return AbilityResult(true, "마피아 팀과 접선했습니다.")
        }

        val jobName = FrogCurseManager.displayedJob(effectiveTarget)?.name ?: "알 수 없음"
        notifyInvestigationResult(caster, effectiveTarget.member.effectiveName, jobName)
        return AbilityResult(true, "${target.member.effectiveName}님의 직업을 확인했습니다.")
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        private const val SPY_INTEL_IMAGE_URL =
            "https://cdn.discordapp.com/attachments/1483977619258212392/1485095367002554611/gSi2xCM8lqqiXJjf7wnnHF9ZFquwAUP9CwUR6086i8uZC4Y4h-T-W9O911C9Dq9njgQpc1uRkplrXXKK68uathlGas1KAuz3mu1Ne2cAWayhde6Wex68kt0eMUvU0PlgH0WHI437-tVhpnl_JdZucg.webp?ex=69c09e57&is=69bf4cd7&hm=88c5f17a652a634f03d8cfb3b613fa47e08f57ace7a19a07f5020e3f36447c2d&"
        private const val SPY_CONTACT_IMAGE_URL =
            "https://cdn.discordapp.com/attachments/1483977619258212392/1485095638931734699/36RXBo7-kuLUExBuE_kWriQbw5wVrunku4S93RbKCqX3p84cQ3DIICEpoeAzvUyyaUWGcqat9QOTar3r6T4nsDO-IYfUuoKVt1aAy7gNse3dAacQ5zYx1Ux3u43o9krFaspF-jD9VGDsgcsnibk6yg.webp?ex=69c09e98&is=69bf4d18&hm=08b19737b9c100da1541e49ec98ddd206247d43627045da5b9b1d534b9ae682e&"
        private const val SPY_SOLDIER_IMAGE_URL =
            "https://cdn.discordapp.com/attachments/1483977619258212392/1485095746746454046/03rkLr-nzfEae0eg1U3LRdOhVMxACBsf3izEft5AGmwzfAQD_BJ3xedClu_q9PW5mZEm2vqe2FiyU1ZP1tgPdwPVusRg9_iHf7jS6Fjt_weR7CcajRV9pKiQFbtdlqH9NtrdehTYaXYbaCcqBwqPoQ.webp?ex=69c09eb1&is=69bf4d31&hm=875765426abded18dbf1015ab1689cc448276cea9c559a9e5378960fa3456872&"

        fun applyAutopsyOnDeath(game: Game, victim: PlayerData) {
            game.playerDatas.forEach { spyPlayer ->
                if (spyPlayer.state.isDead) return@forEach
                if (spyPlayer.member.id == victim.member.id) return@forEach

                val spyJob = spyPlayer.job as? Spy ?: return@forEach
                spyJob.lastInvestigatedTargetId = victim.member.id

                if (victim.job is Soldier) {
                    notifySoldierDetected(spyPlayer, victim)
                    return@forEach
                }

                if (victim.job is Mafia) {
                    if (!spyJob.hasContactedMafia) {
                        spyJob.hasContactedMafia = true
                        notifySpyContact(game)
                    }
                    return@forEach
                }

                val revealedJobName = FrogCurseManager.displayedJob(victim)?.name ?: "알 수 없음"
                notifyInvestigationResult(spyPlayer, victim.member.effectiveName, revealedJobName)
            }
        }

        private fun notifyInvestigationResult(spyPlayer: PlayerData, targetName: String, jobName: String) {
            scope.launch {
                runCatching {
                    spyPlayer.member.getDmChannel().createMessage("**${targetName}님의 직업은 ${jobName}**\n$SPY_INTEL_IMAGE_URL")
                }
            }
        }

        private fun notifySoldierDetected(spyPlayer: PlayerData, soldierPlayer: PlayerData) {
            scope.launch {
                runCatching {
                    spyPlayer.member.getDmChannel().createMessage("**${soldierPlayer.member.effectiveName}님의 직업은 군인**\n$SPY_SOLDIER_IMAGE_URL")
                }
                runCatching {
                    soldierPlayer.member.getDmChannel().createMessage("**스파이 ${spyPlayer.member.effectiveName}님이 당신을 조사하였습니다.**\n$SPY_SOLDIER_IMAGE_URL")
                }
            }
        }

        private fun notifySpyContact(game: Game) {
            scope.launch {
                runCatching {
                    game.mafiaChannel?.createMessage("$SPY_CONTACT_IMAGE_URL\n**접선했습니다.**")
                }
                runCatching {
                    GameLoopManager.refreshMafiaChannelContactState(game)
                }
            }
        }
    }
}
