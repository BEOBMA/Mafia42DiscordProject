package org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.AssistanceCommonAbility
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import kotlin.reflect.KClass

class Exorcism : Ability, JobSpecificExtraAbility {
    override val name: String = "퇴마"
    override val description: String = "마피아팀이 아닌 플레이어를 처형한 후 성불한다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484618941929226493/e70049479e45220b.png?ex=69bee2a2&is=69bd9122&hm=04fc52abd78146d032607795775774b3a67365219f3845763ea8088c224d15cd&"
    override val targetJob: List<KClass<out Job>> = listOf(Mafia::class)
}