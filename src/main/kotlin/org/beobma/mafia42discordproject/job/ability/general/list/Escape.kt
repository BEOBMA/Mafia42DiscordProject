package org.beobma.mafia42discordproject.job.ability.general.list

import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.CommonAbility

class Escape : Ability, CommonAbility {
    override val name: String = "도주"
    override val description: String = "투표로 처형될 때 처형되지 않고 도주할 수 있지만, 다음날 투표시간이 시작될 때 사망한다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484596832561660104/5dc72f3cbc77df7f.png?ex=69bece0b&is=69bd7c8b&hm=ac88a035ecb2ede4f33adcd5b543073758d43830ebe8babd783a7254aefda132&"
}