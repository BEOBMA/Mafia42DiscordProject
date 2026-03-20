package org.beobma.mafia42discordproject.job.ability.general.definition.list.other

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Cabal
import org.beobma.mafia42discordproject.job.definition.list.Nurse
import kotlin.reflect.KClass

class Postmortem : Ability, JobSpecificExtraAbility {
    override val name: String = "검시"
    override val description: String = "자신의 능력과 관련된 접선하지 않은 직업을 가진 사람이 사망한 경우, 그 사실을 알게 된다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484604356010446869/69fc4adcdda5b9d3.png?ex=69bed50d&is=69bd838d&hm=cad42bc4a83f774ed0b9357eadcdbfef75cc4d736b46c9bdc3f65a1d39dceb74&"
    override val targetJob: List<KClass<out Job>> = listOf(Nurse::class, Cabal::class)
}