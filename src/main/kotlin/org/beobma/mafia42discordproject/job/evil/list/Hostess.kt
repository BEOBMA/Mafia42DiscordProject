package org.beobma.mafia42discordproject.job.evil.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.hostess.HostessAbility
import org.beobma.mafia42discordproject.job.evil.Evil

class Hostess : Job(), Evil {
    override val name: String = "마담"
    override val description: String = "[유혹] 투표한 사람을 마담에게 유혹 당한 상태로 만든다. 유혹 당한 상태에서는 직업 능력을 사용할 수 없으며, 일시적으로 말을 할 수 없게 된다.\n[접대] 마피아를 유혹할 경우, 서로의 존재를 알아차리고 밤에 대화할 수 있게 된다."
    override val jobImage: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(67).webp"
    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(HostessAbility())

    var hasContactedMafia: Boolean = false
}
