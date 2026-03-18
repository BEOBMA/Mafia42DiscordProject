package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

class Agent : Job(), Definition {
    override val name: String = "공무원"
    override val description: String = "[조회] 경찰 계열, 시민 직업을 제외한 시민팀 직업 중 하나를 지목하여 밤이 종료될 때, 그 직업을 가진 사람을 알아낸다."
}