package org.beobma.mafia42discordproject.job.ability.general.definition.list.other

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.AssistanceCommonAbility
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Detective
import org.beobma.mafia42discordproject.job.definition.list.Hacker
import org.beobma.mafia42discordproject.job.definition.list.Police
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import org.beobma.mafia42discordproject.job.evil.list.Spy
import kotlin.reflect.KClass

class Eavesdropping : Ability, JobSpecificExtraAbility {
    override val name: String = "도청"
    override val description: String = "조사한 대상이 밤에 하는 말을 들을 수 있게 된다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484611240121602118/dfa170ec3f52ca02.png?ex=69bedb76&is=69bd89f6&hm=56a11686cd80491e605cbd0958f8ebbc1285c4f366d3e31b1f810535216c6918&"
    override val targetJob: List<KClass<out Job>> = listOf(Police::class, Detective::class, Hacker::class)
}