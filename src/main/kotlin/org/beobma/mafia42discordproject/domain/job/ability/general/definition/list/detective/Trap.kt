package org.beobma.mafia42discordproject.job.ability.general.definition.list.detective

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Detective
import org.beobma.mafia42discordproject.job.definition.list.Gangster
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import kotlin.reflect.KClass

class Trap : Ability, JobSpecificExtraAbility {
    override val name: String = "함정"
    override val description: String = "추리능력의 대상이 된 플레이어가 자신에게 능력을 사용한 경우 직업을 알아낸다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484605681704243220/69b1eea05d64b6ec.png?ex=69bed649&is=69bd84c9&hm=3c3ec053daf18bb2ee71813b08067a1833bfbe50d8f84ac382af05a626dff069&"
    override val targetJob: List<KClass<out Job>> = listOf(Detective::class)
}