package org.beobma.mafia42discordproject.job.ability.general.definition.list

import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.CitizenCommonAbility
import org.beobma.mafia42discordproject.job.ability.CommonAbility
import org.beobma.mafia42discordproject.job.ability.EvilCommonAbility

class Belongings : Ability, CitizenCommonAbility {
    override val name: String = "유품"
    override val description: String = "사망 시 자신의 직업을 모든 플레이어에게 공개한다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484615716589146293/2e8483c1f09c9b5a.png?ex=69bedfa1&is=69bd8e21&hm=72eced6e613c729ba6f2d6ee0ffafc1c9ae03d1c81c6dbbed44e6846b2455bc9&"
}