package org.beobma.mafia42discordproject.job.definition.list

import dev.kord.common.entity.Snowflake
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.magician.Trick
import org.beobma.mafia42discordproject.job.definition.Definition

class Magician : Job(), Definition {
    override val name: String = "마술사"
    override val description: String = "[트릭] 투표 시간마다 생존한 다른 플레이어를 골라 자신이 최후의 반론에 오를 경우 그 플레이어를 대신 최후의 반론에 올린다."
    override val jobImage: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(69).webp"
    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(Trick())

    var trickTargetId: Snowflake? = null
    var trickSubstitutedTargetId: Snowflake? = null
    var hasUsedTrick: Boolean = false
}
