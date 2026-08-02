package org.beobma.mafia42discordproject.job.ability.general.definition.list.police

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GameLoopManager
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.DiscoveryStep
import org.beobma.mafia42discordproject.game.system.FrogCurseManager
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.game.system.HackerRedirectManager
import org.beobma.mafia42discordproject.game.system.PoliceSearchPolicy
import org.beobma.mafia42discordproject.game.system.notifications.PoliceNotificationManager
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.definition.list.Police
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import org.beobma.mafia42discordproject.job.evil.list.Thief

class PoliceAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "수색"
    override val description: String = "밤마다 플레이어 한 명을 조사하여 그 플레이어의 마피아 여부를 알아낼 수 있다."
    override val image: String = ""
    override val usablePhase: GamePhase = GamePhase.NIGHT

    companion object {
        private val policeSearchScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "밤에만 사용할 수 있습니다.")
        }
        if (caster.state.isDead) {
            return AbilityResult(false, "죽은 사람에게 사용할 수 없습니다.")
        }

        val policeJob = caster.job as? Police
        val thiefJob = caster.job as? Thief
        if (policeJob == null && thiefJob == null) {
            return AbilityResult(false, "경찰 또는 수색 능력을 훔친 도둑만 사용할 수 있습니다.")
        }

        if (policeJob?.hasUsedSearchThisNight == true || thiefJob?.hasUsedStolenPoliceSearchThisNight == true) {
            return AbilityResult(false, "이미 이번 밤에 수색 능력을 사용했습니다.")
        }

        if (target == null) {
            policeJob?.currentSearchTarget = null
            return AbilityResult(true, null)
        }
        if (target.state.isDead) {
            return AbilityResult(false, "죽은 플레이어는 수색할 수 없습니다.")
        }

        val effectiveTarget = HackerRedirectManager.resolveTarget(game, target) ?: target
        val searchedTargets = (policeJob?.searchedTargets ?: thiefJob?.stolenPoliceSearchedTargetIds).orEmpty()
        val searchEvent = GameEvent.PoliceSearchResolved(
            police = caster,
            target = effectiveTarget,
            isMafia = PoliceSearchPolicy.isMafia(effectiveTarget),
            isRepeatedSearch = effectiveTarget.member.id in searchedTargets
        )
        dispatchPoliceEvent(game, searchEvent)
        policeSearchScope.launch {
            PoliceNotificationManager.notifySearchResult(caster, searchEvent)
        }
        if (thiefJob != null && effectiveTarget.job is Mafia && !thiefJob.hasContactedMafia) {
            thiefJob.hasContactedMafia = true
            policeSearchScope.launch {
                runCatching {
                    GameLoopManager.announceMafiaSupportContact(
                        game,
                        caster,
                        "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(26).webp",
                        "도둑"
                    )
                }
                runCatching { GameLoopManager.refreshMafiaChannelContactState(game) }
            }
        }

        val warrant = caster.allAbilities.filterIsInstance<Warrant>().firstOrNull()
        if (warrant?.shouldRevealJob(effectiveTarget.member.id, searchedTargets) == true) {
            val actualJob = effectiveTarget.job ?: return AbilityResult(false, "대상의 직업 정보를 확인할 수 없습니다.")
            val revealedJob = FrogCurseManager.displayedJob(effectiveTarget) ?: actualJob

            val revealEvent = GameEvent.PoliceJobRevealed(
                police = caster,
                target = effectiveTarget,
                actualJob = actualJob,
                revealedJob = revealedJob,
                resolvedAt = DiscoveryStep.NIGHT
            )
            dispatchPoliceEvent(game, revealEvent)
            game.privateDisplayedJobNamesByObserver
                .getOrPut(caster.member.id, ::mutableMapOf)[effectiveTarget.member.id] = revealEvent.revealedJob.name

            policeSearchScope.launch {
                PoliceNotificationManager.notifyWarrantResult(caster, revealEvent)
            }
        }

        policeJob?.currentSearchTarget = null
        policeJob?.hasUsedSearchThisNight = true
        policeJob?.eavesdroppingTargetId = effectiveTarget.member.id
        policeJob?.searchedTargets?.add(effectiveTarget.member.id)
        thiefJob?.hasUsedStolenPoliceSearchThisNight = true
        thiefJob?.stolenPoliceSearchedTargetIds?.add(effectiveTarget.member.id)
        return AbilityResult(true, "수색 대상을 결정했습니다.")
    }

    private fun dispatchPoliceEvent(game: Game, event: GameEvent) {
        game.playerDatas
            .filter { !it.state.isDead }
            .forEach { player ->
                player.allAbilities
                    .filterIsInstance<PassiveAbility>()
                    .filterNot { FrogCurseManager.shouldSuppressPassive(player) }
                    .sortedByDescending(PassiveAbility::priority)
                    .forEach { passive ->
                        passive.onEventObserved(game, player, event)
                    }
            }
    }
}
