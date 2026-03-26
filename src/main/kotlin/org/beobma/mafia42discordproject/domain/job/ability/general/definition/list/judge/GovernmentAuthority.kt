package org.beobma.mafia42discordproject.job.ability.general.definition.list.judge

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Detective
import org.beobma.mafia42discordproject.job.definition.list.Gangster
import org.beobma.mafia42discordproject.job.definition.list.Ghoul
import org.beobma.mafia42discordproject.job.definition.list.Judge
import org.beobma.mafia42discordproject.job.definition.list.Martyr
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import kotlin.reflect.KClass

class GovernmentAuthority : Ability, JobSpecificExtraAbility {
    override val name: String = "관권"
    override val description: String = "투표에서 찬성한 플레이어가 누군지 알수 있다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484609185000984787/f5bbc56d540fcc1d.png?ex=69bed98c&is=69bd880c&hm=9928571c471b2ff593ea3f8401d995a094447718664c5c9947379e155ddf9eb0&"
    override val targetJob: List<KClass<out Job>> = listOf(Judge::class)
}