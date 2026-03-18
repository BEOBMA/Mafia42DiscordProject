package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

class Agent : Job(), Definition {
    override val name: String = "심리학자"
    override val description: String = "[관찰] 낮마다 다른 플레이어들의 대화를 선택해 서로 다른 팀인지 확인하고, 앞서 선택한 플레이어와 같은 팀이 나올 때까지 이를 반복한다."
}