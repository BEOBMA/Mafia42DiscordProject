package org.beobma.mafia42discordproject.job.definition.list

import dev.kord.common.entity.Snowflake
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.hypnotist.HypnotizeAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.hypnotist.ReleaseHypnosisAbility
import org.beobma.mafia42discordproject.job.definition.Definition

class Hypnotist : Job(), Definition {
    override val name: String = "최면술사"
    override val description: String = "[최면] 하루에 한 번, 밤에 다른 플레이어에게 최면을 걸거나 낮에 최면에 걸린 플레이어들을 깨워 시민팀이 아니라면 직업을 알아낸다."
    override val jobImage: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(79).webp"
    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(
        HypnotizeAbility(),
        ReleaseHypnosisAbility()
    )

    val hypnotizedTargetIds: MutableSet<Snowflake> = linkedSetOf()
    var selectedTargetIdTonight: Snowflake? = null
    var blockedNightsRemaining: Int = 0
}
