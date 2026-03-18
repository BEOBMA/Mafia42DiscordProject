package org.beobma.mafia42discordproject.job.ability.general.list

import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.CommonAbility

class Megaphone : Ability, CommonAbility {
    override val name: String = "확성기"
    override val description: String = "밤이 되었을 때 채팅을 한번 칠 수 있으며, 확성기를 사용한 모든 유저중 한 명의 메세지가 노출된다."
}