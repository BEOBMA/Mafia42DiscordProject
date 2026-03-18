package org.beobma.mafia42discordproject.job.ability.general.list

import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.CommonAbility

class Will : Ability, CommonAbility {
    override val name: String = "유언"
    override val description: String = "밤에 유언을 작성할 수 있다. 밤에 사망할 경우 작성한 유언이 모두에게 공개된다."
}