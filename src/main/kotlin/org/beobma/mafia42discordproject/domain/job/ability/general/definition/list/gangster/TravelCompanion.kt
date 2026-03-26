package org.beobma.mafia42discordproject.job.ability.general.definition.list.gangster

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Gangster
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import kotlin.reflect.KClass

class TravelCompanion : Ability, JobSpecificExtraAbility {
    override val name: String = "길동무"
    override val description: String = "밤에 협박한 대상에게 사망할 경우 대상의 투표권을 영구히 박탈시킨다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484607683234627684/42217ec1944d31ac.png?ex=69bed826&is=69bd86a6&hm=222aac6fee0e7c1acf01ad8b1714594b154b71a045436635ec0ce9e7aa9c28d2&"
    override val targetJob: List<KClass<out Job>> = listOf(Gangster::class)
}