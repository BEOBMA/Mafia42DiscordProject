package org.beobma.mafia42discordproject.job.evil.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.evil.Evil

class Beastman : Job(), Evil {
    override val name: String = "짐승인간"
    override val description: String = "[갈망] 밤에 선택한 플레이어에게 표식을 새긴다. 표식이 새겨진 대상이 마피아에게 선택되면 마피아에게 길들여진다. 길들여진 후 플레이어를 제거할 수 있다.\n[민첩] 마피아의 공격으로부터 죽지 않는다."
}