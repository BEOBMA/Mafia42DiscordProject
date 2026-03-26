package org.beobma.mafia42discordproject.job.evil.list

import dev.kord.common.entity.Snowflake
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.spy.SpyAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.spy.SpyAbilityTwo
import org.beobma.mafia42discordproject.job.evil.Evil

class Spy : Job(), Evil {
    override val name: String = "스파이"
    override val description: String = "[첩보] 밤마다 플레이어 한 명을 선택하여 직업을 알아낼 수 있다. 마피아와 접선할 경우, 한 번 더 능력을 사용할 수 있다.\n[접선] 밤에 선택한 플레이어가 마피아라면 접선한다."
    override val jobImage: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(90).webp"
    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(SpyAbilityTwo(), SpyAbility())

    var hasContactedMafia: Boolean = false
    var remainingIntelUsesTonight: Int = 1
    var lastInvestigatedTargetId: Snowflake? = null
    var hasTriggeredAssassin: Boolean = false
}
