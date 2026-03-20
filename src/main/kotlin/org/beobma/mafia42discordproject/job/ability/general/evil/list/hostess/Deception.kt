package org.beobma.mafia42discordproject.job.ability.general.evil.list.hostess

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.Hostess
import kotlin.reflect.KClass

class Deception : Ability, JobSpecificExtraAbility {
    override val name: String = "현혹"
    override val description: String = "마담이 사망해도 유혹이 해제되지 않는다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484617546434089071/1b3c5067374128aa.png?ex=69bee155&is=69bd8fd5&hm=427c18965bfd0634ad9607e498042ce5453f86a56f2fb9f5ec6ea6a7aa4e36a8&"
    override val targetJob: List<KClass<out Job>> = listOf(Hostess::class)
}