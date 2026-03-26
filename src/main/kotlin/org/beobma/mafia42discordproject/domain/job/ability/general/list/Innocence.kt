package org.beobma.mafia42discordproject.job.ability.general.list

import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.CommonAbility

class Innocence : Ability, CommonAbility {
    override val name: String = "결백"
    override val description: String = "내가 다른 팀 플레이어를 투표했을 때 그 사람이 나를 투표하지 않았을 경우, 결백상태가 되어 나에 대한 투표가 무산된다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484597328097841173/4322d797038d16ec.png?ex=69bece81&is=69bd7d01&hm=273ac8750f92082cea3205698a04c1db05f25bbd507e364da00f934e8a85bf4f&"
}