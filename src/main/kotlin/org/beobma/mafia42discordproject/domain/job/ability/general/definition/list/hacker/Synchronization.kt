package org.beobma.mafia42discordproject.job.ability.general.definition.list.hacker

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Hacker
import kotlin.reflect.KClass

class Synchronization : Ability, JobSpecificExtraAbility {
    override val name: String = "동기화"
    override val description: String = "해킹당한 대상이 시민 팀일 경우, 해커의 존재를 대상에게 알려준다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484608633168859216/b22b96783839683c.png?ex=69bed908&is=69bd8788&hm=d489002fbf94ccf5754f9b44f6d14c7ced7e8fc67e1060f3673a4c85e6b065aa&"
    override val targetJob: List<KClass<out Job>> = listOf(Hacker::class)
}