package org.beobma.mafia42discordproject.job.ability.general.evil.list.spy

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GameLoopManager
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.replay.GameReplayLogger
import org.beobma.mafia42discordproject.game.system.FrogCurseManager
import org.beobma.mafia42discordproject.game.system.HackerRedirectManager
import org.beobma.mafia42discordproject.game.system.PrivateJobKnowledgeManager
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import org.beobma.mafia42discordproject.job.evil.list.Spy
import org.beobma.mafia42discordproject.job.evil.list.Thief

class SpyAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "첩보"
    override val description: String = "밤마다 플레이어 한 명을 선택하여 직업을 알 수 있으며 마피아라면 접선한다. 마피아와 접선할 경우, 한 번 더 능력을 사용할 수 있다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(106).webp"
    override val usablePhase: GamePhase = GamePhase.NIGHT

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "첩보는 밤에만 사용할 수 있습니다.")
        }
        if (caster.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 첩보를 사용할 수 없습니다.")
        }

        val spy = caster.job as? Spy
        val thief = caster.job as? Thief
        if (spy == null && thief == null) {
            return AbilityResult(false, "스파이 또는 첩보 능력을 훔친 도둑만 사용할 수 있습니다.")
        }
        val remainingUses = spy?.remainingIntelUsesTonight ?: thief?.stolenSpyRemainingIntelUsesTonight ?: 0
        if (remainingUses <= 0) {
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

        if (spy != null) {
            spy.remainingIntelUsesTonight -= 1
            spy.lastInvestigatedTargetId = effectiveTarget.member.id
        }
        if (thief != null) {
            thief.stolenSpyRemainingIntelUsesTonight -= 1
            thief.stolenSpyLastInvestigatedTargetId = effectiveTarget.member.id
        }

        if (effectiveTarget.job is Soldier) {
            notifySoldierDetected(game, caster, effectiveTarget)
            return AbilityResult(true, "${target.member.effectiveName}님의 직업을 확인했습니다.")
        }

        if (effectiveTarget.job is Mafia) {
            PrivateJobKnowledgeManager.rememberExactJob(game, caster, effectiveTarget, "마피아")
            if (spy != null && !spy.hasContactedMafia) {
                spy.hasContactedMafia = true
                spy.remainingIntelUsesTonight += 1
                notifySpyContact(game, caster)
            }
            if (thief != null && !thief.hasContactedMafiaByStolenSpy) {
                thief.hasContactedMafiaByStolenSpy = true
                thief.hasContactedMafia = true
                thief.stolenSpyRemainingIntelUsesTonight += 1
                notifySpyContact(game, caster, supportJobNameOverride = "도둑")
            }
            return AbilityResult(true, "마피아 팀과 접선했습니다.")
        }

        val jobName = FrogCurseManager.displayedJob(effectiveTarget)?.name ?: "알 수 없음"
        notifyInvestigationResult(game, caster, effectiveTarget, jobName)
        return AbilityResult(true, "${target.member.effectiveName}님의 직업을 확인했습니다.")
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        private const val SPY_INTEL_IMAGE_URL =
            "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(7).webp"
        private const val SPY_CONTACT_IMAGE_URL =
            "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(30).webp"
        private const val SPY_SOLDIER_IMAGE_URL =
            "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(47).webp"

        fun applyAutopsyOnDeath(game: Game, victim: PlayerData) {
            game.playerDatas.forEach { spyPlayer ->
                if (spyPlayer.state.isDead) return@forEach
                if (FrogCurseManager.shouldSuppressPassive(spyPlayer)) return@forEach
                if (spyPlayer.member.id == victim.member.id) return@forEach

                val spyJob = spyPlayer.job as? Spy ?: return@forEach
                spyJob.lastInvestigatedTargetId = victim.member.id

                if (victim.job is Soldier) {
                    notifySoldierDetected(game, spyPlayer, victim)
                    return@forEach
                }

                if (victim.job is Mafia) {
                    PrivateJobKnowledgeManager.rememberExactJob(game, spyPlayer, victim, "마피아")
                    if (!spyJob.hasContactedMafia) {
                        spyJob.hasContactedMafia = true
                        notifySpyContact(game, spyPlayer)
                    }
                    return@forEach
                }

                val revealedJobName = FrogCurseManager.displayedJob(victim)?.name ?: "알 수 없음"
                notifyInvestigationResult(game, spyPlayer, victim, revealedJobName)
            }
        }

        private fun notifyInvestigationResult(game: Game, spyPlayer: PlayerData, target: PlayerData, jobName: String) {
            PrivateJobKnowledgeManager.rememberExactJob(game, spyPlayer, target, jobName)
            scope.launch {
                runCatching {
                    val message = "**${target.member.effectiveName}님의 직업은 ${jobName}**\n$SPY_INTEL_IMAGE_URL"
                    GameReplayLogger.logDirectMessage(game, spyPlayer, message, "스파이 첩보")
                    spyPlayer.member.getDmChannel().createMessage(message)
                }
            }
        }

        private fun notifySoldierDetected(game: Game, spyPlayer: PlayerData, soldierPlayer: PlayerData) {
            PrivateJobKnowledgeManager.rememberExactJob(game, spyPlayer, soldierPlayer, "군인")
            scope.launch {
                runCatching {
                    val message = "**${soldierPlayer.member.effectiveName}님의 직업은 군인**\n$SPY_SOLDIER_IMAGE_URL"
                    GameReplayLogger.logDirectMessage(game, spyPlayer, message, "스파이 첩보")
                    spyPlayer.member.getDmChannel().createMessage(message)
                }
                runCatching {
                    val message = "**스파이 ${spyPlayer.member.effectiveName}님이 당신을 조사하였습니다.**\n$SPY_SOLDIER_IMAGE_URL"
                    GameReplayLogger.logDirectMessage(game, soldierPlayer, message, "스파이 조사")
                    soldierPlayer.member.getDmChannel().createMessage(message)
                }
            }
        }

        private fun notifySpyContact(game: Game, spyPlayer: PlayerData, supportJobNameOverride: String? = null) {
            scope.launch {
                runCatching {
                    GameLoopManager.announceMafiaSupportContact(game, spyPlayer, SPY_CONTACT_IMAGE_URL, supportJobNameOverride)
                }
                runCatching {
                    GameLoopManager.refreshMafiaChannelContactState(game)
                }
            }
        }
    }
}
