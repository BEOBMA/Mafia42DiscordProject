package org.beobma.mafia42discordproject.job.ability.general.definition.list.mentalist

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.FrogCurseManager
import org.beobma.mafia42discordproject.game.system.HackerRedirectManager
import org.beobma.mafia42discordproject.game.system.InvestigationTeam
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.definition.list.Mentalist
import org.beobma.mafia42discordproject.job.evil.list.Thief
import org.beobma.mafia42discordproject.job.evil.list.actualOrStolenJob
import kotlin.random.Random

class MentalistAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "관찰"
    override val description: String = "낮마다 최대 2번, 다른 플레이어들의 대화를 선택해 서로 다른 팀인지 확인하고, 앞서 선택한 플레이어와 같은 팀이 나올 때까지 이를 반복한다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mentalist_ability_image.webp"
    override val usablePhase: GamePhase = GamePhase.DAY

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase && !(caster.job is Thief && game.currentPhase == GamePhase.VOTE)) {
            return AbilityResult(false, "관찰은 낮에만 사용할 수 있습니다.")
        }
        if (caster.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 능력을 사용할 수 없습니다.")
        }
        if (target == null) {
            return AbilityResult(false, "관찰할 대상을 지정해야 합니다.")
        }
        val effectiveTarget = HackerRedirectManager.resolveTarget(game, target) ?: target
        if (effectiveTarget.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 관찰 대상으로 지정할 수 없습니다.")
        }
        if (effectiveTarget.member.id == caster.member.id) {
            return AbilityResult(false, "자기 자신은 관찰 대상으로 지정할 수 없습니다.")
        }

        val mentalist = caster.actualOrStolenJob<Mentalist>()
            ?: return AbilityResult(false, "심리학자 또는 관찰 능력을 훔친 도둑만 사용할 수 있습니다.")

        if (mentalist.isObservationResolvedToday) {
            return AbilityResult(false, "오늘은 이미 같은 팀을 확인했습니다.")
        }
        if (mentalist.observationUsesToday >= MAX_OBSERVATION_USES_PER_DAY) {
            return AbilityResult(false, "오늘은 이미 관찰을 2번 사용했습니다.")
        }

        val initialTargetId = mentalist.initialObservationTargetId
        if (initialTargetId == null) {
            mentalist.initialObservationTargetId = effectiveTarget.member.id
            mentalist.lastObservationTargetId = effectiveTarget.member.id
            mentalist.initialObservationSelectedTargetId = target.member.id
            mentalist.lastObservationSelectedTargetId = target.member.id
            mentalist.observationUsesToday += 1
            return AbilityResult(
                true,
                "${target.member.effectiveName}님을 첫 관찰 대상으로 지정했습니다. 한 번 더 관찰을 사용해 다른 플레이어를 선택하세요."
            )
        }

        val previousTargetId = mentalist.lastObservationTargetId ?: initialTargetId
        if (effectiveTarget.member.id == previousTargetId) {
            return AbilityResult(false, "앞서 관찰한 플레이어와는 비교할 수 없습니다.")
        }

        val initialTarget = game.getPlayer(initialTargetId)
            ?: return AbilityResult(false, "처음 관찰한 플레이어 정보를 찾을 수 없습니다.")

        if (initialTarget.state.isDead) {
            return AbilityResult(false, "처음 관찰한 플레이어가 사망해 더 이상 관찰을 이어갈 수 없습니다.")
        }

        val previousTarget = game.getPlayer(previousTargetId)
            ?: return AbilityResult(false, "앞서 관찰한 플레이어 정보를 찾을 수 없습니다.")
        val previousSelectedTarget = mentalist.lastObservationSelectedTargetId
            ?.let(game::getPlayer)
            ?: previousTarget

        if (previousTarget.state.isDead) {
            return AbilityResult(false, "앞서 관찰한 플레이어가 사망해 더 이상 관찰을 이어갈 수 없습니다.")
        }

        mentalist.lastObservationTargetId = effectiveTarget.member.id
        mentalist.lastObservationSelectedTargetId = target.member.id
        mentalist.observationUsesToday += 1
        val isSameTeam = isSameTeam(previousTarget, effectiveTarget)
        if (!isSameTeam) {
            val remainingUses = MAX_OBSERVATION_USES_PER_DAY - mentalist.observationUsesToday
            val suffix = if (remainingUses > 0) {
                " 관찰을 다시 사용할 수 있습니다."
            } else {
                " 오늘 관찰 횟수를 모두 사용했습니다."
            }
            return AbilityResult(
                true,
                "관찰 결과: ${previousSelectedTarget.member.effectiveName}님과 ${target.member.effectiveName}님은 서로 **다른 팀**입니다.$suffix"
            )
        }

        mentalist.isObservationResolvedToday = true
        val initialSelectedTarget = mentalist.initialObservationSelectedTargetId
            ?.let(game::getPlayer)
            ?: initialTarget
        val profilingMessage = buildProfilingMessage(
            game = game,
            caster = caster,
            initialTarget = initialTarget,
            lastTarget = effectiveTarget,
            initialSelectedTarget = initialSelectedTarget,
            lastSelectedTarget = target
        )
        return AbilityResult(
            true,
            "관찰 결과: ${previousSelectedTarget.member.effectiveName}님과 ${target.member.effectiveName}님은 서로 **같은 팀**입니다.$profilingMessage"
        )
    }

    private fun isSameTeam(first: PlayerData, second: PlayerData): Boolean {
        val firstDisplayedTeam = InvestigationTeam.of(FrogCurseManager.displayedJob(first))
        val secondDisplayedTeam = InvestigationTeam.of(FrogCurseManager.displayedJob(second))
        return firstDisplayedTeam == secondDisplayedTeam
    }

    private fun buildProfilingMessage(
        game: Game,
        caster: PlayerData,
        initialTarget: PlayerData,
        lastTarget: PlayerData,
        initialSelectedTarget: PlayerData,
        lastSelectedTarget: PlayerData
    ): String {
        val hasProfiling = caster.allAbilities.any { it is Profiling }
        if (!hasProfiling) return ""

        val (profiledTarget, displayedTarget) = if (Random.nextBoolean()) {
            initialTarget to initialSelectedTarget
        } else {
            lastTarget to lastSelectedTarget
        }
        val usedTargetId = game.abilityTargetByUserThisPhase[profiledTarget.member.id]
            ?: game.lastNightAbilityTargetByUser[profiledTarget.member.id]
            ?: return "\n프로파일링 결과: ${displayedTarget.member.effectiveName}님의 능력 사용 대상을 확인할 수 없습니다."

        val usedTargetPlayerName = game.getPlayer(usedTargetId)?.member?.effectiveName ?: "알 수 없음"
        return "\n프로파일링 결과: ${displayedTarget.member.effectiveName}님은 ${usedTargetPlayerName}님에게 능력을 사용했습니다."
    }

    companion object {
        private const val MAX_OBSERVATION_USES_PER_DAY = 2

        fun resetDayState(owner: PlayerData) {
            val mentalist = owner.job as? Mentalist ?: return
            mentalist.initialObservationTargetId = null
            mentalist.lastObservationTargetId = null
            mentalist.initialObservationSelectedTargetId = null
            mentalist.lastObservationSelectedTargetId = null
            mentalist.isObservationResolvedToday = false
            mentalist.observationUsesToday = 0
        }
    }
}
