package org.beobma.mafia42discordproject.job.ability.general.evil.list.thief

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.Thief
import kotlin.reflect.KClass

class Condolences : Ability, JobSpecificExtraAbility {
    override val name: String = "조문"
    override val description: String = "최근 하루 내에 죽은 사람에게도 '도벽' 능력을 사용할 수 있다."
    override val targetJob: List<KClass<out Job>> = listOf(Thief::class)
}