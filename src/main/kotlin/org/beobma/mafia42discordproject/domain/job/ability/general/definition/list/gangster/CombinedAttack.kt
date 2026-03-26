package org.beobma.mafia42discordproject.job.ability.general.definition.list.gangster

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Gangster
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import kotlin.reflect.KClass

class CombinedAttack : Ability, JobSpecificExtraAbility {
    override val name: String = "연타"
    override val description: String = "전날 협박한 대상을 협박할 경우, 한 번 더 공갈 능력을 사용할 수 있다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484607366048518217/3bcaf32cb617caf7.png?ex=69bed7da&is=69bd865a&hm=05ae4a232d232eb715c66c6bee41a4c8e7eb720804e8485d91d3b458b9d1b238&"
    override val targetJob: List<KClass<out Job>> = listOf(Gangster::class)
}