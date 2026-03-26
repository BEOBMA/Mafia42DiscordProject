package org.beobma.mafia42discordproject.job.ability.general.definition.list.ghoul

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Detective
import org.beobma.mafia42discordproject.job.definition.list.Gangster
import org.beobma.mafia42discordproject.job.definition.list.Ghoul
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import kotlin.reflect.KClass

class Succession : Ability, JobSpecificExtraAbility {
    override val name: String = "계승"
    override val description: String = "도굴한 플레이어의 모든 고유능력을 가져온다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484608248953704448/9b24276c080fc5f9.png?ex=69bed8ad&is=69bd872d&hm=921fa9ac145e672d4a6d7dd978295aeab9cd07a75a89130adbfc640bd7f705be&"
    override val targetJob: List<KClass<out Job>> = listOf(Ghoul::class)
}