package org.beobma.mafia42discordproject.job.ability.general.definition.list.paparazzi

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Detective
import org.beobma.mafia42discordproject.job.definition.list.Gangster
import org.beobma.mafia42discordproject.job.definition.list.Ghoul
import org.beobma.mafia42discordproject.job.definition.list.Martyr
import org.beobma.mafia42discordproject.job.definition.list.Nurse
import org.beobma.mafia42discordproject.job.definition.list.Paparazzi
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import kotlin.reflect.KClass

class Tact : Ability, JobSpecificExtraAbility {
    override val name: String = "눈치"
    override val description: String = "파파라치가 이슈의 대상이 되었을 경우, 자신을 알아낸 플레이어의 직업을 알아낸다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484612170179280946/505a26d228a4ded5.png?ex=69bedc54&is=69bd8ad4&hm=b2a1b5d10985cefb4c05a20e87d5fdac33d8e34f61c93fa5c6d168d4be77065c&"
    override val targetJob: List<KClass<out Job>> = listOf(Paparazzi::class)
}