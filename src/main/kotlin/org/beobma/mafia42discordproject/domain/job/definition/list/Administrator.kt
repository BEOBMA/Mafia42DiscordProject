package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.administrator.AdministratorAbility
import org.beobma.mafia42discordproject.job.definition.Definition

class Administrator : Job(), Definition {
    override val name: String = "공무원"
    override val description: String = "[조회] 경찰 계열, 시민 직업을 제외한 시민팀 직업 중 하나를 지목하여 밤이 종료될 때, 그 직업을 가진 사람을 알아낸다."
    override val jobImage: String = "https://media.discordapp.net/attachments/1483977619258212392/1484548929046712373/chrome_5XGcWog6yW.png?ex=69bea16e&is=69bd4fee&hm=a98ce5add3f05c0ad307585043d78cc2f63ae9d542fad110f67cf8c6023e52af&=&format=webp&quality=lossless"
    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(AdministratorAbility())

    var selectedInvestigationJobName: String? = null
    var investigationResultPlayerId: dev.kord.common.entity.Snowflake? = null
}
