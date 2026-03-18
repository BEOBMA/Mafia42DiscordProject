package org.beobma.mafia42discordproject.job.ability.general.evil.list.beastman

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.Beastman
import kotlin.reflect.KClass

class Roar : Ability, JobSpecificExtraAbility {
    override val name: String = "포효"
    override val description: String = "첫 번째 낮이 될 때 마피아에게 짐승인간의 포효가 들려 자신의 존재가 알려진다."
    override val targetJob: List<KClass<out Job>> = listOf(Beastman::class)
}