package org.beobma.mafia42discordproject.job.ability.general.evil.list

import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.CommonAbility
import org.beobma.mafia42discordproject.job.ability.EvilCommonAbility

class Password : Ability, EvilCommonAbility {
    override val name: String = "암구호"
    override val description: String = "암구호 채팅은 자신의 팀에게만 보이게 된다. 이 채팅은 낮이 된 직후의 제한을 무시할 수 있다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484624744883159210/fceef70d021b6baa.png?ex=69bee80a&is=69bd968a&hm=0f101e4c676038de28768a198e9f2d7400e1ee7418be17c81f25666b49f90d3b&"
}