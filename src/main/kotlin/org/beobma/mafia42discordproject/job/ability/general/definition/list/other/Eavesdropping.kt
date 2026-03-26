package org.beobma.mafia42discordproject.job.ability.general.definition.list.other

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.AssistanceCommonAbility
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Detective
import org.beobma.mafia42discordproject.job.definition.list.Hacker
import org.beobma.mafia42discordproject.job.definition.list.Police
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import org.beobma.mafia42discordproject.job.evil.list.Spy
import kotlin.reflect.KClass

class Eavesdropping : Ability, JobSpecificExtraAbility {
    override val name: String = "도청"
    override val description: String = "조사한 대상이 밤에 하는 말을 들을 수 있게 된다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(194).webp"
    override val targetJob: List<KClass<out Job>> = listOf(Police::class, Detective::class, Hacker::class)
}