package org.beobma.mafia42discordproject.job.evil.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.evil.Evil

class Swindler : Job(), Evil {
    override val name: String = "짐승인간"
    override val description: String = "[사기] 게임 시작 시 시민 한 명의 정체를 알아내고 그 직업으로 변장한다.\n[교섭] 사기 대상 또는 사기꾼이 처형 능력 대상이 된 경우, 사기꾼은 처형되지 않으며 처형 여부와 관계 없이 마피아와 접선한다."
}