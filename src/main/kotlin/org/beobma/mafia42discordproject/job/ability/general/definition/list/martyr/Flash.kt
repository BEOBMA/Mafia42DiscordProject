package org.beobma.mafia42discordproject.job.ability.general.definition.list.martyr

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Detective
import org.beobma.mafia42discordproject.job.definition.list.Gangster
import org.beobma.mafia42discordproject.job.definition.list.Ghoul
import org.beobma.mafia42discordproject.job.definition.list.Martyr
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import kotlin.reflect.KClass

class Flash : Ability, JobSpecificExtraAbility {
    override val name: String = "섬광"
    override val description: String = "밤에 자폭 능력을 성공할 경우, 자기자신은 생존한다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484609922954952886/e913d4a70b6336f1.png?ex=69beda3c&is=69bd88bc&hm=983da9fede9c09e0bad5ed6bcf5bb27b05c2775144b7300656a68e2d42c9dd3e&"
    override val targetJob: List<KClass<out Job>> = listOf(Martyr::class)
}