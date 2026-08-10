package org.beobma.mafia42discordproject.job.ability.general.evil.list.thief

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
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.JobManager
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.magician.Assistant
import org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.MafiaAbility
import org.beobma.mafia42discordproject.job.definition.list.*
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import org.beobma.mafia42discordproject.job.evil.list.Thief
import org.beobma.mafia42discordproject.job.evil.list.Villain

class ThiefAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "도벽"
    override val description: String = "투표시간마다 원하는 플레이어를 선택해 그 사람의 고유능력을 밤까지 사용할 수 있다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/thief_ability_1.webp"
    override val usablePhase: GamePhase = GamePhase.VOTE

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase || game.defenseTargetId != null) {
            return AbilityResult(false, "도벽은 본투표 시간에만 사용할 수 있습니다.")
        }
        if (target == null) {
            return AbilityResult(false, "훔칠 대상을 지정해야 합니다.")
        }
        val thief = caster.job as? Thief
            ?: return AbilityResult(false, "도둑만 사용할 수 있습니다.")
        if (thief.hasUsedTheftThisVote) {
            return AbilityResult(false, "이번 투표시간에는 이미 도벽 대상을 선택했습니다.")
        }
        thief.hasUsedTheftThisVote = true
        val effectiveTarget = HackerRedirectManager.resolveTarget(game, target) ?: target
        return stealFromTarget(game, caster, effectiveTarget, target)
    }

    private fun stealFromTarget(
        game: Game,
        caster: PlayerData,
        target: PlayerData,
        selectedTarget: PlayerData
    ): AbilityResult {
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
            return failWithNotification(game, caster, FrogCurseManager.abilityBlockedMessage(caster))
        }

        val thief = caster.job as? Thief ?: return AbilityResult(false, "도둑만 사용할 수 있습니다.")
        thief.stolenSourcePlayerId?.let { previousSourceId ->
            val isStillStolenByAnotherThief = game.playerDatas.any { player ->
                player.member.id != caster.member.id &&
                    (player.job as? Thief)?.stolenSourcePlayerId == previousSourceId
            }
            if (!isStillStolenByAnotherThief) {
                game.judgeAuthorityDisabledByThiefIds.remove(previousSourceId)
            }
        }
        if (target.member.id == caster.member.id) {
            if (
                target.job is Thief &&
                thief.hasSuccessor() &&
                isAliveMafiaAbsent(game)
            ) {
                val mafiaJob = Mafia()
                thief.hasActivatedSuccessorMafia = true
                thief.setStolenJob(mafiaJob, caster.member.id, mafiaJob.abilities)
                notifyStealSuccess(game, caster, selectedTarget, "마피아")
                return AbilityResult(true, "후계자로서 마피아의 처형 능력을 얻었습니다.")
            }
            thief.clearStolenAbility()
            return failWithNotification(game, caster, "자기 자신의 능력은 훔칠 수 없습니다.")
        }
        if (target.state.isDead && !canStealFromDeadTarget(game, thief, target)) {
            thief.clearStolenAbility()
            return failWithNotification(game, caster, "사망한 플레이어의 능력은 훔칠 수 없습니다.")
        }

        val targetJob = target.job ?: run {
            thief.clearStolenAbility()
            return failWithNotification(game, caster, "대상의 직업 정보를 확인할 수 없습니다.")
        }

        if (targetJob is Mafia) {
            val mafiaJob = Mafia()
            thief.setStolenJob(mafiaJob, target.member.id, mafiaJob.abilities)
            if (!thief.hasContactedMafia) {
                thief.hasContactedMafia = true
                notifyThiefContact(game, caster)
                return AbilityResult(true, "마피아 팀과 접선하고 처형 능력을 얻었습니다.")
            }
            notifyStealSuccess(game, caster, selectedTarget, targetJob.name)
            return AbilityResult(true, "마피아의 처형 능력을 얻었습니다.")
        }

        if (targetJob is Soldier) {
            thief.clearStolenAbility()
            notifyStealFailedOnSoldier(game, caster, target)
            return AbilityResult(true, "훔치는 데 실패했습니다.")
        }

        if (targetJob is Politician && thief.hasStolenPoliticianAbility) {
            thief.clearStolenAbility()
            return failWithNotification(game, caster, "정치인의 능력은 게임당 1회만 훔칠 수 있습니다.")
        }
        if (targetJob is Judge && target.state.isDead && thief.hasStolenJudgeAbility) {
            thief.clearStolenAbility()
            return failWithNotification(game, caster, "판사의 능력은 게임당 1회만 훔칠 수 있습니다.")
        }

        val borrowedJob = createBorrowedJob(targetJob)
            ?: run {
                thief.clearStolenAbility()
                return failWithNotification(game, caster, "훔칠 수 있는 고유 능력이 없습니다.")
            }
        seedBorrowedJobState(borrowedJob, targetJob)
        if (borrowedJob is Magician && thief.hasUsedStolenMagicianTrick) {
            borrowedJob.hasUsedTrick = true
        }
        val stolenAbilities = selectBorrowedAbilities(
            targetJob = targetJob,
            borrowedJob = borrowedJob,
            targetIsDead = target.state.isDead
        )

        if (targetJob is Judge && !target.state.isDead) {
            game.judgeAuthorityDisabledByThiefIds += target.member.id
        }
        if (stolenAbilities.isEmpty() && targetJob !is Prophet && targetJob !is Nurse && targetJob !is Judge) {
            thief.clearStolenAbility()
            return failWithNotification(game, caster, "훔칠 수 있는 고유 능력이 없습니다.")
        }

        thief.setStolenJob(borrowedJob, target.member.id, stolenAbilities)
        notifyStealSuccess(game, caster, selectedTarget, targetJob.name)
        return AbilityResult(true, "**${selectedTarget.member.effectiveName}님의 직업 ${targetJob.name}을 훔쳤습니다.**")
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
        internal val supportedStolenAbilityNames = setOf(
            "처형",
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
            "저주",
            "공작",
            "특종",
            "접신",
            "성불",
            "관찰",
            "조회",
            "이슈",
            "트릭",
            "계시",
            "처방",
            "해킹",
            "의뢰",
            "밀사",
            "접선",
            "최면",
            "최면 해제",
            "연애"
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

    private fun isAliveMafiaAbsent(game: Game): Boolean {
        return game.playerDatas.none { !it.state.isDead && it.job is Mafia }
    }

    fun canStealFromDeadTarget(game: Game, thief: Thief, target: PlayerData): Boolean {
        if (!thief.hasCondolences()) return false
        val diedDayCount = target.state.diedDayCount ?: return false
        return game.dayCount - diedDayCount <= 1
    }

    private fun createBorrowedJob(targetJob: Job): Job? {
        if (targetJob is Ghoul || targetJob is Citizen || targetJob is Villain || targetJob is Thief) {
            return null
        }
        return JobManager.createByName(targetJob.name)
    }

    private fun selectBorrowedAbilities(
        targetJob: Job,
        borrowedJob: Job,
        targetIsDead: Boolean
    ): List<JobUniqueAbility> {
        if (targetJob is Judge && !targetIsDead) return emptyList()
        if (targetJob is Nurse || targetJob is Prophet) {
            return borrowedJob.abilities.filter { it.name in supportedStolenAbilityNames }
        }
        return borrowedJob.abilities.filter { ability ->
            ability.name != name && ability.name in supportedStolenAbilityNames
        }
    }

    private fun seedBorrowedJobState(borrowedJob: Job, sourceJob: Job) {
        when {
            borrowedJob is Couple && sourceJob is Couple -> {
                borrowedJob.role = sourceJob.role
                borrowedJob.pairedPlayerId = sourceJob.pairedPlayerId
            }
            borrowedJob is Reporter && sourceJob is Reporter -> {
                borrowedJob.hasUsedScoop = sourceJob.hasUsedScoop
                borrowedJob.hasPublishedArticle = sourceJob.hasPublishedArticle
            }
            borrowedJob is Magician && sourceJob is Magician -> {
                borrowedJob.hasUsedTrick = sourceJob.hasUsedTrick &&
                    sourceJob.extraAbilities.none { it is Assistant }
            }
            borrowedJob is Hacker && sourceJob is Hacker -> {
                borrowedJob.hasResolvedHackDiscovery = sourceJob.hasResolvedHackDiscovery
            }
            borrowedJob is Mercenary && sourceJob is Mercenary -> {
                borrowedJob.hasExecutionAuthority = sourceJob.hasExecutionAuthority
            }
            borrowedJob is Cabal && sourceJob is Cabal -> {
                borrowedJob.role = sourceJob.role
            }
        }
    }
}
