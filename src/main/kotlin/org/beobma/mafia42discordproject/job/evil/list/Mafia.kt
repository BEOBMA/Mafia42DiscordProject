package org.beobma.mafia42discordproject.job.evil.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.MafiaAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.NightRaid
import org.beobma.mafia42discordproject.job.evil.Evil

class Mafia : Job(), Evil {
    override val name: String = "마피아"
    override val description: String = "[처형] 밤마다 한 명의 플레이어를 죽일 수 있으며 마피아끼리 대화가 가능하다."
    override val jobImage: String = "https://media.discordapp.net/attachments/1483977619258212392/1484548915419156551/chrome_AEPNiEMd38.png?ex=69bea16b&is=69bd4feb&hm=2528f68c5abd533fa450671cc5b7d9b5dd75c1d60eb4890cbfacd5f0597b477e&=&format=webp&quality=lossless"

    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(MafiaAbility(), NightRaid())
}
