package org.beobma.mafia42discordproject.job.ability.general.definition.list.judge

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Detective
import org.beobma.mafia42discordproject.job.definition.list.Gangster
import org.beobma.mafia42discordproject.job.definition.list.Ghoul
import org.beobma.mafia42discordproject.job.definition.list.Judge
import org.beobma.mafia42discordproject.job.definition.list.Martyr
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import kotlin.reflect.KClass

class GovernmentAuthority : Ability, JobSpecificExtraAbility {
    override val name: String = "관권"
    override val description: String = "투표에서 찬성한 플레이어가 누군지 알수 있다."
    override val targetJob: List<KClass<out Job>> = listOf(Judge::class)
}