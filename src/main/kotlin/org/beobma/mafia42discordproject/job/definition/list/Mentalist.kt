package org.beobma.mafia42discordproject.job.definition.list

import dev.kord.common.entity.Snowflake
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.mentalist.MentalistAbility
import org.beobma.mafia42discordproject.job.definition.Definition

class Mentalist : Job(), Definition {
    override val name: String = "심리학자"
    override val description: String = "[관찰] 낮마다 다른 플레이어들의 대화를 선택해 서로 다른 팀인지 확인하고, 앞서 선택한 플레이어와 같은 팀이 나올 때까지 이를 반복한다."
    override val jobImage: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(70).webp"
    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(MentalistAbility())

    var initialObservationTargetId: Snowflake? = null
    var lastObservationTargetId: Snowflake? = null
    var isObservationResolvedToday: Boolean = false
}
