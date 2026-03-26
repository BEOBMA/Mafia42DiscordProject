package org.beobma.mafia42discordproject.job.ability.general.definition.list.politician

import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility

class PoliticianAbility : Ability, JobUniqueAbility {
    override val name: String = "처세"
    override val description: String = "투표할 때 2표로 취급되며 투표로 처형되지 않는다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1485333205115732119/44a0db4f01b72bf1.png?ex=69c17bd8&is=69c02a58&hm=a6ca3e56d253a57c20ba109247e2cc92074cfce6d212340053cf7df3eebb9f8c&"
}