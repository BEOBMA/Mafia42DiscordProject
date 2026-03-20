package org.beobma.mafia42discordproject.job.ability.general.list

import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.CommonAbility

class Megaphone : Ability, CommonAbility {
    override val name: String = "확성기"
    override val description: String = "밤이 되었을 때 채팅을 한번 칠 수 있으며, 확성기를 사용한 모든 유저중 한 명의 메세지가 노출된다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484598278627528834/d897fbfc5ea95b0f.png?ex=69becf64&is=69bd7de4&hm=8a7feb07c8426ed67236cf0592e482e813546012dfe9e355535a60a4c249f2ee&"
}