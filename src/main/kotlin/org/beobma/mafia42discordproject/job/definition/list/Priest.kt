package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

class Priest : Job(), Definition {
    override val name: String = "성직자"
    override val description: String = "[소생] 밤에 죽은 플레이어 한 명의 직업을 없애고 부활시킨다. (1회용)"
    override val jobImage: String = "https://media.discordapp.net/attachments/1483977619258212392/1484548884981223464/chrome_P6YWHcycfB.png?ex=69bea163&is=69bd4fe3&hm=7f029c42bde0a22da19d7f00790d21d0d43da19cb133a6cd414c4acc02eb73db&=&format=webp&quality=lossless"
}