package org.beobma.mafia42discordproject.job.ability.general.evil.list.hitman

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.HitMan
import org.beobma.mafia42discordproject.job.evil.list.Hostess
import kotlin.reflect.KClass

class Intuition : Ability, JobSpecificExtraAbility {
    override val name: String = "직감"
    override val description: String = "플레이어를 지목할 때마다 직업을 맞췄는지 여부를 알 수 있다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(169).webp"
    override val targetJob: List<KClass<out Job>> = listOf(HitMan::class)
}