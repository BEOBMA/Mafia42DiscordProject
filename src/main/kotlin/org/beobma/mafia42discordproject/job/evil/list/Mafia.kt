package org.beobma.mafia42discordproject.job.evil.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.MafiaAbility
import org.beobma.mafia42discordproject.job.evil.Evil

class Mafia : Job(), Evil {
    override val name: String = "마피아"
    override val description: String = "[처형] 밤마다 한 명의 플레이어를 죽일 수 있으며 마피아끼리 대화가 가능하다."
    override val jobImage: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1483984932777885760/2026-03-19_092429.png?ex=69bc942b&is=69bb42ab&hm=05b77306328eca5f2aacb1d0651edf2281072deb01c304cd67d9e36dd561e71f&"

    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(MafiaAbility())
}
