package org.beobma.mafia42discordproject.job.evil.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.evil.Evil

class Thief : Job(), Evil {
    override val name: String = "도둑"
    override val description: String = "[도벽] 투표시간마다 원하는 플레이어의 표식을 클릭해 그 사람의 고유능력을 밤까지 사용할 수 있다.\n[교련] 마피아 직업을 훔칠 경우, 마피아와 접선한다."
}