package org.beobma.mafia42discordproject.job.ability.general.definition.list.reporter

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Reporter
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import kotlin.reflect.KClass

class Obituary : Ability, JobSpecificExtraAbility {
    override val name: String = "부고"
    override val description: String = "취재 대상이 사망하더라도 취재에 실패하지 않는다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484614716977578015/3f98729e1684ea16.png?ex=69bedeb3&is=69bd8d33&hm=a3b4c68f58f0f1085cbb381b8c7698dc2a0c1d4b5cd2c0be3d4df6c1b3713248&"
    override val targetJob: List<KClass<out Job>> = listOf(Reporter::class)
}