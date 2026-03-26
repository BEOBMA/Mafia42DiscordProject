package org.beobma.mafia42discordproject.job.ability.general.list

import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.CommonAbility

class Will : Ability, CommonAbility {
    override val name: String = "유언"
    override val description: String = "밤에 유언을 작성할 수 있다. 밤에 사망할 경우 작성한 유언이 모두에게 공개된다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484602115803582665/55b9661b4e8da0fb.png?ex=69bed2f6&is=69bd8176&hm=7eec808b33d8f73ea92ba43df4e40fcf936fb83cdf96a3a0880215a3fc643993&"
}