package org.beobma.mafia42discordproject.job.ability.general.definition.list.doctor

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.AttackTier
import org.beobma.mafia42discordproject.game.system.DefenseTier
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.WinOrDead

class Heal : ActiveAbility, JobUniqueAbility {
    override val name: String = "치료"
    override val description: String = "밤이 되면 플레이어 한 명을 처형으로부터 치료한다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484626955612455174/bef253042e3dab28.png?ex=69beea19&is=69bd9899&hm=99b25a7815280b9c866654be45d3ae55eb1a37c4229fc2216f78e4ca341d49ab&"
    override val usablePhase: GamePhase = GamePhase.NIGHT

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "치료는 밤에만 사용할 수 있습니다.")
        }
        if (caster.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 능력을 사용할 수 없습니다.")
        }
        if (caster.state.isSilenced) {
            return AbilityResult(false, "매혹 상태에서는 치료를 사용할 수 없습니다.")
        }
        if (target == null) {
            return AbilityResult(false, "치료할 대상을 지정해야 합니다.")
        }
        if (target.state.isDead) {
            return AbilityResult(false, "이미 사망한 플레이어는 치료할 수 없습니다.")
        }

        val doctorJob = caster.job as? Doctor
            ?: return AbilityResult(false, "")
        doctorJob.currentHealTarget = target.member.id

        // 박애가 있으면 적용
        target.state.healTier = maxOf(target.state.healTier, healEvent.defenseTier)
        caster.state.hasUsedDailyAbility = true

        // 밤 이벤트 큐에 등록
        game.nightEvents += healEvent
        return AbilityResult(true, "${target.member.effectiveName}님을 치료 대상으로 지정했습니다.")
    }
}
