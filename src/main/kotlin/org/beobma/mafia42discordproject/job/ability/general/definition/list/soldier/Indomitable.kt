package org.beobma.mafia42discordproject.job.ability.general.definition.list.soldier

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import kotlin.reflect.KClass

class Indomitable : Ability, JobSpecificExtraAbility {
    override val name: String = "불굴"
    override val description: String = "'방탄' 능력을 2번 발동시킬 수 있다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484615312656699572/19124858af12393d.png?ex=69bedf41&is=69bd8dc1&hm=ec533a06a0323124943d78d020a241b70e16b5ceb7a2b5328f596aea384f01f3&"
    override val targetJob: List<KClass<out Job>> = listOf(Soldier::class)
}