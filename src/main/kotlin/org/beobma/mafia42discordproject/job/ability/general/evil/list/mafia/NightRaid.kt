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

    private val nightRaidImageUrl = "https://cdn.discordapp.com/attachments/1483977619258212392/1485026577174761552/tDrhfZgWgty6Q9kFMqfxr5HQnMMP1cWYYjcba66zLfZvXZvXZJMeNNApMoL7i6j90o9jlSpbqmwYOyTZ8qOvauWdOVICQ2IDYLbjhfrYQeBTYqzmU6bD49K46HsE4mRxANcYFfHoQWDPAn8dnyB4WQ.webp?ex=69c05e46&is=69bf0cc6&hm=3eadc3e0fc151e40539a1da614eac05591ffdb2f8eadd62a0a083c206accd9a0&"

    override fun onEventObserved(game: Game, owner: PlayerData, event: GameEvent) {
        if (event !is GameEvent.ResolveDawnPresentation) return
        if (event.dayCount != 1) return

        val mafiaAttack = event.attacks.firstOrNull { it.attacker == owner } ?: return
        if (mafiaAttack.target.job !is Doctor) return
        mafiaAttack.target.state.isJobPubliclyRevealed = true

        event.presentation = DawnPresentation(
            imageUrl = nightRaidImageUrl,
            message = "의사 ${mafiaAttack.target.member.effectiveName}님이 마피아의 야습으로 사망하였습니다."
        )
    }
}
