package org.beobma.mafia42discordproject.job.ability.general.evil.list.beastman

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.Beastman
import kotlin.reflect.KClass

class Barbarism : Ability, JobSpecificExtraAbility {
    override val name: String = "야만성"
    override val description: String = "첫 번째 밤에 두 명에게 표식을 새길 수 있다."
    override val targetJob: List<KClass<out Job>> = listOf(Beastman::class)
}