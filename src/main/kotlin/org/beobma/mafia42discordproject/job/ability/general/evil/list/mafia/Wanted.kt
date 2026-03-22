package org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia

import dev.kord.core.behavior.channel.createMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.FrogCurseManager
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.AssistanceCommonAbility
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.evil.Evil
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import kotlin.reflect.KClass

class Wanted : Ability, JobSpecificExtraAbility, PassiveAbility {
    override val name: String = "수배"
    override val description: String = "첫날 낮이 될 때 접선하지 않은 마피아팀의 직업을 알 수 있다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484621994967171233/8aa5d19667efd598.png?ex=69bee57a&is=69bd93fa&hm=8f960c819d3bfa5847139e455c0c80784eb40175c0fb27dcc5ebfecc55a6136c&"
    override val targetJob: List<KClass<out Job>> = listOf(Mafia::class)

    override fun onPhaseChanged(game: Game, owner: PlayerData, newPhase: GamePhase) {
        if (newPhase != GamePhase.DAY || game.dayCount != 1) return

        val unknownMafiaTeam = game.playerDatas.filter { candidate ->
            candidate != owner && candidate.job is Evil && candidate.job !is Mafia
        }

        val message = if (unknownMafiaTeam.isEmpty()) {
            "수배 결과: 접선하지 않은 마피아팀이 없습니다."
        } else {
            buildString {
                appendLine("수배 결과: 접선하지 않은 마피아팀 정보")
                unknownMafiaTeam.forEach { candidate ->
                    appendLine("${candidate.member.effectiveName}의 직업은 ${FrogCurseManager.displayedJob(candidate)?.name ?: "알 수 없음"}")
                }
            }
        }

        notificationScope.launch {
            runCatching {
                owner.member.getDmChannel().createMessage(message)
            }
        }
    }

    companion object {
        private val notificationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
