package org.beobma.mafia42discordproject.job.ability.general.definition.list.cabal

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Cabal
import org.beobma.mafia42discordproject.job.definition.list.Detective
import org.beobma.mafia42discordproject.job.definition.list.Gangster
import org.beobma.mafia42discordproject.job.definition.list.Ghoul
import org.beobma.mafia42discordproject.job.definition.list.Martyr
import org.beobma.mafia42discordproject.job.definition.list.Nurse
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import kotlin.reflect.KClass

class Marker : Ability, JobSpecificExtraAbility {
    override val name: String = "표식"
    override val description: String = "해 비밀결사가 달 비밀결사를 알아낸 상태에서 상대 비밀결사가 사망할 경우, 누가 죽였는지 알아낸다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484603773698441439/18d7f67f75074b03.png?ex=69bed482&is=69bd8302&hm=0877aa20fde7957934d23e40fadd5e81198202886d5ff91861d2e1da0870a72b&"
    override val targetJob: List<KClass<out Job>> = listOf(Cabal::class)
}