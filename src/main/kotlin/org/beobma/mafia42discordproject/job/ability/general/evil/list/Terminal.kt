package org.beobma.mafia42discordproject.job.ability.general.evil.list

import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.CommonAbility
import org.beobma.mafia42discordproject.job.ability.EvilCommonAbility

class Terminal : Ability, EvilCommonAbility {
    override val name: String = "시한부"
    override val description: String = "플레이어의 절반 +2번째 밤까지 생존한 경우, 본인이 소속된 팀이 승리한다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484624944699539557/7c4b7683218c22e8.png?ex=69bee839&is=69bd96b9&hm=a94ea035470fcad1d9549b9b930cbe5994f92642719532dcc09ed13b6f9375c2&"
}