package org.beobma.mafia42discordproject.job.ability.general.definition.list.mentalist

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Mentalist
import kotlin.reflect.KClass

class Profiling : Ability, JobSpecificExtraAbility {
    override val name: String = "프로파일링"
    override val description: String = "관찰을 통해 같은 팀 플레이어를 알아낸 경우, 처음 조사한 플레이어와 마지막에 조사한 플레이어 중 한 명의 능력사용여부를 알아낼 수 있다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484610542395199648/8414afa1be86efdc.png?ex=69bedad0&is=69bd8950&hm=f557a01fd52190590f26f323a5ce32c00c317d6c3291a4fe8e42b826e29b72ec&"
    override val targetJob: List<KClass<out Job>> = listOf(Mentalist::class)
}