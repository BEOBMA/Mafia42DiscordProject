package org.beobma.mafia42discordproject.job.ability.general.evil.list.witch

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.Witch
import kotlin.reflect.KClass

class Oblivion : Ability, JobSpecificExtraAbility {
    override val name: String = "망각술"
    override val description: String = "저주받은 플레이어가 자신이 저주받은 사실을 알지 못하며 밤이 종료될 때 저주가 걸린다."
    override val targetJob: List<KClass<out Job>> = listOf(Witch::class)
}