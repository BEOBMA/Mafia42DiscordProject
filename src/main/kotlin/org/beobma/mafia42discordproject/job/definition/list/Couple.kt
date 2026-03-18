package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

class Couple : Job(), Definition {
    override val name: String = "연인"
    override val description: String = "[연애] 밤에 다른 연인과 서로 대화가 가능하다.\n[희생] 연인 두 명이 모두 생존하고 있을 때, 연인 한명이 마피아에게 지목당할 경우 다른 연인이 대신 죽게 된다."
}