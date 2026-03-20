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
    override val description: String = "첫번째 밤에 치료 상태의 의사를 처형할 경우, 치료 효과를 무시하며 의사의 정체가 모두에게 밝혀진다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484619710573318174/a89a721f40d8cc96.png?ex=69bee359&is=69bd91d9&hm=bcad2d87a15ee16ee6c14b1eb8d0a5af03d6d752407be56b74deec30680d8a9e&"

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
