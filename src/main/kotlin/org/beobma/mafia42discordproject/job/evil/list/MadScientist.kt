package org.beobma.mafia42discordproject.job.evil.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.evil.Evil

class MadScientist : Job(), Evil {
    override val name: String = "과학자"
    override val description: String = "[재생] 사망할 경우, 다음날 밤에 부활한다. (1회용)\n[유착] 사망할 경우, 마피아와 접선한다."
}