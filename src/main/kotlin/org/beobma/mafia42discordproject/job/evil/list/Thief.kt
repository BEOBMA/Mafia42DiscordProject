package org.beobma.mafia42discordproject.job.evil.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.thief.Condolences
import org.beobma.mafia42discordproject.job.ability.general.evil.list.thief.Successor
import org.beobma.mafia42discordproject.job.ability.general.evil.list.thief.ThiefAbility
import org.beobma.mafia42discordproject.job.evil.Evil

class Thief : Job(), Evil {
    override val name: String = "도둑"
    override val description: String = "[도벽] 투표시간마다 원하는 플레이어의 표식을 클릭해 그 사람의 고유능력을 밤까지 사용할 수 있다.\n[교련] 마피아 직업을 훔칠 경우, 마피아와 접선한다."
    override val jobImage: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(95).webp"
    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(ThiefAbility())

    var hasContactedMafia: Boolean = false
    var hasStolenPoliticianAbility: Boolean = false
    var hasStolenJudgeAbility: Boolean = false
    private var stolenAbility: JobUniqueAbility? = null

    fun setStolenAbility(ability: JobUniqueAbility?) {
        stolenAbility?.let { abilities.remove(it) }
        stolenAbility = ability
        if (ability != null && ability !in abilities) {
            abilities += ability
        }
    }

    fun clearStolenAbility() {
        stolenAbility?.let { abilities.remove(it) }
        stolenAbility = null
    }

    fun hasCondolences(): Boolean {
        return extraAbilities.any { it is Condolences }
    }

    fun hasSuccessor(): Boolean {
        return extraAbilities.any { it is Successor }
    }
}
