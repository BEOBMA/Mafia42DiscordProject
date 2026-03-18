package org.beobma.mafia42discordproject.job.ability.general.evil.list

import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.CommonAbility
import org.beobma.mafia42discordproject.job.ability.EvilCommonAbility

class Instructions : Ability, EvilCommonAbility {
    override val name: String = "지령"
    override val description: String = "첫 번째 낮이 될 때 경찰 계열 직업이 누군지 알게 된다."
}