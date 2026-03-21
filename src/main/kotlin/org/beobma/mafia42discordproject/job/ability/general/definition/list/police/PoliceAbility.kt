package org.beobma.mafia42discordproject.job.ability.general.definition.list.police

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.DiscoveryStep
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.game.system.PoliceSearchNotificationManager
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.definition.list.Police
import org.beobma.mafia42discordproject.job.evil.Evil

class PoliceAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "?섏깋"
    override val description: String = "諛ㅻ쭏???뚮젅?댁뼱 ??紐낆쓣 議곗궗?섏뿬 洹??뚮젅?댁뼱??留덊뵾???щ?瑜??뚯븘?????덈떎."
    override val image: String = ""
    override val usablePhase: GamePhase = GamePhase.NIGHT

    companion object {
        private val policeSearchScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "諛ㅼ뿉留??ъ슜?????덉뒿?덈떎.")
        }
        if (caster.state.isDead) {
            return AbilityResult(false, "二쎌? ?щ엺?먭쾶 ?ъ슜?????놁뒿?덈떎.")
        }

        val policeJob = caster.job as? Police
            ?: return AbilityResult(false, "寃쎌같???꾨떃?덈떎")

        if (policeJob.hasUsedSearchThisNight) {
            return AbilityResult(false, "")
        }

        if (target == null) {
            policeJob.currentSearchTarget = null
            return AbilityResult(true, null)
        }
        if (target.state.isDead) {
            return AbilityResult(false, "")
        }

        val searchEvent = GameEvent.PoliceSearchResolved(
            police = caster,
            target = target,
            isMafia = target.job is Evil,
            isRepeatedSearch = target.member.id in policeJob.searchedTargets
        )
        dispatchPoliceEvent(game, searchEvent)

        val warrant = caster.allAbilities.filterIsInstance<Warrant>().firstOrNull()
        val revealEvent = if (warrant?.shouldRevealJob(target.member.id, policeJob.searchedTargets) == true) {
            val actualJob = target.job ?: return AbilityResult(false, "")
            GameEvent.PoliceJobRevealed(
                police = caster,
                target = target,
                actualJob = actualJob,
                revealedJob = actualJob,
                resolvedAt = DiscoveryStep.NIGHT
            ).also { dispatchPoliceEvent(game, it) }
        } else {
            null
        }

        policeSearchScope.launch {
            PoliceSearchNotificationManager.notifyPoliceSearchResult(searchEvent)
            if (revealEvent != null) {
                PoliceSearchNotificationManager.notifyPoliceRevealedJob(revealEvent)
            }
        }

        policeJob.currentSearchTarget = null
        policeJob.hasUsedSearchThisNight = true
        policeJob.searchedTargets += target.member.id
        return AbilityResult(true, "")
    }

    private fun dispatchPoliceEvent(game: Game, event: GameEvent) {
        game.playerDatas
            .filter { !it.state.isDead }
            .forEach { player ->
                player.allAbilities
                    .filterIsInstance<PassiveAbility>()
                    .sortedByDescending(PassiveAbility::priority)
                    .forEach { passive ->
                        passive.onEventObserved(game, player, event)
                    }
            }
    }
}
