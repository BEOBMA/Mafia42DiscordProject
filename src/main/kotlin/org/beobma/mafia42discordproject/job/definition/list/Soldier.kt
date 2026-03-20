package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

class Soldier : Job(), Definition {
    override val name: String = "군인"
    override val description: String = "[방탄] 처형 대상이 된 경우 한 차례 버텨낼 수 있다.\n[불침번] 마피아 팀에게 직업을 조사당할 경우 그 직업의 정체를 알 수 있고 조사의 부가적인 효과도 무효화 시킨다."
    override val jobImage: String = "https://media.discordapp.net/attachments/1483977619258212392/1484548809651388616/chrome_SOQKAK9pFV.png?ex=69bea151&is=69bd4fd1&hm=d2f9073fbe2c415c6c78805cdf7e89dd98e9dd6709b291dc89a77ce7c43d43e5&=&format=webp&quality=lossless"
}