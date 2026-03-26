package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.administrator.AdministratorAbility
import org.beobma.mafia42discordproject.job.definition.Definition

class Administrator : Job(), Definition {
    override val name: String = "공무원"
    override val description: String = "[조회] 경찰 계열, 시민 직업을 제외한 시민팀 직업 중 하나를 지목하여 밤이 종료될 때, 그 직업을 가진 사람을 알아낸다."
    override val jobImage: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(63).webp"
    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(AdministratorAbility())

    var selectedInvestigationJobName: String? = null
    var investigationResultPlayerId: dev.kord.common.entity.Snowflake? = null
}
