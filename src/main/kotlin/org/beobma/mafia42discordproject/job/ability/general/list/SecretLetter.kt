package org.beobma.mafia42discordproject.job.ability.general.list

import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.CommonAbility

class SecretLetter : Ability, CommonAbility {
    override val name: String = "밀서"
    override val description: String = "게임에서 단 한번 받은 사람만이 볼 수 있는 편지를 보낼 수 있다."
}