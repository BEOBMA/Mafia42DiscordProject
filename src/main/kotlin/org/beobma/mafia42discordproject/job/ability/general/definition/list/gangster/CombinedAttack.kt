package org.beobma.mafia42discordproject.job.ability.general.definition.list.gangster

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Gangster
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import kotlin.reflect.KClass

class CombinedAttack : Ability, JobSpecificExtraAbility {
    override val name: String = "연타"
    override val description: String = "전날 협박한 대상을 협박할 경우, 한 번 더 공갈 능력을 사용할 수 있다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(153).webp"
    override val targetJob: List<KClass<out Job>> = listOf(Gangster::class)
}