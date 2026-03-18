package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

class Magician : Job(), Definition {
    override val name: String = "마술사"
    override val description: String = "[조수] '트릭' 능력이 발동되지 않은 상태에서 '트릭' 대상이 사망할 경우, 대상을 변경할 수 있다.\n
            [투시] 트릭에 성공했을 때 그 사람의 직업을 알아낸다."
}