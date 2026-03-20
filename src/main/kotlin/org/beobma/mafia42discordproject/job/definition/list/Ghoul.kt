package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

class Ghoul : Job(), Definition {
    override val name: String = "도굴꾼"
    override val description: String = "[도굴] 첫 번째 밤에 마피아팀에게 살해당한 사람의 직업을 얻으며, 도굴당한 대상에게 도굴꾼이 누구인지 알려지게 된다.\n[약탈] 도굴에 성공한 경우, 도굴당한 플레이어를 시민 또는 악인으로 만든다."
    override val jobImage: String = "https://media.discordapp.net/attachments/1483977619258212392/1484548883655823541/chrome_nkKiBF8mzC.png?ex=69bea163&is=69bd4fe3&hm=2383b090605f8939570994ce47239736fd8fa51555afd00972025f1ba5019b86&=&format=webp&quality=lossless"
}