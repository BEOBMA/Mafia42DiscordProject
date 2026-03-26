package org.beobma.mafia42discordproject.job.ability.general.definition.list.soldier

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import kotlin.reflect.KClass

class MentalStrength : Ability, JobSpecificExtraAbility {
    override val name: String = "정신력"
    override val description: String = "자신에게 발동되는 해로운 효과를 무시한다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484615492462448762/1e776621b2c935b4.png?ex=69bedf6c&is=69bd8dec&hm=3ceb701d265be7093e8c523765dc102022890d421625e01d04d698b5a42ce9aa&"
    override val targetJob: List<KClass<out Job>> = listOf(Soldier::class)
}