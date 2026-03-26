package org.beobma.mafia42discordproject.job.definition.list

import dev.kord.common.entity.Snowflake
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.martyr.MartyrDefenseBombAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.martyr.MartyrNightBombAbility
import org.beobma.mafia42discordproject.job.definition.Definition

class Martyr : Job(), Definition {
    override val name: String = "테러리스트"
    override val description: String = "[자폭] 마피아를 지목하고 있는 상태에서 마피아에게 처형당할 때 대상과 함께 사망한다.\n[산화] 투표로 인해 처형될 때 적 팀을 지목했다면 대상과 함께 사망한다."
    override val jobImage: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(66).webp"
    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(MartyrNightBombAbility(), MartyrDefenseBombAbility())

    var nightBombTargetId: Snowflake? = null
    var defenseBombTargetId: Snowflake? = null
}
