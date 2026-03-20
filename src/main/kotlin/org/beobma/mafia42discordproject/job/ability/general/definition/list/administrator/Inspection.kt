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

class Inspection : Ability, JobSpecificExtraAbility {
    override val name: String = "감사"
    override val description: String = "이번 게임에 존재하는 다른 시민 팀 직업 하나를 아는 상태로 시작한다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484603054866038955/516d713e99cc4e63.png?ex=69bed3d6&is=69bd8256&hm=6d20b2888c7414499f20e43df82ad67b63346eb3652be1d84c9e3ca0e24c18c4&"
    override val targetJob: List<KClass<out Job>> = listOf(Administrator::class)
}