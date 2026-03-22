package org.beobma.mafia42discordproject.job.evil.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.hostess.HostessAbility
import org.beobma.mafia42discordproject.job.evil.Evil

class Hostess : Job(), Evil {
    override val name: String = "마담"
    override val description: String = "[유혹] 투표한 사람을 마담에게 유혹 당한 상태로 만든다. 유혹 당한 상태에서는 직업 능력을 사용할 수 없으며, 일시적으로 말을 할 수 없게 된다.\n[접대] 마피아를 유혹할 경우, 서로의 존재를 알아차리고 밤에 대화할 수 있게 된다."
    override val jobImage: String = "https://media.discordapp.net/attachments/1483977619258212392/1484548920192532540/chrome_icHM6NWK4r.png?ex=69bea16c&is=69bd4fec&hm=178bf903378b5ab484d381f6fb7a8641c6043dd4024eac35af83980e4b06b505&=&format=webp&quality=lossless"
    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(HostessAbility())

    var hasContactedMafia: Boolean = false
}
