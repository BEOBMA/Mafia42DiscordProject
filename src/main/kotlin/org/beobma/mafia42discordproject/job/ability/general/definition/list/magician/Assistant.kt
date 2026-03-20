package org.beobma.mafia42discordproject.job.ability.general.definition.list.magician

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Detective
import org.beobma.mafia42discordproject.job.definition.list.Gangster
import org.beobma.mafia42discordproject.job.definition.list.Ghoul
import org.beobma.mafia42discordproject.job.definition.list.Magician
import org.beobma.mafia42discordproject.job.definition.list.Martyr
import org.beobma.mafia42discordproject.job.definition.list.Nurse
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import kotlin.reflect.KClass

class Assistant : Ability, JobSpecificExtraAbility {
    override val name: String = "조수"
    override val description: String = "'트릭' 능력이 발동되지 않은 상태에서 '트릭' 대상이 사망할 경우, 대상을 변경할 수 있다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484609477314482207/2a3351a81714c877.png?ex=69bed9d2&is=69bd8852&hm=fe9122ab8a9f10e6dab0bf4a00701b3e57fc90b8f1b3545fbc4ae184d624c737&"
    override val targetJob: List<KClass<out Job>> = listOf(Magician::class)
}