package org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.AssistanceCommonAbility
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import kotlin.reflect.KClass

class WinOrDead : Ability, JobSpecificExtraAbility {
    override val name: String = "승부수"
    override val description: String = "마피아가 모두 죽고 혼자 남았을 경우, 그날 밤 대상을 무조건 처형시킨다."
    override val targetJob: List<KClass<out Job>> = listOf(Mafia::class)
}