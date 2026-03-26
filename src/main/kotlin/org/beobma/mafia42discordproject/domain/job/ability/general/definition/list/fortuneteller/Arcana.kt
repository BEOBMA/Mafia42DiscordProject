package org.beobma.mafia42discordproject.job.ability.general.definition.list.fortuneteller

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Detective
import org.beobma.mafia42discordproject.job.definition.list.Fortuneteller
import org.beobma.mafia42discordproject.job.definition.list.Gangster
import org.beobma.mafia42discordproject.job.definition.list.Ghoul
import org.beobma.mafia42discordproject.job.definition.list.Martyr
import org.beobma.mafia42discordproject.job.definition.list.Nurse
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import kotlin.reflect.KClass

class Arcana : Ability, JobSpecificExtraAbility {
    override val name: String = "아르카나"
    override val description: String = "운세로 확인한 직업이 누구의 것인지 유추할 수 있다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484607103590203402/5dd86ba8c8237677.png?ex=69bed79c&is=69bd861c&hm=176f98ba86a415efd53057306ad1e24950bc57490df8bf76a82c84c02cc560df&"
    override val targetJob: List<KClass<out Job>> = listOf(Fortuneteller::class)
}