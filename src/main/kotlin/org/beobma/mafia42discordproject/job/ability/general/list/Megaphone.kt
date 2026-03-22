package org.beobma.mafia42discordproject.job.ability.general.list

import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.CommonAbility

class Megaphone : Ability, CommonAbility {
    override val name: String = "확성기"
    override val description: String = "이번 밤에 확성기 능력을 사용한 사람이 없을 경우, 게임에서 단 한번만 모두를 향해 채팅을 사용할 수 있다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484598278627528834/d897fbfc5ea95b0f.png?ex=69becf64&is=69bd7de4&hm=8a7feb07c8426ed67236cf0592e482e813546012dfe9e355535a60a4c249f2ee&"
}