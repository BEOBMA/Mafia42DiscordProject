package org.beobma.mafia42discordproject.job.ability.general.definition.list.other

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.AssistanceCommonAbility
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Detective
import org.beobma.mafia42discordproject.job.definition.list.Hacker
import org.beobma.mafia42discordproject.job.definition.list.Mercenary
import org.beobma.mafia42discordproject.job.definition.list.Police
import org.beobma.mafia42discordproject.job.definition.list.Vigilante
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import org.beobma.mafia42discordproject.job.evil.list.Spy
import kotlin.reflect.KClass

class Resolute : Ability, JobSpecificExtraAbility {
    override val name: String = "결사"
    override val description: String = "마피아에게 처형 당하더라도 능력이 발동된다."
    override val targetJob: List<KClass<out Job>> = listOf(Vigilante::class, Mercenary::class)
}