package org.beobma.mafia42discordproject.job.ability.general.definition.list.police

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Police
import kotlin.reflect.KClass

class Confidential : Ability, JobSpecificExtraAbility {
    override val name: String = "기밀"
    override val description: String = "두 번째 밤 시작 시에 다른 플레이어 중 한 명을 조사한다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484612718559498350/7207e8c524f40089.png?ex=69bedcd6&is=69bd8b56&hm=16391b630f1f509497351b08cd954bb61b1acc388ea119ff071b30cdbfe77fbe&"
    override val targetJob: List<KClass<out Job>> = listOf(Police::class)
}