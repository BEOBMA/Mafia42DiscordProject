package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

class Paparazzi : Job(), Definition {
    override val name: String = "파파라치"
    override val description: String = "[이슈] 하루에 한 번 시민 팀이 다른 사람의 직업을 알아낼 경우, 그 정보를 공유받는다."
    override val jobImage: String = "https://media.discordapp.net/attachments/1483977619258212392/1484548812092735619/chrome_y5XepLMXAr.png?ex=69bea152&is=69bd4fd2&hm=d27dd575c773c638ac528a0a78a16847aebf9fc972827dd53875a5282b976959&=&format=webp&quality=lossless"
}