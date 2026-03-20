package org.beobma.mafia42discordproject.job.ability.general.definition.list.priest

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Police
import org.beobma.mafia42discordproject.job.definition.list.Priest
import kotlin.reflect.KClass

class Blessing : Ability, JobSpecificExtraAbility {
    override val name: String = "축복"
    override val description: String = "소생 능력 대상의 직업을 보존한다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484613542245044264/501826f04133a4c9.png?ex=69bedd9b&is=69bd8c1b&hm=be2821f540d7d7844b211e578271cc475f88ccf854c6c3a353ad7eebcd567e04&"
    override val targetJob: List<KClass<out Job>> = listOf(Priest::class)
}