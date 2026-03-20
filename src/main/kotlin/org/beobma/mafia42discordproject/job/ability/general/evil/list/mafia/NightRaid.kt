package org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia

import org.beobma.mafia42discordproject.game.DawnPresentation
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.definition.list.Doctor

class NightRaid : JobUniqueAbility, PassiveAbility {
    override val name: String = "야습"
    override val description: String = "첫 날 처형하는 대상이 치료 상태의 의사일 경우, 치료 효과를 무시하며 의사의 정체가 모두에게 알려진다."

    private val nightRaidImageUrl = ""

    override fun onEventObserved(game: Game, owner: PlayerData, event: GameEvent) {
        if (event !is GameEvent.ResolveDawnPresentation) return
        if (event.dayCount != 1) return

        val mafiaAttack = event.attacks.firstOrNull { it.attacker == owner } ?: return
        if (mafiaAttack.target.job !is Doctor) return

        event.presentation = DawnPresentation(
            imageUrl = nightRaidImageUrl,
            message = "의사 ${mafiaAttack.target.member.nickname}님이 마피아의 야습으로 사망하였습니다."
        )
    }
}
