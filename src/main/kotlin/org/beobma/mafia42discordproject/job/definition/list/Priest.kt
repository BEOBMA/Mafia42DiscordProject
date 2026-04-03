package org.beobma.mafia42discordproject.job.definition.list

import dev.kord.common.entity.Snowflake
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.priest.Resurrection
import org.beobma.mafia42discordproject.job.definition.Definition

class Priest : Job(), Definition {
    override val name: String = "성직자"
    override val description: String = "[소생] 밤에 죽은 플레이어 한 명의 직업을 없애고 부활시킨다. (1회용)"
    override val jobImage: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(81).webp"
    var pendingResurrectionTargetId: Snowflake? = null
    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(Resurrection())
}
