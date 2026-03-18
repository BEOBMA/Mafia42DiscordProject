package org.beobma.mafia42discordproject.job.ability.general.definition.list

import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.AssistanceCommonAbility
import org.beobma.mafia42discordproject.job.ability.CitizenCommonAbility

class Source : Ability, CitizenCommonAbility {
    override val name: String = "정보원"
    override val description: String = "낮이 끝날 때마다 살아남은 마피아팀이 몇 명인지 알 수 있다."
}