package org.beobma.mafia42discordproject.job.ability.general.definition.list.paparazzi

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Detective
import org.beobma.mafia42discordproject.job.definition.list.Gangster
import org.beobma.mafia42discordproject.job.definition.list.Ghoul
import org.beobma.mafia42discordproject.job.definition.list.Martyr
import org.beobma.mafia42discordproject.job.definition.list.Nurse
import org.beobma.mafia42discordproject.job.definition.list.Paparazzi
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import kotlin.reflect.KClass

class Tact : Ability, JobSpecificExtraAbility {
    override val name: String = "눈치"
    override val description: String = "파파라치가 이슈의 대상이 되었을 경우, 자신을 알아낸 플레이어의 직업을 알아낸다."
    override val targetJob: List<KClass<out Job>> = listOf(Paparazzi::class)
}