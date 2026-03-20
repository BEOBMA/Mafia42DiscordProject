package org.beobma.mafia42discordproject.job.ability.general.definition.list.magician

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Detective
import org.beobma.mafia42discordproject.job.definition.list.Gangster
import org.beobma.mafia42discordproject.job.definition.list.Ghoul
import org.beobma.mafia42discordproject.job.definition.list.Magician
import org.beobma.mafia42discordproject.job.definition.list.Martyr
import org.beobma.mafia42discordproject.job.definition.list.Nurse
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import kotlin.reflect.KClass

class Xray : Ability, JobSpecificExtraAbility {
    override val name: String = "투시"
    override val description: String = "트릭에 성공했을 때 그 사람의 직업을 알아낸다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484609707800006696/ef27e03db22614fc.png?ex=69beda09&is=69bd8889&hm=3fceb92df342429867d92987869002106fabcbcde892525ccbe4ba9a745ebc73&"
    override val targetJob: List<KClass<out Job>> = listOf(Magician::class)
}