package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.politician.PoliticianAbility
import org.beobma.mafia42discordproject.job.definition.Definition

class Politician : Job(), Definition {
    override val name: String = "정치인"
    override val description: String = "[처세] 플레이어간의 투표로 처형당하지 않는다.\n[논객] 정치인의 투표권은 두 표로 인정된다."
    override val jobImage: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(77).webp"
    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(PoliticianAbility())
}