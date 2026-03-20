package org.beobma.mafia42discordproject.job.ability.general.evil.list.madscientist

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.AssistanceCommonAbility
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.MadScientist
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import org.beobma.mafia42discordproject.job.evil.list.Spy
import kotlin.reflect.KClass

class Analysis : Ability, JobSpecificExtraAbility {
    override val name: String = "분석"
    override val description: String = "투표로 사망했다가 부활한 후, 첫번째 낮에 투표했던 사람에게 투표할 경우 1회에 한해 2표로 취급된다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484618160236662905/0fd80e112750dc31.png?ex=69bee1e8&is=69bd9068&hm=703f9dc2e127dc2cfdcbf05b7315e55209ea9f8c69be9e0945ec0566f188e0b0&"
    override val targetJob: List<KClass<out Job>> = listOf(MadScientist::class)
}