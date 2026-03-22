package org.beobma.mafia42discordproject.job.evil.list

import dev.kord.common.entity.Snowflake
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.spy.SpyAbility
import org.beobma.mafia42discordproject.job.evil.Evil

class Spy : Job(), Evil {
    override val name: String = "스파이"
    override val description: String = "[첩보] 밤마다 플레이어 한 명을 선택하여 직업을 알아낼 수 있다. 마피아와 접선할 경우, 한 번 더 능력을 사용할 수 있다.\n[접선] 밤에 선택한 플레이어가 마피아라면 접선한다."
    override val jobImage: String = "https://media.discordapp.net/attachments/1483977619258212392/1484548917101330564/chrome_aLhXZDrYQL.png?ex=69bea16b&is=69bd4feb&hm=f3971b24e7a7ec2077ea899d9779e5341b822c7d768c1e2b01206521e5549576&=&format=webp&quality=lossless"
    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(SpyAbility())

    var hasContactedMafia: Boolean = false
    var remainingIntelUsesTonight: Int = 1
    var lastInvestigatedTargetId: Snowflake? = null
    var hasTriggeredAssassin: Boolean = false
}
