package org.beobma.mafia42discordproject.job.ability.general.evil.list.hostess

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.Hostess
import kotlin.reflect.KClass

class Deception : Ability, JobSpecificExtraAbility {
    override val name: String = "현혹"
    override val description: String = "마담이 사망해도 유혹이 해제되지 않는다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(192).webp"
    override val targetJob: List<KClass<out Job>> = listOf(Hostess::class)
}