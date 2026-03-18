package org.beobma.mafia42discordproject.job.ability.general.definition.list.couple

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Couple
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import kotlin.reflect.KClass

class Dedication : Ability, JobSpecificExtraAbility {
    override val name: String = "헌신"
    override val description: String = "짝 연인과 같은 대상을 투표할 경우, 1표가 추가된다."
    override val targetJob: List<KClass<out Job>> = listOf(Couple::class)
}