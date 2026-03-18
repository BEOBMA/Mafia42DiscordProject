package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

class Nurse : Job(), Definition {
    override val name: String = "간호사"
    override val description: String = "[처방] 밤마다 플레이어 한 명을 선택해 의사인지 조사하고, 의사 또는 자신이 상대방에게 능력을 사용한 경우 접선한다. 접선 상태에서 의사의 치료 능력은 모든 부가 능력을 무시하고 성공하며, 의사가 사망할 시 치료 능력을 사용할 수 있다."
}