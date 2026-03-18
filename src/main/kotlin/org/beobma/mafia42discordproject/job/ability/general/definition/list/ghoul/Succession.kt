package org.beobma.mafia42discordproject.job.ability.general.definition.list.ghoul

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Detective
import org.beobma.mafia42discordproject.job.definition.list.Gangster
import org.beobma.mafia42discordproject.job.definition.list.Ghoul
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import kotlin.reflect.KClass

class Succession : Ability, JobSpecificExtraAbility {
    override val name: String = "계승"
    override val description: String = "도굴한 플레이어의 모든 고유능력을 가져온다."
    override val targetJob: List<KClass<out Job>> = listOf(Ghoul::class)
}