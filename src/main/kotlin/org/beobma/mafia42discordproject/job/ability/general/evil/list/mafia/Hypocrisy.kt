package org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.AssistanceCommonAbility
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import kotlin.reflect.KClass

class Hypocrisy : Ability, JobSpecificExtraAbility {
    override val name: String = "위선"
    override val description: String = "첫번째 밤에 시민팀에 의해 직업을 조사받을 경우, 의사 직업으로 판정된다."
    override val targetJob: List<KClass<out Job>> = listOf(Mafia::class)
}