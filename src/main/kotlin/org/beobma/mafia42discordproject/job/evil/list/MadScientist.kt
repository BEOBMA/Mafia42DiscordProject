package org.beobma.mafia42discordproject.job.evil.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.evil.Evil

class MadScientist : Job(), Evil {
    override val name: String = "과학자"
    override val description: String = "[재생] 사망할 경우, 다음날 밤에 부활한다. (1회용)\n[유착] 사망할 경우, 마피아와 접선한다."
    override val jobImage: String = "https://media.discordapp.net/attachments/1483977619258212392/1484548813757616189/N9wFfyFx6O.png?ex=69bea152&is=69bd4fd2&hm=6df25e387fc8482ed056295c3fcef4de31df298cbf549134c8c5bc476fcf3694&=&format=webp&quality=lossless"
}