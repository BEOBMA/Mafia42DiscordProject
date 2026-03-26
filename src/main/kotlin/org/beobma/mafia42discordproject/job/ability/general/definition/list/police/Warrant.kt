package org.beobma.mafia42discordproject.job.ability.general.definition.list.police

import dev.kord.common.entity.Snowflake
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.definition.list.Police
import kotlin.reflect.KClass

class Warrant : Ability, JobSpecificExtraAbility {
    override val name: String = "영장"
    override val description: String = "이미 조사했던 플레이어를 한 번 더 조사할 경우 직업을 알아낸다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(159).webp"
    override val targetJob: List<KClass<out Job>> = listOf(Police::class)

    fun shouldRevealJob(targetId: Snowflake, searchedTargets: Set<Snowflake>): Boolean {
        return targetId in searchedTargets
    }
}
