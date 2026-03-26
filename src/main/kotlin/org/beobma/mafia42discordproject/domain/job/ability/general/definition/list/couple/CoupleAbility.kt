package org.beobma.mafia42discordproject.job.ability.general.definition.list.couple

import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility

class CoupleAbility : JobUniqueAbility, PassiveAbility {
    override val name: String = "연애"
    override val description: String = "밤이 되면 둘만의 대화가 가능하며 두 명 모두 살아있는 상태에서 밤에 한 명이 처형당하면 상대방이 대신 죽게된다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1485327994720358612/660992f34b9f1947.png?ex=69c176fe&is=69c0257e&hm=56de1dc9a59cea3987e0cbc395fffab51ae09ed0e4ff7cded2521e64484dd542&"
}
