package org.beobma.mafia42discordproject.job.definition.list

import dev.kord.common.entity.Snowflake
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.gangster.GangsterAbility
import org.beobma.mafia42discordproject.job.definition.Definition

class Gangster : Job(), Definition {
    override val name: String = "건달"
    override val description: String = "[공갈] 밤마다 플레이어 한 명을 선택하여 다음날 투표시 해당 플레이어의 투표권을 빼앗는다."
    override val jobImage: String = "https://media.discordapp.net/attachments/1483977619258212392/1484548811719180529/chrome_xK2jLc6k0R.png?ex=69bea152&is=69bd4fd2&hm=5235db6402310f882866f140f62a589b8bbd292e4bc0a02e20755019bddd2f19&=&format=webp&quality=lossless"
    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(GangsterAbility())

    var remainingThreatUsesTonight: Int = 1
    var threatenedTargetIdsTonight: MutableSet<Snowflake> = mutableSetOf()
    var threatenedTargetIdsLastNight: MutableSet<Snowflake> = mutableSetOf()

    fun prepareNightThreatSelection() {
        remainingThreatUsesTonight = 1
        threatenedTargetIdsTonight.clear()
    }

    fun finalizeNightThreatSelection() {
        threatenedTargetIdsLastNight = threatenedTargetIdsTonight.toMutableSet()
        threatenedTargetIdsTonight.clear()
    }
}
