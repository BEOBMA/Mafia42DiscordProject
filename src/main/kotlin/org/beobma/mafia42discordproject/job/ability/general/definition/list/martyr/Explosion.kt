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

class Explosion : Ability, JobSpecificExtraAbility {
    override val name: String = "유폭"
    override val description: String = "밤에 마피아 이외의 적에게 사망 한 경우라도, 해당 플레이어를 지목하고 있었다면 함께 사망한다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484609922954952886/e913d4a70b6336f1.png?ex=69beda3c&is=69bd88bc&hm=983da9fede9c09e0bad5ed6bcf5bb27b05c2775144b7300656a68e2d42c9dd3e&"
    override val targetJob: List<KClass<out Job>> = listOf(Martyr::class)
}