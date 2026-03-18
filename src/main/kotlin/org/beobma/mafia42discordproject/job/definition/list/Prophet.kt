package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

class Prophet : Job(), Definition {
    override val name: String = "예언자"
    override val description: String = "[계시] 네번째 낮까지 생존 할 경우, 자신이 속한 팀이 승리한다."
}