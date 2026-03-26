package org.beobma.mafia42discordproject.job.ability.general.definition.list.mercenary

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Detective
import org.beobma.mafia42discordproject.job.definition.list.Gangster
import org.beobma.mafia42discordproject.job.definition.list.Ghoul
import org.beobma.mafia42discordproject.job.definition.list.Martyr
import org.beobma.mafia42discordproject.job.definition.list.Mercenary
import org.beobma.mafia42discordproject.job.definition.list.Nurse
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import kotlin.reflect.KClass

class Tracking : Ability, JobSpecificExtraAbility {
    override val name: String = "추적"
    override val description: String = "의뢰인이 밤에 사망한 다음날 밤 처음으로 지목한 플레이어가 의뢰인을 죽였을 경우, 그 플레이어의 직업을 알아낸다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484610768107339796/db867be542b53978.png?ex=69bedb05&is=69bd8985&hm=1a63fe06ac2356981489d5fc158bbbd47e5ccdb2d1986a63148cff203cf74ab6&"
    override val targetJob: List<KClass<out Job>> = listOf(Mercenary::class)
}