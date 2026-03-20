package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

class Fortuneteller : Job(), Definition {
    override val name: String = "점쟁이"
    override val description: String = "[운세] 밤마다 한 명을 선택한다. 선택한 플레이어 및 그와 다른 팀인 플레이어의 직업이 제시된다."
    override val jobImage: String = "https://media.discordapp.net/attachments/1483977619258212392/1484548930825093130/chrome_14PwFDtOIU.png?ex=69bea16e&is=69bd4fee&hm=f52dfb105e77bee9994d124cf41eb3df72aa4b9d90ee9f04d5c01b67ec8579cf&=&format=webp&quality=lossless"
}