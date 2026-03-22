package org.beobma.mafia42discordproject.job.definition.list

import dev.kord.common.entity.Snowflake
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.vigilante.VigilantePurgeDayAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.vigilante.VigilantePurgeNightAbility
import org.beobma.mafia42discordproject.job.definition.Definition

class Vigilante : Job(), Definition {
    override val name: String = "자경단원"
    override val description: String = "[숙청] 게임당 한 번, 낮에 플레이어 한 명을 선택해 마피아 여부를 알아낼 수 있으며 밤에 마피아를 처형할 수 있다. (1회용)"
    override val jobImage: String = "https://media.discordapp.net/attachments/1483977619258212392/1484548813208424628/MJqe5F4jFh.png?ex=69bea152&is=69bd4fd2&hm=8c40421fe328e859924252afccb61c966b24b582ddf16c2e8579ba3512322d20&=&format=webp&quality=lossless"
    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(VigilantePurgeDayAbility(), VigilantePurgeNightAbility())

    var fixedPurgeTargetId: Snowflake? = null
    var hasDiscoveredMafiaTarget: Boolean = false
    var discoveredMafiaDayCount: Int? = null
    var hasUsedNightPurge: Boolean = false
}