package org.beobma.mafia42discordproject.job.ability.general.list

import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.CommonAbility

class MindReading : Ability, CommonAbility {
    override val name: String = "독심술"
    override val description: String = "자신에게 투표한 유저를 알 수 있다."
}