package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

class Agent : Job(), Definition {
    override val name: String = "파파라치"
    override val description: String = "[이슈] 하루에 한 번 시민 팀이 다른 사람의 직업을 알아낼 경우, 그 정보를 공유받는다."
}