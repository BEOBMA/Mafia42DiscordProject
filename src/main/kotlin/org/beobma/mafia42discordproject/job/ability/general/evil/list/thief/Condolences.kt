package org.beobma.mafia42discordproject.job.ability.general.evil.list.thief

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.Thief
import kotlin.reflect.KClass

class Condolences : Ability, JobSpecificExtraAbility {
    override val name: String = "조문"
    override val description: String = "최근 하루 내에 죽은 사람에게도 '도벽' 능력을 사용할 수 있다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484623664736370728/201b7da7130e8c29.png?ex=69bee708&is=69bd9588&hm=bfc556486d0c7095f96e93275bacd7e014cbf88612f0e02f229b0243a107d9da&"
    override val targetJob: List<KClass<out Job>> = listOf(Thief::class)
}