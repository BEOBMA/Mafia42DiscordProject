package org.beobma.mafia42discordproject.job.evil.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.evil.Evil

class Godfather : Job(), Evil {
    override val name: String = "대부"
    override val description: String = "[배후] 세번째 밤이 될 때 마피아와 접선한다.\n[말살] 접선 후 밤마다 다른 플레이어의 능력을 무시하고 처형할 수 있다."
    override val jobImage: String = "https://media.discordapp.net/attachments/1483977619258212392/1484548883211358218/chrome_ngMcHTACqh.png?ex=69bea163&is=69bd4fe3&hm=34a19cb63b0ebbbc7cac88e3ec3ec491101ba86d85357310bd8033f5792c3669&=&format=webp&quality=lossless"
}