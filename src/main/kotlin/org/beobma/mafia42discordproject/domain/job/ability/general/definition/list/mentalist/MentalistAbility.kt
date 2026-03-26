package org.beobma.mafia42discordproject.job.ability.general.definition.list.mentalist

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.definition.list.Mentalist
import org.beobma.mafia42discordproject.job.evil.Evil
import kotlin.random.Random

class MentalistAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "관찰"
    override val description: String = "낮마다 다른 플레이어들의 대화를 선택해 서로 다른 팀인지 확인하고, 앞서 선택한 플레이어와 같은 팀이 나올 때까지 이를 반복한다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1485331806495445002/cbfdda837ac98b09.png?ex=69c17a8a&is=69c0290a&hm=fc20185670fafacd56beb977c862aea03ae7910bbaeb34b3ab767f70c48cae94&"
    override val usablePhase: GamePhase = GamePhase.DAY

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "관찰은 낮에만 사용할 수 있습니다.")
        }
        if (caster.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 능력을 사용할 수 없습니다.")
        }
        if (target == null) {
            return AbilityResult(false, "관찰할 대상을 지정해야 합니다.")
        }
        if (target.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 관찰 대상으로 지정할 수 없습니다.")
        }
        if (target.member.id == caster.member.id) {
            return AbilityResult(false, "자기 자신은 관찰 대상으로 지정할 수 없습니다.")
        }

        val mentalist = caster.job as? Mentalist
            ?: return AbilityResult(false, "심리학자만 사용할 수 있습니다.")

        if (mentalist.isObservationResolvedToday) {
            return AbilityResult(false, "오늘은 이미 같은 팀을 확인했습니다.")
        }

        val initialTargetId = mentalist.initialObservationTargetId
        if (initialTargetId == null) {
            mentalist.initialObservationTargetId = target.member.id
            mentalist.lastObservationTargetId = target.member.id
            return AbilityResult(
                true,
                "${target.member.effectiveName}님을 첫 관찰 대상으로 지정했습니다. 한 번 더 관찰을 사용해 다른 플레이어를 선택하세요."
            )
        }

        if (target.member.id == initialTargetId) {
            return AbilityResult(false, "처음 관찰한 플레이어와는 비교할 수 없습니다.")
        }

        val initialTarget = game.getPlayer(initialTargetId)
            ?: return AbilityResult(false, "처음 관찰한 플레이어 정보를 찾을 수 없습니다.")

        if (initialTarget.state.isDead) {
            return AbilityResult(false, "처음 관찰한 플레이어가 사망해 더 이상 관찰을 이어갈 수 없습니다.")
        }

        mentalist.lastObservationTargetId = target.member.id
        val isSameTeam = isSameTeam(initialTarget, target)
        if (!isSameTeam) {
            return AbilityResult(
                true,
                "관찰 결과: ${initialTarget.member.effectiveName}님과 ${target.member.effectiveName}님은 서로 **다른 팀**입니다. 관찰을 다시 사용할 수 있습니다."
            )
        }

        mentalist.isObservationResolvedToday = true
        val profilingMessage = buildProfilingMessage(game, caster, initialTarget, target)
        return AbilityResult(
            true,
            "관찰 결과: ${initialTarget.member.effectiveName}님과 ${target.member.effectiveName}님은 서로 **같은 팀**입니다.$profilingMessage"
        )
    }

    private fun isSameTeam(first: PlayerData, second: PlayerData): Boolean {
        val firstIsEvil = first.job is Evil
        val secondIsEvil = second.job is Evil
        return firstIsEvil == secondIsEvil
    }

    private fun buildProfilingMessage(
        game: Game,
        caster: PlayerData,
        initialTarget: PlayerData,
        lastTarget: PlayerData
    ): String {
        val hasProfiling = caster.allAbilities.any { it is Profiling }
        if (!hasProfiling) return ""

        val profiledTarget = if (Random.nextBoolean()) initialTarget else lastTarget
        val usedTargetId = game.abilityTargetByUserThisPhase[profiledTarget.member.id]
        if (usedTargetId == null) {
            return "\n프로파일링 결과: ${profiledTarget.member.effectiveName}님의 능력 사용 대상을 확인할 수 없습니다."
        }

        val usedTargetPlayerName = game.getPlayer(usedTargetId)?.member?.effectiveName ?: "알 수 없음"
        return "\n프로파일링 결과: ${profiledTarget.member.effectiveName}님은 ${usedTargetPlayerName}님에게 능력을 사용했습니다."
    }

    companion object {
        fun resetDayState(owner: PlayerData) {
            val mentalist = owner.job as? Mentalist ?: return
            mentalist.initialObservationTargetId = null
            mentalist.lastObservationTargetId = null
            mentalist.isObservationResolvedToday = false
        }
    }
}
