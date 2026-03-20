package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

class Martyr : Job(), Definition {
    override val name: String = "테러리스트"
    override val description: String = "[자폭] 마피아를 지목하고 있는 상태에서 마피아에게 처형당할 때 대상과 함께 사망한다.\n[산화] 투표로 인해 처형될 때 적 팀을 지목했다면 대상과 함께 사망한다."
    override val jobImage: String = "https://media.discordapp.net/attachments/1483977619258212392/1484548918380466266/chrome_D3Bw9Jy4wO.png?ex=69bea16b&is=69bd4feb&hm=ecc19a6987d40a0d5ba6dfed00bf5d539527ec2d7137ac30a9eaa2df131173e5&=&format=webp&quality=lossless"
}