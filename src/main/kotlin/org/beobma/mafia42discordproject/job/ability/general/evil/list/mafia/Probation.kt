package org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.AssistanceCommonAbility
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import kotlin.reflect.KClass

class Probation : Ability, JobSpecificExtraAbility {
    override val name: String = "수습"
    override val description: String = "처형한 대상의 직업을 알 수 있으며, 처형한 대상이 시민 팀인 경우 직업을 시민으로 만든다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484621375250628760/ac7c13a565e340ba.png?ex=69bee4e6&is=69bd9366&hm=974da89e905d94b1a3ee2b2505c048b82521080c10d8451895bef41295b5faa2&"
    override val targetJob: List<KClass<out Job>> = listOf(Mafia::class)
}