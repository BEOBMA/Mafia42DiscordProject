package org.beobma.mafia42discordproject.job.ability.general.definition.list.shaman

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import kotlin.reflect.KClass

class ShamanAbilityOne : PassiveAbility, JobUniqueAbility {
    override val name: String = "접신"
    override val description: String = "죽은 사람이 하는 채팅을 들을 수 있으며, 밤에 죽은 사람과 대화를 할 수 있다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484615080682197172/e7773b6b4df39801.png?ex=69bedf0a&is=69bd8d8a&hm=07211110ed28045f754c927fdf77ba500ecf212d43f69cf3dbbcae56d0788725&"

    override fun onDeceasedChat(game: Game, owner: PlayerData, event: GameEvent) {

    }
}