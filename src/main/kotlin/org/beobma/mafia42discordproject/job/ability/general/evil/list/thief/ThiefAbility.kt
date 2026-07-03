package org.beobma.mafia42discordproject.job.ability.general.evil.list.thief

import dev.kord.core.behavior.channel.createMessage
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
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.MafiaAbility
import org.beobma.mafia42discordproject.job.definition.list.Judge
import org.beobma.mafia42discordproject.job.definition.list.Politician
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import org.beobma.mafia42discordproject.job.evil.list.Thief

class ThiefAbility : JobUniqueAbility {
    override val name: String = "도벽"
    override val description: String = "투표시간에 최종적으로 투표한 플레이어의 고유 능력을 훔쳐 밤까지 사용할 수 있다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(133).webp"

    fun stealFromFinalVote(game: Game, caster: PlayerData, target: PlayerData): AbilityResult {
        if (caster.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 도벽을 사용할 수 없습니다.")
        }
        if (caster.member.id in game.pendingNightDeathPlayerIds) {
            return failWithNotification(game, caster, "이미 암살당해 도벽이 발동하지 않습니다.")
        }
        if (caster.state.isSilenced) {
            return failWithNotification(game, caster, "유혹 상태에서는 도벽이 발동하지 않습니다.")
        }
        if (FrogCurseManager.isCursed(caster)) {
            return failWithNotification(game, caster, "개구리 상태에서는 도벽이 발동하지 않습니다.")
        }

        val thief = caster.job as? Thief ?: return AbilityResult(false, "도둑만 사용할 수 있습니다.")
        if (target.member.id == caster.member.id) {
            return failWithNotification(game, caster, "자기 자신의 능력은 훔칠 수 없습니다.")
        }
        if (target.state.isDead && !canStealFromDeadTarget(game, thief, target)) {
            return failWithNotification(game, caster, "사망한 플레이어의 능력은 훔칠 수 없습니다.")
        }

        val targetJob = target.job ?: return failWithNotification(game, caster, "대상의 직업 정보를 확인할 수 없습니다.")

        if (targetJob is Mafia) {
            if (thief.hasSuccessor() && isAliveMafiaAbsent(game)) {
                val successorAbility = instantiateAbility(MafiaAbility())
                thief.setStolenAbility(successorAbility)
                notifyStealSuccess(game, caster, target, targetJob.name)
                return AbilityResult(true, "**${target.member.effectiveName}님의 직업 ${targetJob.name}을 훔쳤습니다.**")
            }
            thief.hasContactedMafia = true
            notifyThiefContact(game, caster)
            return AbilityResult(true, "마피아 팀과 접선했습니다.")
        }

        if (targetJob is Soldier) {
            notifyStealFailedOnSoldier(game, caster, target)
            return AbilityResult(true, "훔치는 데 실패했습니다.")
        }

        if (targetJob is Politician && thief.hasStolenPoliticianAbility) {
            return failWithNotification(game, caster, "정치인의 능력은 게임당 1회만 훔칠 수 있습니다.")
        }
        if (targetJob is Judge && thief.hasStolenJudgeAbility) {
            return failWithNotification(game, caster, "판사의 능력은 게임당 1회만 훔칠 수 있습니다.")
        }

        val targetAbility = pickStealableAbility(targetJob.abilities)
            ?: return failWithNotification(game, caster, "훔칠 수 있는 고유 능력이 없습니다.")
        val stolenAbility = instantiateAbility(targetAbility)

        thief.setStolenAbility(stolenAbility)
        if (targetJob is Politician) {
            thief.hasStolenPoliticianAbility = true
        }
        if (targetJob is Judge) {
            thief.hasStolenJudgeAbility = true
        }

        notifyStealSuccess(game, caster, target, targetJob.name)
        return AbilityResult(true, "**${target.member.effectiveName}님의 직업 ${targetJob.name}을 훔쳤습니다.**")
    }

    private fun failWithNotification(game: Game, caster: PlayerData, message: String): AbilityResult {
        notifyStealFailed(game, caster, message)
        return AbilityResult(false, message)
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        private const val THIEF_STEAL_IMAGE_URL =
            "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(10).webp"
        private const val THIEF_CONTACT_IMAGE_URL =
            "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(26).webp"
        private const val THIEF_SOLDIER_FAIL_IMAGE_URL =
            "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(39).webp"
        private val supportedStolenAbilityNames = setOf(
            "수색",
            "추리",
            "치료",
            "운세",
            "공갈",
            "자폭",
            "산화",
            "소생",
            "숙청",
            "처세",
            "선고",
            "말살",
            "청부",
            "첩보",
            "저주"
        )

        private fun notifyStealSuccess(game: Game, caster: PlayerData, target: PlayerData, targetJobName: String) {
            scope.launch {
                runCatching {
                    val message = "**${target.member.effectiveName}님의 직업 ${targetJobName}을 훔쳤습니다.**\n$THIEF_STEAL_IMAGE_URL"
                    GameReplayLogger.logDirectMessage(game, caster, message, "도둑질 결과")
                    caster.member.getDmChannel().createMessage(message)
                }
            }
        }

        private fun notifyStealFailedOnSoldier(game: Game, caster: PlayerData, soldierTarget: PlayerData) {
            scope.launch {
                runCatching {
                    val message = "**훔치는 데 실패했습니다.**\n$THIEF_SOLDIER_FAIL_IMAGE_URL"
                    GameReplayLogger.logDirectMessage(game, caster, message, "도둑질 실패")
                    caster.member.getDmChannel().createMessage(message)
                }
                runCatching {
                    val message = "**${caster.member.effectiveName}님이 직업을 훔치려고 시도했습니다.**\n$THIEF_SOLDIER_FAIL_IMAGE_URL"
                    GameReplayLogger.logDirectMessage(game, soldierTarget, message, "도둑질 감지")
                    soldierTarget.member.getDmChannel().createMessage(message)
                }
            }
        }

        private fun notifyStealFailed(game: Game, caster: PlayerData, reason: String) {
            scope.launch {
                runCatching {
                    val message = "**도벽 실패:** $reason"
                    GameReplayLogger.logDirectMessage(game, caster, message, "도둑질 실패")
                    caster.member.getDmChannel().createMessage(message)
                }
            }
        }

        private fun notifyThiefContact(game: Game, thiefPlayer: PlayerData) {
            scope.launch {
                runCatching {
                    val message = "**마피아 팀과 접선했습니다.**\n$THIEF_CONTACT_IMAGE_URL"
                    GameReplayLogger.logDirectMessage(game, thiefPlayer, message, "도둑질 결과")
                    thiefPlayer.member.getDmChannel().createMessage(message)
                }
                runCatching {
                    if (!thiefPlayer.state.hasAnnouncedThiefContact) {
                        thiefPlayer.state.hasAnnouncedThiefContact = true
                        GameLoopManager.announceMafiaSupportContact(game, thiefPlayer, THIEF_CONTACT_IMAGE_URL)
                    }
                    GameLoopManager.refreshMafiaChannelContactState(game)
                }
            }
        }
    }

    private fun instantiateAbility(ability: JobUniqueAbility): JobUniqueAbility {
        return runCatching {
            val constructor = ability::class.java.getDeclaredConstructor()
            constructor.isAccessible = true
            constructor.newInstance()
        }.getOrElse {
            ability
        }
    }

    private fun isAliveMafiaAbsent(game: Game): Boolean {
        return game.playerDatas.none { !it.state.isDead && it.job is Mafia }
    }

    private fun canStealFromDeadTarget(game: Game, thief: Thief, target: PlayerData): Boolean {
        if (!thief.hasCondolences()) return false
        val diedDayCount = target.state.diedDayCount ?: return false
        return game.dayCount - diedDayCount <= 1
    }

    private fun pickStealableAbility(abilities: List<JobUniqueAbility>): JobUniqueAbility? {
        return abilities
            .filter { ability -> ability.name != name }
            .filter { ability -> ability.name in supportedStolenAbilityNames }
            .minByOrNull(::stealPriority)
    }

    private fun stealPriority(ability: JobUniqueAbility): Int {
        val active = ability as? ActiveAbility ?: return 20
        return when (active.usablePhase) {
            GamePhase.NIGHT -> 0
            GamePhase.VOTE -> 1
            GamePhase.DAY -> 30
            GamePhase.DAWN, GamePhase.END -> 40
        }
    }
}
