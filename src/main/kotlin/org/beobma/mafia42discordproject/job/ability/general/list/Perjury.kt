package org.beobma.mafia42discordproject.job.ability.general.list

import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.CommonAbility

class Perjury : Ability, CommonAbility {
    override val name: String = "위증"
    override val description: String = "매일 투표 시간에 연속으로 지목한 플레이어에게 가짜 투표를 행사할수 있다."
}