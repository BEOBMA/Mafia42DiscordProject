package org.beobma.mafia42discordproject.job.ability.general.definition.list.cabal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import dev.kord.core.behavior.channel.createMessage
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.definition.list.Cabal
import org.beobma.mafia42discordproject.job.definition.list.CabalRole
import kotlin.reflect.KClass

class Marker : Ability, JobSpecificExtraAbility, PassiveAbility {
    override val name: String = "표식"
    override val description: String = "해 비밀결사가 달 비밀결사를 알아낸 상태에서 상대 비밀결사가 사망할 경우, 누가 죽였는지 알아낸다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484603773698441439/18d7f67f75074b03.png?ex=69bed482&is=69bd8302&hm=0877aa20fde7957934d23e40fadd5e81198202886d5ff91861d2e1da0870a72b&"
    override val targetJob: List<KClass<out Job>> = listOf(Cabal::class)

    companion object {
        private val markerDmScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    override fun onEventObserved(game: Game, owner: PlayerData, event: GameEvent) {
        if (event !is GameEvent.PlayerDied) return
        if (event.isLynch) return

        val cabal = owner.job as? Cabal ?: return
        if (cabal.role != CabalRole.SUN || !cabal.hasFoundMoon) return
        val moonId = cabal.pairedPlayerId ?: return
        if (event.victim.member.id != moonId) return
        if (!cabal.notifiedPartnerDeathIds.add(moonId)) return

        val killer = game.nightAttacks.values
            .firstOrNull { it.target.member.id == moonId }
            ?.attacker
            ?.member
            ?.effectiveName
            ?: "알 수 없음"

        markerDmScope.launch {
            runCatching {
                owner.member.getDmChannel().createMessage("비밀결사 ${event.victim.member.effectiveName}님이 ${killer}님에게 사망하였습니다.")
            }
        }
    }
}
