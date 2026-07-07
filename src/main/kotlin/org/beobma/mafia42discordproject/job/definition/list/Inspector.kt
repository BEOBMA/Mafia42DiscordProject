package org.beobma.mafia42discordproject.job.definition.list

import dev.kord.common.entity.Snowflake
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.inspector.InspectorInvestigation
import org.beobma.mafia42discordproject.job.definition.Definition

class Inspector : Job(), Definition {
    override val name: String = "형사"
    override val description: String = "[수사] 밤에 한 명을 지목한다. 밤이 끝날 때 해당 플레이어가 같은 팀이었다면 직업을 알아내고 자신의 직업을 전송한다. (1회용)"
    override val jobImage: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/inspector_card.webp"
    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(InspectorInvestigation())

    var pendingInvestigationTargetId: Snowflake? = null
    var hasUsedInvestigation: Boolean = false
}
