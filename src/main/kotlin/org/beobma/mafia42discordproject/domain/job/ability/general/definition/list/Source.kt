package org.beobma.mafia42discordproject.job.ability.general.definition.list

import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.AssistanceCommonAbility
import org.beobma.mafia42discordproject.job.ability.CitizenCommonAbility

class Source : Ability, CitizenCommonAbility {
    override val name: String = "정보원"
    override val description: String = "낮이 끝날 때마다 살아남은 마피아팀이 몇 명인지 알 수 있다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484615945430372422/50d554c66a104c6a.png?ex=69bedfd8&is=69bd8e58&hm=a21cb0b8b632e9cfdef7fdbc7cf894ae7e9d69725bf306f10dea2e905766d7f5&"
}