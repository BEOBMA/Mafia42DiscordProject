package org.beobma.mafia42discordproject.job.evil.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.evil.Evil

class Witch : Job(), Evil {
    override val name: String = "마녀"
    override val description: String = "[저주] 밤마다 플레이어 한 명의 닉네임을 적어 다음날 낮이 완전히 종료될 때까지 개구리로 변신시킨다. 마피아를 저주할 경우, 마피아와 접선한다."
    override val jobImage: String = "https://media.discordapp.net/attachments/1483977619258212392/1484548919542157374/chrome_HmLMZmKGIP.png?ex=69bea16c&is=69bd4fec&hm=5cb95a3b628968b36894ac984a08046970e331209d03b61f3144fe09c1f4cf41&=&format=webp&quality=lossless"
}