package org.beobma.mafia42discordproject.job.ability.general.evil.list.godfather

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.Godfather
import kotlin.reflect.KClass

class Cleanup : Ability, JobSpecificExtraAbility {
    override val name: String = "뒷처리"
    override val description: String = "마피아가 모두 사망하면 마피아와 접선한다."
    override val targetJob: List<KClass<out Job>> = listOf(Godfather::class)
}