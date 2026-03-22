package org.beobma.mafia42discordproject.job.ability.general.evil.list.beastman

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.Beastman
import kotlin.reflect.KClass

class Barbarism : Ability, JobSpecificExtraAbility {
    override val name: String = "야만성"
    override val description: String = "첫 번째 밤에 두 명에게 표식을 새길 수 있다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484616453519970404/c2b4a4e028c1f3ed.png?ex=69c031d1&is=69bee051&hm=24779bc0089d8942e7302c7319e81ab95903a933a34429fbbc53f9628a4dc97f&"
    override val targetJob: List<KClass<out Job>> = listOf(Beastman::class)
}