package org.beobma.mafia42discordproject.job.ability.general.evil.list.hitman

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.HitMan
import org.beobma.mafia42discordproject.job.evil.list.Hostess
import kotlin.reflect.KClass

class Intuition : Ability, JobSpecificExtraAbility {
    override val name: String = "직감"
    override val description: String = "플레이어를 지목할 때마다 직업을 맞췄는지 여부를 알 수 있다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484617166769619115/c961af185036ec4d.png?ex=69bee0fb&is=69bd8f7b&hm=2cf6d1d6c04ccb862c08a10e853fdf1d974918a8eba3faca0e865f54bbcd5de8&"
    override val targetJob: List<KClass<out Job>> = listOf(HitMan::class)
}