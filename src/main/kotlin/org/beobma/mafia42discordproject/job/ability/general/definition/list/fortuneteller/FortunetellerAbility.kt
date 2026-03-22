package org.beobma.mafia42discordproject.job.ability.general.definition.list.fortuneteller

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.FortunetellerNotificationManager
import org.beobma.mafia42discordproject.game.system.FrogCurseManager
import org.beobma.mafia42discordproject.game.system.HackerRedirectManager
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.definition.list.Fortuneteller

class FortunetellerAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "운세"
    override val description: String = "밤마다 한 명을 선택한다. 선택한 플레이어 및 그와 다른 팀인 플레이어의 직업이 제시된다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1485329164134252715/b0fc0b0560bbe854.png?ex=69c17814&is=69c02694&hm=b10dbc61bd0586c903f85b6141ef0de6fcc4b623ee3a45456eadc69b658f4240&"
    override val usablePhase: GamePhase = GamePhase.NIGHT

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "운세는 밤에만 사용할 수 있습니다.")
        }
        if (caster.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 능력을 사용할 수 없습니다.")
        }
        if (target == null) {
            return AbilityResult(false, "운세 대상을 지정해야 합니다.")
        }
        if (target.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 운세 대상으로 지정할 수 없습니다.")
        }

        val fortuneteller = caster.job as? Fortuneteller
            ?: return AbilityResult(false, "점쟁이만 사용할 수 있습니다.")

        val effectiveTarget = HackerRedirectManager.resolveTarget(game, target) ?: target
        val fixedTargetId = fortuneteller.fixedFortuneTargetId
        if (fixedTargetId != null && fixedTargetId != effectiveTarget.member.id) {
            return AbilityResult(false, "한번 정한 운세 대상은 변경할 수 없습니다.")
        }

        fortuneteller.fixedFortuneTargetId = effectiveTarget.member.id
        sendFortuneResultImmediately(game, caster, effectiveTarget)
        return AbilityResult(true, "${target.member.effectiveName}님을 운세 대상으로 지정했습니다.")
    }

    private fun sendFortuneResultImmediately(game: Game, fortuneteller: PlayerData, target: PlayerData) {
        val targetJobName = FrogCurseManager.displayedJob(target)?.name ?: return
        val gameJobNames = game.playerDatas.mapNotNull { FrogCurseManager.displayedJob(it)?.name }.distinct()
        if (gameJobNames.isEmpty()) return

        val decoyPool = gameJobNames.filter { it != targetJobName }
        val decoyJobName = (decoyPool.ifEmpty { gameJobNames }).randomOrNull() ?: return
        val shownJobs = listOf(targetJobName, decoyJobName).shuffled()

        val arcanaTargets = if (fortuneteller.allAbilities.any { it is Arcana }) {
            selectArcanaTargets(game, fortuneteller, target, shownJobs, targetJobName)
        } else {
            emptyList()
        }

        fortunetellerDmScope.launch {
            FortunetellerNotificationManager.notifyFortuneResult(
                fortuneteller = fortuneteller,
                target = target,
                shownJobs = shownJobs,
                arcanaTargets = arcanaTargets
            )
        }
    }

    private fun selectArcanaTargets(
        game: Game,
        fortuneteller: PlayerData,
        fixedTarget: PlayerData,
        shownJobs: List<String>,
        targetJobName: String
    ): List<PlayerData> {
        val candidates = game.playerDatas.filter { it.member.id != fixedTarget.member.id }
        if (candidates.isEmpty()) return emptyList()

        val complementaryRole = shownJobs.firstOrNull { it != targetJobName }

        val complementaryCandidates = candidates.filter { FrogCurseManager.displayedJob(it)?.name == complementaryRole }.shuffled()
        val shownJobCandidates = candidates.filter { candidate ->
            val jobName = FrogCurseManager.displayedJob(candidate)?.name
            jobName != null && jobName in shownJobs
        }.shuffled()
        val nonShownJobCandidates = candidates.filter { candidate ->
            val jobName = FrogCurseManager.displayedJob(candidate)?.name
            jobName == null || jobName !in shownJobs
        }.shuffled()

        val selected = mutableListOf<PlayerData>()

        complementaryCandidates.firstOrNull { it.member.id != fortuneteller.member.id }
            ?.let { selected += it }
            ?: complementaryCandidates.firstOrNull()?.let { selected += it }
            ?: shownJobCandidates.firstOrNull { it.member.id != fortuneteller.member.id }?.let { selected += it }
            ?: shownJobCandidates.firstOrNull()?.let { selected += it }

        nonShownJobCandidates.firstOrNull { it !in selected && it.member.id != fortuneteller.member.id }
            ?.let { selected += it }
            ?: nonShownJobCandidates.firstOrNull { it !in selected }?.let { selected += it }

        if (selected.size < 2) {
            candidates
                .filter { it !in selected }
                .shuffled()
                .take(2 - selected.size)
                .forEach { selected += it }
        }

        return selected.take(2)
    }

    companion object {
        private val fortunetellerDmScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
