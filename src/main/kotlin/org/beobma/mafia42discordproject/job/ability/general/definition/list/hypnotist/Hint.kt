package org.beobma.mafia42discordproject.job.ability.general.definition.list.hypnotist

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.Hypocrisy
import org.beobma.mafia42discordproject.job.definition.list.Detective
import org.beobma.mafia42discordproject.job.definition.list.Gangster
import org.beobma.mafia42discordproject.job.definition.list.Ghoul
import org.beobma.mafia42discordproject.job.definition.list.Hypnotist
import org.beobma.mafia42discordproject.job.definition.list.Martyr
import org.beobma.mafia42discordproject.job.definition.list.Nurse
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import kotlin.reflect.KClass

class Hint : Ability, JobSpecificExtraAbility {
    override val name: String = "암시"
    override val description: String = "최면 대상이 사망할 경우, 직업을 알아낸다."
    override val targetJob: List<KClass<out Job>> = listOf(Hypnotist::class)
}