package org.beobma.mafia42discordproject.job.ability.general.definition.list.ghoul

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.definition.list.Ghoul
import kotlin.reflect.KClass

class Ghost : JobSpecificExtraAbility, PassiveAbility {
    override val name: String = "망령"
    override val description: String = "첫번째 낮에 도굴당한 사람이 하는 첫번째 말을 들을 수 있다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(167).webp"
    override val targetJob: List<KClass<out Job>> = listOf(Ghoul::class)
    override val priority: Int = 8

    override fun onDeceasedChat(game: Game, owner: PlayerData, event: GameEvent) {
        val deceasedChatEvent = event as? GameEvent.DeceasedChat ?: return
        if (deceasedChatEvent.dayCount != 1) return
        if (owner.state.isDead) return

        val ghoulId = owner.member.id
        if (ghoulId in game.ghostTriggeredGhouls) return

        val robbedTargetId = game.graveRobTargetsByGhoul[ghoulId] ?: return
        if (deceasedChatEvent.chatSender.member.id != robbedTargetId) return

        game.ghostTriggeredGhouls += ghoulId

        CoroutineScope(Dispatchers.Default).launch {
            runCatching {
                val dm = owner.member.getDmChannelOrNull() ?: owner.member.getDmChannel()
                dm.createMessage("[망령] ${deceasedChatEvent.chatSender.member.effectiveName}: ${deceasedChatEvent.chat}")
            }
        }
    }
}
