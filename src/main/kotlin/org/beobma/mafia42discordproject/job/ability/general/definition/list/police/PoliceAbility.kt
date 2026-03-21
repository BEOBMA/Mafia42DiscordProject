package org.beobma.mafia42discordproject.job.ability.general.definition.list.police

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.definition.list.Police

class PoliceAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "수색"
    override val description: String = "밤마다 플레이어 한 명을 조사하여 그 플레이어의 마피아 여부를 알아낼 수 있다."
    override val image: String = ""
    override val usablePhase: GamePhase = GamePhase.NIGHT

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "밤에만 사용할 수 있습니다.")
        }
        if (caster.state.isDead) {
            return AbilityResult(false, "죽은 사람에게 사용할 수 없습니다.")
        }


        val policeJob = caster.job as? Police
            ?: return AbilityResult(false, "경찰이 아닙니다")

        if (target == null) {
            policeJob.currentSearchTarget = null
            return AbilityResult(true, null)
        }
        if (target.state.isDead) {
            return AbilityResult(false, "")
        }

        policeJob.currentSearchTarget = target.member.id
        return AbilityResult(true, "")
    }
}
