package org.beobma.mafia42discordproject.job.ability.general.list

import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.CommonAbility

class Perjury : Ability, CommonAbility {
    override val name: String = "위증"
    override val description: String = "매일 투표 시간에 가짜 투표를 행사할수 있다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484601242490765442/ebd40e8167c7523b.png?ex=69bed226&is=69bd80a6&hm=361f2bc2a5c32d5d3986d909ed6a5e8446d8b204737d123d507be5bd2471f428&"
}