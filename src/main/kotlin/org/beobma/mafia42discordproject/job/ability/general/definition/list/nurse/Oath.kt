package org.beobma.mafia42discordproject.job.ability.general.definition.list.nurse

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Detective
import org.beobma.mafia42discordproject.job.definition.list.Gangster
import org.beobma.mafia42discordproject.job.definition.list.Ghoul
import org.beobma.mafia42discordproject.job.definition.list.Martyr
import org.beobma.mafia42discordproject.job.definition.list.Nurse
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import kotlin.reflect.KClass

class Oath : Ability, JobSpecificExtraAbility {
    override val name: String = "선서"
    override val description: String = "게임 시작 시 의사에게 간호사의 존재 여부를 알린다."
    override val targetJob: List<KClass<out Job>> = listOf(Nurse::class)
}