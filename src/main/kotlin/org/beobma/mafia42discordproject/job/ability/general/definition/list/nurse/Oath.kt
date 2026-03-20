package org.beobma.mafia42discordproject.job.ability.general.definition.list.nurse

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Detective
import org.beobma.mafia42discordproject.job.definition.list.Gangster
import org.beobma.mafia42discordproject.job.definition.list.Ghoul
import org.beobma.mafia42discordproject.job.definition.list.Martyr
import org.beobma.mafia42discordproject.job.definition.list.Nurse
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import kotlin.reflect.KClass

class Oath : Ability, JobSpecificExtraAbility {
    override val name: String = "선서"
    override val description: String = "게임 시작 시 의사에게 간호사의 존재 여부를 알린다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484611038132568064/8847c832546fb880.png?ex=69bedb46&is=69bd89c6&hm=e9eadc813d4d8ae7920549e434cb15be89d9c78e7873bef9ca2640a9fd5ad765&"
    override val targetJob: List<KClass<out Job>> = listOf(Nurse::class)
}