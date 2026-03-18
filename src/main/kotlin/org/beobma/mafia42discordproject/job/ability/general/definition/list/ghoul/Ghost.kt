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

class Ghost : Ability, JobSpecificExtraAbility {
    override val name: String = "망령"
    override val description: String = "첫번째 낮에 도굴당한 사람이 하는 첫번째 말을 들을 수 있다."
    override val targetJob: List<KClass<out Job>> = listOf(Ghoul::class)
}