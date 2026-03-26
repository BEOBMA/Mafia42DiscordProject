package org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.AssistanceCommonAbility
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import kotlin.reflect.KClass

class Outlaw : Ability, JobSpecificExtraAbility {
    override val name: String = "무법자"
    override val description: String = "경찰계열 직업을 처형 대상으로 지목할 경우 무조건 처형시킨다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484620752966647840/3abb4781af40f119.png?ex=69bee452&is=69bd92d2&hm=90b0d78b3beb2b72373e462eac67fe7bf8120713aff68d7a6a7485e4c71fd980&"
    override val targetJob: List<KClass<out Job>> = listOf(Mafia::class)
}