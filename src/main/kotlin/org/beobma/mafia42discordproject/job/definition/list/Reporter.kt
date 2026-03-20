package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

class Reporter : Job(), Definition {
    override val name: String = "기자"
    override val description: String = "[특종] 밤에 한 명의 플레이어를 선택하여 직업을 알아내고 낮이 될 때 기사를 내어 모든 플레이어에게 해당 사실을 알린다.(1회용)\n[엠바고] 첫 번째 낮에는 기사를 낼 수 없다."
    override val jobImage: String = "https://media.discordapp.net/attachments/1483977619258212392/1484548930044956884/chrome_6drtDtyFhp.png?ex=69bea16e&is=69bd4fee&hm=843603fe6a41a5788dbd85a9784ae9b4f9f306b7d1d6afd3f3cba107cdb2e2dd&=&format=webp&quality=lossless"
}