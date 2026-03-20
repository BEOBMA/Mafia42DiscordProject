package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

class Politician : Job(), Definition {
    override val name: String = "정치인"
    override val description: String = "[처세] 플레이어간의 투표로 처형당하지 않는다.\n[논객] 정치인의 투표권은 두 표로 인정된다."
    override val jobImage: String = "https://media.discordapp.net/attachments/1483977619258212392/1484548917830877264/chrome_CW06WmZ47i.png?ex=69bea16b&is=69bd4feb&hm=8ff9ad3e3d8b1e15fef8803188b17e2db273f2fbcfe6960958c2dd6a8f1ce835&=&format=webp&quality=lossless"
}