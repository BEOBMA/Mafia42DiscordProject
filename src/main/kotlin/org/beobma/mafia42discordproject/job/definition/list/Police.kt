package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

class Police : Job(), Definition {
    override val name: String = "경찰"
    override val description: String = "[수색] 밤마다 플레이어 한 명을 조사하여 그 플레이어의 마피아 여부를 알아낼 수 있다."
    override val jobImage: String = "https://media.discordapp.net/attachments/1483977619258212392/1484548811341959208/chrome_WYFnipB5Ox.png?ex=69bea152&is=69bd4fd2&hm=036b6ff6a21169f03b591b9678a8db7ef520aade90b58af8c94e1d557e9083ab&=&format=webp&quality=lossless"
}