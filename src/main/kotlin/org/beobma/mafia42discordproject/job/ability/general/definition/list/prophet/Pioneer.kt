package org.beobma.mafia42discordproject.job.ability.general.definition.list.prophet

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Prophet
import kotlin.reflect.KClass

class Pioneer : Ability, JobSpecificExtraAbility {
    override val name: String = "선각자"
    override val description: String = "능력이 발동되기 전날 밤에 처형 당하더라도 계시 능력이 발동된다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484614205117173810/d397201b5fd906e4.png?ex=69bede39&is=69bd8cb9&hm=f9030fa4034da2e09c1389f93bf272ae22b720cbf85d6e31f79cb95721f70f68&"
    override val targetJob: List<KClass<out Job>> = listOf(Prophet::class)
}