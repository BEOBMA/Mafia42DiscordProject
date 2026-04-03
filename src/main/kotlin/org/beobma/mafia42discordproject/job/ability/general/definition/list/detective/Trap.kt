package org.beobma.mafia42discordproject.job.ability.general.definition.list.detective

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Detective
import org.beobma.mafia42discordproject.job.definition.list.Gangster
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import kotlin.reflect.KClass

class Trap : Ability, JobSpecificExtraAbility {
    override val name: String = "함정"
    override val description: String = "추리능력의 대상이 된 플레이어가 자신에게 능력을 사용한 경우 직업을 알아낸다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(132).webp"
    override val targetJob: List<KClass<out Job>> = listOf(Detective::class)
}