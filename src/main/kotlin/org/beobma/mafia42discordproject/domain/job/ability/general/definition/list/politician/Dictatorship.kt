package org.beobma.mafia42discordproject.job.ability.general.definition.list.politician

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Politician
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import kotlin.reflect.KClass

class Dictatorship : Ability, JobSpecificExtraAbility {
    override val name: String = "독재"
    override val description: String = "같은 팀 플레이어가 모두 사망하였을 경우, 모든 투표는 정치인에 의해서 결정된다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484613249352863894/f8eac7381bf4c462.png?ex=69bedd55&is=69bd8bd5&hm=6e2446d0629af5922b98a5a7d9570b83543e883037e81b15d1ea1a34ccb2e25e&"
    override val targetJob: List<KClass<out Job>> = listOf(Politician::class)
}