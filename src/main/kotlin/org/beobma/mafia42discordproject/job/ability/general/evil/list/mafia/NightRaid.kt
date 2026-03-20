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
    override val description: String = "첫날 밤 의사를 처형 대상으로 지정하면 새벽 연출을 바꿉니다."

    private val nightRaidImageUrl =
        "https://cdn.discordapp.com/attachments/1483977619258212392/1483980246448603146/99cb963d1b44dc2e.png?ex=69bc8fcd&is=69bb3e4d&hm=51de46f9128d899572989dc0deb0717d66fd93097e5feac91386e9db0901461d&"

    override fun onEventObserved(game: Game, owner: PlayerData, event: GameEvent) {
        if (event !is GameEvent.ResolveDawnPresentation) return
        if (event.dayCount != 1) return

        val mafiaAttack = event.attacks.firstOrNull { it.attacker == owner } ?: return
        if (mafiaAttack.target.job !is Doctor) return

        event.presentation = DawnPresentation(
            imageUrl = nightRaidImageUrl,
            message = "야습이 발동해 새벽 연출이 변경되었습니다."
        )
    }
}
