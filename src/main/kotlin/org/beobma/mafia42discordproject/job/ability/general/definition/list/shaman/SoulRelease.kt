package org.beobma.mafia42discordproject.job.ability.general.definition.list.shaman

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.FrogCurseManager
import org.beobma.mafia42discordproject.game.system.ShamaningPolicy
import org.beobma.mafia42discordproject.game.system.SystemImage
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.list.EarthboundSpirit

class SoulRelease : ActiveAbility, JobUniqueAbility {
    override val name: String = "성불"
    override val description: String = "죽은 플레이어의 대화를 들을 수 있으며 밤마다 죽은 플레이어 한 명을 성불할 수 있다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(129).webp"
    override val usablePhase: GamePhase = GamePhase.NIGHT

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "성불은 밤에만 사용할 수 있습니다.")
        }
        if (caster.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 성불 능력을 사용할 수 없습니다.")
        }
        if (caster.state.hasUsedDailyAbility) {
            return AbilityResult(false, "오늘 밤에는 이미 성불을 사용했습니다.")
        }
        if (target == null) {
            return AbilityResult(false, "성불할 대상을 지정해야 합니다.")
        }
        if (!target.state.isDead) {
            return AbilityResult(false, "죽은 플레이어만 성불할 수 있습니다.")
        }
        if (target.state.isShamaned) {
            return AbilityResult(false, "이미 성불 상태인 플레이어입니다.")
        }
        val isEarthbound = target.allAbilities.any { it is EarthboundSpirit }
        if (!isEarthbound && ShamaningPolicy.canBeShamaned(target)) {
            target.state.isShamaned = true
        }
        caster.state.hasUsedDailyAbility = true

        CoroutineScope(Dispatchers.Default).launch {
            runCatching {
                caster.member.getDmChannel().createMessage(
                    if (isEarthbound) {
                        "${target.member.effectiveName}님을 성불하는데 실패했습니다.\n$image"
                    } else {
                        "${target.member.effectiveName}님을 성불하였습니다.\n$image"
                    }
                )
            }
            runCatching {
                if (!isEarthbound) {
                    target.member.getDmChannel().createMessage("성불되었습니다.\n$image")
                }
            }
        }

        val revealedJobName = FrogCurseManager.displayedJob(target)?.name ?: "알 수 없음"
        return if (isEarthbound) {
            AbilityResult(true, "$revealedJobName ${target.member.effectiveName}님을 성불하는데 실패했습니다.")
        } else {
            AbilityResult(true, "$revealedJobName ${target.member.effectiveName}님을 성불했습니다.")
        }
    }
}
