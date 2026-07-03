package org.beobma.mafia42discordproject.job.evil.list

import dev.kord.common.entity.Snowflake
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.thief.Condolences
import org.beobma.mafia42discordproject.job.ability.general.evil.list.thief.Successor
import org.beobma.mafia42discordproject.job.ability.general.evil.list.thief.ThiefAbility
import org.beobma.mafia42discordproject.job.evil.Evil

class Thief : Job(), Evil {
    override val name: String = "도둑"
    override val description: String = "[도벽] 투표시간에 최종적으로 투표한 플레이어의 고유능력을 밤까지 사용할 수 있다.\n[교련] 마피아 직업을 훔칠 경우, 마피아와 접선한다."
    override val jobImage: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(95).webp"
    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(ThiefAbility())

    var hasContactedMafia: Boolean = false
    var hasStolenPoliticianAbility: Boolean = false
    var hasStolenJudgeAbility: Boolean = false
    var hasUsedStolenVigilanteNightPurge: Boolean = false
    var hasRevealedStolenJudgeAuthority: Boolean = false
    var hasUsedStolenPoliceSearchThisNight: Boolean = false
    var stolenPoliceSearchedTargetIds: MutableSet<Snowflake> = mutableSetOf()
    var stolenDetectiveTargetId: Snowflake? = null
    var stolenHealTargetId: Snowflake? = null
    var stolenFortuneTargetId: Snowflake? = null
    var stolenThreatenedTargetIdsTonight: MutableSet<Snowflake> = mutableSetOf()
    var stolenRemainingThreatUsesTonight: Int = 1
    var stolenSpyRemainingIntelUsesTonight: Int = 1
    var stolenSpyLastInvestigatedTargetId: Snowflake? = null
    var hasContactedMafiaByStolenSpy: Boolean = false
    var hasContactedMafiaByStolenWitch: Boolean = false
    var stolenPriestResurrectionTargetId: Snowflake? = null
    var stolenMartyrNightBombTargetId: Snowflake? = null
    var stolenMartyrDefenseBombTargetId: Snowflake? = null
    var stolenHitmanFirstContractTargetId: Snowflake? = null
    var stolenHitmanFirstSelectedTargetId: Snowflake? = null
    var stolenHitmanFirstContractGuessedJobName: String? = null
    private var stolenAbility: JobUniqueAbility? = null

    fun setStolenAbility(ability: JobUniqueAbility?) {
        stolenAbility?.let { abilities.remove(it) }
        resetStolenAbilityState()
        stolenAbility = ability
        if (ability != null && ability !in abilities) {
            abilities += ability
        }
    }

    fun clearStolenAbility() {
        stolenAbility?.let { abilities.remove(it) }
        stolenAbility = null
        resetStolenAbilityState()
    }

    fun hasStolenAbility(name: String): Boolean {
        return stolenAbility?.name == name
    }

    fun hasCondolences(): Boolean {
        return extraAbilities.any { it is Condolences }
    }

    fun hasSuccessor(): Boolean {
        return extraAbilities.any { it is Successor }
    }

    private fun resetStolenAbilityState() {
        hasRevealedStolenJudgeAuthority = false
        hasUsedStolenPoliceSearchThisNight = false
        stolenPoliceSearchedTargetIds.clear()
        stolenDetectiveTargetId = null
        stolenHealTargetId = null
        stolenFortuneTargetId = null
        stolenThreatenedTargetIdsTonight.clear()
        stolenRemainingThreatUsesTonight = 1
        stolenSpyRemainingIntelUsesTonight = 1
        stolenSpyLastInvestigatedTargetId = null
        hasContactedMafiaByStolenSpy = false
        hasContactedMafiaByStolenWitch = false
        stolenPriestResurrectionTargetId = null
        stolenMartyrNightBombTargetId = null
        stolenMartyrDefenseBombTargetId = null
        stolenHitmanFirstContractTargetId = null
        stolenHitmanFirstSelectedTargetId = null
        stolenHitmanFirstContractGuessedJobName = null
    }
}
