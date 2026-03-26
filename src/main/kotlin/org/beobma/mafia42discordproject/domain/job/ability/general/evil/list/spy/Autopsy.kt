package org.beobma.mafia42discordproject.job.ability.general.evil.list.spy

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.AssistanceCommonAbility
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import org.beobma.mafia42discordproject.job.evil.list.Spy
import kotlin.reflect.KClass

class Autopsy : Ability, JobSpecificExtraAbility {
    override val name: String = "부검"
    override val description: String = "사망한 플레이어가 생길때마다 해당 플레이어를 자동으로 조사한다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484612503357886595/bcb3e39815879915.png?ex=69bedca3&is=69bd8b23&hm=9d5f86f26466c4be0cffb0e28811e6887dec348aa8fffc3520723b942d682c5c&"
    override val targetJob: List<KClass<out Job>> = listOf(Spy::class)
}