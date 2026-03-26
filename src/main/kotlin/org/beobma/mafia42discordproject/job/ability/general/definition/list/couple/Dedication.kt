package org.beobma.mafia42discordproject.job.ability.general.definition.list.couple

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.definition.list.Couple
import org.beobma.mafia42discordproject.job.definition.list.CoupleRole
import kotlin.reflect.KClass

class Dedication : Ability, JobSpecificExtraAbility, PassiveAbility {
    override val name: String = "헌신"
    override val description: String = "짝 연인과 같은 대상을 투표할 경우, 1표가 추가된다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(114).webp"
    override val targetJob: List<KClass<out Job>> = listOf(Couple::class)

    override fun onEventObserved(game: Game, owner: PlayerData, event: GameEvent) {
        if (event !is GameEvent.CalculateVoteWeight) return
        if (event.voter.member.id != owner.member.id) return

        val ownerCouple = owner.job as? Couple ?: return
        if (ownerCouple.role != CoupleRole.MALE) return
        val partnerId = ownerCouple.pairedPlayerId ?: return
        val partner = game.getPlayer(partnerId) ?: return
        if (partner.state.isDead) return

        val ownerVote = game.currentMainVotes[owner.member.id] ?: return
        val partnerVote = game.currentMainVotes[partner.member.id] ?: return
        if (ownerVote == partnerVote) {
            event.weight += 1
        }
    }
}
