package org.beobma.mafia42discordproject.job.ability.general.evil.list.other

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.Spy
import org.beobma.mafia42discordproject.job.evil.list.Swindler
import kotlin.reflect.KClass

class BeautyTrap : Ability, JobSpecificExtraAbility {
    override val name: String = "미인계"
    override val description: String = "시민팀 플레이어의 능력 대상이 될 경우, 해당 플레이어의 직업을 알아낼 수 있다."
    override val targetJob: List<KClass<out Job>> = listOf(Spy::class, Swindler::class)
}