package org.beobma.mafia42discordproject.job.ability.general.list

import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.CommonAbility

class Escape : Ability, CommonAbility {
    override val name: String = "도주"
    override val description: String = "투표로 처형될 때 처형되지 않고 도주할 수 있지만, 다음날 투표시간이 시작될 때 사망한다."
}