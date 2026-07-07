package org.beobma.mafia42discordproject.job.ability.general.evil.list.beastman

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.Beastman
import kotlin.reflect.KClass

class Barbarism : Ability, JobSpecificExtraAbility {
    override val name: String = "야만성"
    override val description: String = "마피아에게 공격받은 경우 접선한다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/beastman_ability_4.webp"
    override val targetJob: List<KClass<out Job>> = listOf(Beastman::class)
}
