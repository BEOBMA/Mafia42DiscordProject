package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

class Magician : Job(), Definition {
    override val name: String = "마술사"
    override val description: String = "[조수] '트릭' 능력이 발동되지 않은 상태에서 '트릭' 대상이 사망할 경우, 대상을 변경할 수 있다.\n[투시] 트릭에 성공했을 때 그 사람의 직업을 알아낸다."
    override val jobImage: String = "https://media.discordapp.net/attachments/1483977619258212392/1484548810834182174/chrome_WmPFDfyp8m.png?ex=69bea152&is=69bd4fd2&hm=4b831214350bc81e4cfde6f9e7f6096118029e198cdf0cfc97289ed37db6d63b&=&format=webp&quality=lossless"
}