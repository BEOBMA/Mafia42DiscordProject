package org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.AssistanceCommonAbility
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import kotlin.reflect.KClass

class Sniper : Ability, JobSpecificExtraAbility {
    override val name: String = "저격"
    override val description: String = "전날 밤에 처형 대상을 처형하는 데에 실패했을 경우, 다음날 처형하는 대상의 모든 능력을 무시한다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484621701290524774/8c1e1bb54ffb8fe0.png?ex=69bee534&is=69bd93b4&hm=0fd7ef3e0992ef9008fe86ae707daaaab916cb9c454322b050b4ca56f899d424&"
    override val targetJob: List<KClass<out Job>> = listOf(Mafia::class)
}