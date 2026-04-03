package org.beobma.mafia42discordproject.job.ability.general.definition.list.politician

import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility

class PoliticianAbility : Ability, JobUniqueAbility {
    override val name: String = "처세"
    override val description: String = "투표할 때 2표로 취급되며 투표로 처형되지 않는다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(14).png"
}