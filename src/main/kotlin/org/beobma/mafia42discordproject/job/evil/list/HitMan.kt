package org.beobma.mafia42discordproject.job.evil.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.evil.Evil

class HitMan : Job(), Evil {
    override val name: String = "청부업자"
    override val description: String = "[청부] 두 번째 밤부터 공개적으로 능력이 사용된 대상을 제외한 시민 두 명을 지목하여 직업을 맞출 경우 둘 다 암살한다.\n[동업] 마피아를 지목할 경우 접선한다."
}