package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

class Detective : Job(), Definition {
    override val name: String = "사립탐정"
    override val description: String = "[추리] 밤마다 플레이어 한 명을 선택하여 그 플레이어가 누구에게 능력을 사용하였는지 알아낼 수 있다."
    override val jobImage: String = "https://media.discordapp.net/attachments/1483977619258212392/1484548884293488660/chrome_oZ5PTqg1dd.png?ex=69bea163&is=69bd4fe3&hm=03ac62987e3a7f6e0e495b55f056c3a6bf91270636f59ec03dccf2c39ac3d57e&=&format=webp&quality=lossless"
}