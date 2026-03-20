package org.beobma.mafia42discordproject.job.ability.general.list

import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.CommonAbility

class SecretLetter : Ability, CommonAbility {
    override val name: String = "밀서"
    override val description: String = "게임에서 1회에 한해 받은 사람만이 볼 수 있는 편지를 보낼 수 있다. 편지는 낮이 될 때 도착한다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484601709803343932/36be57cd11ad1686.png?ex=69bed296&is=69bd8116&hm=59c8a6640abee34e17eb92e490536672ec1ddc7c2492417755f94bfe2b0d68a7&"
}