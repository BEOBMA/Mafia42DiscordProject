package org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.AssistanceCommonAbility
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import kotlin.reflect.KClass

class NightRaid : Ability, JobSpecificExtraAbility {
    override val name: String = "야습"
    override val description: String = "첫 날 처형하는 대상이 치료 상태의 의사일 경우, 치료 효과를 무시하며 의사의 정체가 모두에게 알려진다."
    override val targetJob: List<KClass<out Job>> = listOf(Mafia::class)
}