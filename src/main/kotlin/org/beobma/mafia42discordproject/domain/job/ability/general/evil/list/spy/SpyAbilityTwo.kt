package org.beobma.mafia42discordproject.job.ability.general.evil.list.spy

import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility

class SpyAbilityTwo : PassiveAbility, JobUniqueAbility {
    override val name: String = "첩보"
    override val description: String = "밤에 선택한 플레이어가 마피아라면 접선한다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1485291286893297746/a3b7182ec7981a2f.png?ex=69c154ce&is=69c0034e&hm=fbbc1736dbb1e47414d6d10852a060fde6eb97c3d44f614fc6e03284a00a484a&"
}
