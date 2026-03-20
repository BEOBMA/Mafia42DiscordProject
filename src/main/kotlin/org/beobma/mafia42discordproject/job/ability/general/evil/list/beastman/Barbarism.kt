package org.beobma.mafia42discordproject.job.ability.general.evil.list.beastman

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.Beastman
import kotlin.reflect.KClass

class Barbarism : Ability, JobSpecificExtraAbility {
    override val name: String = "야만성"
    override val description: String = "첫 번째 밤에 두 명에게 표식을 새길 수 있다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484616453519970404/c2b4a4e028c1f3ed.png?ex=69bee051&is=69bd8ed1&hm=590a6ad3cf47dcbd94cd888816e2eebc88ea3a1f3db1a835e4b21540025c57dd&"
    override val targetJob: List<KClass<out Job>> = listOf(Beastman::class)
}