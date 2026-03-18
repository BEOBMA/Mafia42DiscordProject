package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

class Soldier : Job(), Definition {
    override val name: String = "군인"
    override val description: String = "[방탄] 처형 대상이 된 경우 한 차례 버텨낼 수 있다.\n[불침번] 마피아 팀에게 직업을 조사당할 경우 그 직업의 정체를 알 수 있고 조사의 부가적인 효과도 무효화 시킨다."
}