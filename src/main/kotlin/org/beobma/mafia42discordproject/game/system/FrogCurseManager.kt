package org.beobma.mafia42discordproject.game.system

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.JobManager
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.cabal.MoonCabalAbility
import org.beobma.mafia42discordproject.job.definition.list.Cabal
import org.beobma.mafia42discordproject.job.definition.list.CabalRole
import org.beobma.mafia42discordproject.job.definition.list.Frog
import org.beobma.mafia42discordproject.job.definition.list.MentalPatient
import org.beobma.mafia42discordproject.job.evil.list.Mafia

object FrogCurseManager {
    fun isCursed(player: PlayerData): Boolean = player.state.isFrogCursed

    fun applyCurse(
        target: PlayerData,
        currentDay: Int,
        hiddenFromTarget: Boolean = false,
        hallucinatedAsMafia: Boolean = false
    ) {
        target.state.isFrogCursed = true
        target.state.frogCurseExpiresAfterDay = currentDay
        target.state.isFrogCurseHiddenFromSelf = hiddenFromTarget
        target.state.isFrogHallucinatedAsMafia = hallucinatedAsMafia
    }

    fun clearExpiredAtNightStart(game: Game) {
        game.playerDatas.forEach { player ->
            val expiresAfterDay = player.state.frogCurseExpiresAfterDay ?: return@forEach
            if (expiresAfterDay < game.dayCount) {
                player.state.isFrogCursed = false
                player.state.frogCurseExpiresAfterDay = null
                player.state.isFrogCurseHiddenFromSelf = false
                player.state.isFrogHallucinatedAsMafia = false
            }
        }
    }

    fun displayedJob(target: PlayerData): Job? {
        val actualJob = target.job ?: return null
        if (isCursed(target) && target.state.isFrogHallucinatedAsMafia) return Mafia()
        if (isCursed(target)) return Frog()
        target.state.forcedDisplayedJobName
            ?.let(JobManager::findByName)
            ?.let { return it }
        return SwindlerManager.disguisedJobOf(target) ?: (actualJob as? MentalPatient)?.displayedJob ?: actualJob
    }

    fun canUseActiveAbility(caster: PlayerData, ability: ActiveAbility): Boolean {
        if (!isCursed(caster)) return true
        if (caster.state.hasCompletedGraveRobbing) return false
        if (caster.job is Mafia) return true
        val cabal = caster.job as? Cabal
        return cabal?.role == CabalRole.MOON && ability is MoonCabalAbility
    }

    fun shouldSuppressPassive(player: PlayerData): Boolean {
        return shouldSuppressAbilities(
            job = player.job,
            isFrogCursed = isCursed(player),
            hasCompletedGraveRobbing = player.state.hasCompletedGraveRobbing
        )
    }

    fun abilityBlockedMessage(player: PlayerData): String {
        return if (player.state.isFrogCurseHiddenFromSelf) {
            "현재 능력을 사용할 수 없습니다."
        } else {
            "개구리 상태에서는 능력을 사용할 수 없습니다."
        }
    }

    fun shouldSuppressAbilities(
        job: Job?,
        isFrogCursed: Boolean,
        hasCompletedGraveRobbing: Boolean = false
    ): Boolean {
        return isFrogCursed && (job !is Mafia || hasCompletedGraveRobbing)
    }
}
