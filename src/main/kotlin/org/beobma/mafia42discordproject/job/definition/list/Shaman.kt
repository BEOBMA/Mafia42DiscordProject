package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

class Shaman : Job(), Definition {
    override val name: String = "영매"
    override val description: String = "[접신] 죽은 사람이 하는 채팅을 들을 수 있으며, 밤에 죽은 사람과 대화를 할 수 있다.\n[성불] 밤마다 죽은 사람 한명을 선택하여 그 사람의 직업을 알아내고 성불 상태로 만든다."
    override val jobImage: String = "https://media.discordapp.net/attachments/1483977619258212392/1484548913766858842/chrome_28VHFuu5Px.png?ex=69bea16a&is=69bd4fea&hm=58b22ea2eb02f9222846b93ff522d552e1453997cacf8c42a36fbf1e10bffd2a&=&format=webp&quality=lossless"
}