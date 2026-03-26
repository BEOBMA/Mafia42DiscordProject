package org.beobma.mafia42discordproject.job.ability.general.list

import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.CommonAbility

class SecretLetter : Ability, CommonAbility {
    override val name: String = "밀서"
    override val description: String = "게임에서 1회에 한해 받은 사람만이 볼 수 있는 편지를 보낼 수 있다. 편지는 낮이 될 때 도착한다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484685904667873380/662ad7bbd34636f1.png?ex=69bf20ff&is=69bdcf7f&hm=c7837766b08ea0dfc2ebe7329ae27f1f463df8ba2d36cdcb3563b9fffab18d67&"
}