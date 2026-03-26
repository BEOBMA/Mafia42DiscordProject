package org.beobma.mafia42discordproject.job.ability.general.definition.list.administrator

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Administrator
import org.beobma.mafia42discordproject.job.definition.list.Detective
import org.beobma.mafia42discordproject.job.definition.list.Gangster
import org.beobma.mafia42discordproject.job.definition.list.Ghoul
import org.beobma.mafia42discordproject.job.definition.list.Martyr
import org.beobma.mafia42discordproject.job.definition.list.Nurse
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import kotlin.reflect.KClass

class Cooperation : Ability, JobSpecificExtraAbility {
    override val name: String = "공조"
    override val description: String = "'조회' 능력 대상으로 경찰계열 직업을 선택할 수 있다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484602546688360659/5bb5fcc7bbe162c1.png?ex=69bed35d&is=69bd81dd&hm=e23a7d1e2d3f522517108e0bfb78aca6b590581a48ba8e41690c2c33170d0388&"
    override val targetJob: List<KClass<out Job>> = listOf(Administrator::class)
}