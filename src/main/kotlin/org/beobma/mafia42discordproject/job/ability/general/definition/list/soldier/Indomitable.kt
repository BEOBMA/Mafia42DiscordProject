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
    override val targetJob: List<KClass<out Job>> = listOf(Soldier::class)
}