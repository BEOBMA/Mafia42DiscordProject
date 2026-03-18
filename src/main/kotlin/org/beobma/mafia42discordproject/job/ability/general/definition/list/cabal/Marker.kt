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
    override val targetJob: List<KClass<out Job>> = listOf(Cabal::class)
}