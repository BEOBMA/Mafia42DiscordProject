package org.beobma.mafia42discordproject.job.ability.general.evil.list

import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.CommonAbility
import org.beobma.mafia42discordproject.job.ability.EvilCommonAbility

class Terminal : Ability, EvilCommonAbility {
    override val name: String = "시한부"
    override val description: String = "플레이어의 절반 +2번째 밤까지 생존한 경우, 본인이 소속된 팀이 승리한다."
}