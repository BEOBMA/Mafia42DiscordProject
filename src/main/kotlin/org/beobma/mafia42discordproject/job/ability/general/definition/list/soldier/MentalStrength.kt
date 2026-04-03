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
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(199).webp"
    override val targetJob: List<KClass<out Job>> = listOf(Soldier::class)
}