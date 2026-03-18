package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

class Judge : Job(), Definition {
    override val name: String = "판사"
    override val description: String = "[선고] 찬성/반대의 결과가 자신의 선택과 다를 경우, 모습을 드러내며, 판사의 선택에 의해 찬성/반대가 결정된다. 모습을 드러낸 이후, 모든 투표의 결과는 판사에 의해서만 결정된다."
}