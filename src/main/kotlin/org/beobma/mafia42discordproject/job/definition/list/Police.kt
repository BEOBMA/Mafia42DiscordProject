package org.beobma.mafia42discordproject.job.definition.list

import dev.kord.common.entity.Snowflake
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.police.PoliceAbility
import org.beobma.mafia42discordproject.job.definition.Definition

class Police : Job(), Definition {
    override val name: String = "경찰"
    override val description: String = "[수색] 밤마다 플레이어 한 명을 조사하여 그 플레이어의 마피아 여부를 알아낼 수 있다."
    override val jobImage: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(86).webp"
    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(PoliceAbility())

    var currentSearchTarget: Snowflake? = null
    var eavesdroppingTargetId: Snowflake? = null
    var hasUsedSearchThisNight: Boolean = false
    var hasUsedConfidential: Boolean = false
    val searchedTargets: MutableSet<Snowflake> = mutableSetOf()
}
