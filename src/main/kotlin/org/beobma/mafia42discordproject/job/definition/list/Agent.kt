package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

class Agent : Job(), Definition {
    override val name: String = "요원"
    override val description: String = "[공작] 낮마다 지령을 받아 시민 한 명의 직업을 알아낸다."
    override val jobImage: String = "https://media.discordapp.net/attachments/1483977619258212392/1484548814097350666/UOSGMLjboF.png?ex=69bea152&is=69bd4fd2&hm=4d387171893e1ec6f7eedcefb430bffe144ac1e00785fa31bc60b08a84c79b18&=&format=webp&quality=lossless"
}