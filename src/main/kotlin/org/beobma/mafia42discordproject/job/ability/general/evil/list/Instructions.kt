package org.beobma.mafia42discordproject.job.ability.general.evil.list

import dev.kord.core.behavior.channel.createMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.replay.GameReplayLogger
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.EvilCommonAbility
import org.beobma.mafia42discordproject.job.definition.list.Agent
import org.beobma.mafia42discordproject.job.definition.list.Police
import org.beobma.mafia42discordproject.job.definition.list.Vigilante
import org.beobma.mafia42discordproject.job.evil.list.Villain

class Instructions : Ability, EvilCommonAbility {
    override val name: String = "지령"
    override val description: String = "첫 번째 낮이 될 때 경찰 계열 직업이 누군지 알게 된다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(187).webp"

    companion object {
        private val notificationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        fun notifyAtFirstDay(game: Game, owner: PlayerData) {
            if (game.dayCount != 1) return
            if (owner.state.isDead) return
            if (owner.state.hasReceivedInstructionsNoticeFirstDay) return
            if (owner.job is Villain) return
            if (owner.allAbilities.none { it is Instructions }) return

            owner.state.hasReceivedInstructionsNoticeFirstDay = true

            val policePlayers = game.playerDatas.filter { target ->
                !target.state.isDead && (target.job is Police || target.job is Agent || target.job is Vigilante)
            }

            notificationScope.launch {
                runCatching {
                    val dm = owner.member.getDmChannel()
                    if (policePlayers.isEmpty()) {
                        val message = "경찰 계열 직업이 없습니다."
                        GameReplayLogger.logDirectMessage(game, owner, message, "지령 결과")
                        dm.createMessage(message)
                        return@runCatching
                    }

                    val lines = policePlayers.joinToString("\n") { target ->
                        "${target.member.effectiveName}은 경찰 계열 직업."
                    }
                    GameReplayLogger.logDirectMessage(game, owner, lines, "지령 결과")
                    dm.createMessage(lines)
                }
            }
        }
    }
}
