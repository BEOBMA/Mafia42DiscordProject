package org.beobma.mafia42discordproject.job.ability.general.definition.list.couple

import dev.kord.common.entity.Snowflake
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.definition.list.Couple
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import kotlin.reflect.KClass

class Resentment : Ability, JobSpecificExtraAbility, PassiveAbility {
    override val name: String = "원한"
    override val description: String = "짝 연인을 죽인 마피아를 투표한다면 2표로 인정된다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484605387817615430/1b1f1e997651b741.png?ex=69bed603&is=69bd8483&hm=621d53b47ad63cba9661bb2686316978d01775a43c49fd62d224d11b9574d51d&"
    override val targetJob: List<KClass<out Job>> = listOf(Couple::class)

    override fun onEventObserved(game: Game, owner: PlayerData, event: GameEvent) {
        if (event !is GameEvent.CalculateVoteWeight) return
        if (event.voter.member.id != owner.member.id) return

        val ownerCouple = owner.job as? Couple ?: return
        val targetId = game.currentMainVotes[owner.member.id] ?: return
        val target = game.getPlayer(Snowflake(targetId)) ?: return
        if (target.job !is Mafia) return

        if (target.member.id in ownerCouple.avengedMafiaIds) {
            event.weight += 1
        }
    }
}
