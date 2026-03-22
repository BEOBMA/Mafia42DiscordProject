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
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484617166769619115/c961af185036ec4d.png?ex=69c0327b&is=69bee0fb&hm=96aee2d3d1d101b58d1d11f8da270f26b1ef3a8d31dd05d65105c1cb679fa06a&"
    override val targetJob: List<KClass<out Job>> = listOf(HitMan::class)
}