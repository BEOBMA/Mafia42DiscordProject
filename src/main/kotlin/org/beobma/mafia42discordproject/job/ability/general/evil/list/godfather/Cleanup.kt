package org.beobma.mafia42discordproject.job.ability.general.evil.list.godfather

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.Godfather
import kotlin.reflect.KClass

class Cleanup : Ability, JobSpecificExtraAbility {
    override val name: String = "뒷처리"
    override val description: String = "마피아가 모두 사망하면 마피아와 접선한다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484616921726062692/aa5743249dc9263b.png?ex=69bee0c0&is=69bd8f40&hm=3a430feb9c8d7d4a3e2316f2d8becb0ff3d08e92d0e5ab10dd613e81ec1f4f85&"
    override val targetJob: List<KClass<out Job>> = listOf(Godfather::class)
}