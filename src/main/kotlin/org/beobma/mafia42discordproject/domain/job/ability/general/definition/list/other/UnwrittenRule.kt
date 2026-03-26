package org.beobma.mafia42discordproject.job.ability.general.definition.list.other

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.AssistanceCommonAbility
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Detective
import org.beobma.mafia42discordproject.job.definition.list.Hacker
import org.beobma.mafia42discordproject.job.definition.list.Judge
import org.beobma.mafia42discordproject.job.definition.list.Police
import org.beobma.mafia42discordproject.job.definition.list.Politician
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import org.beobma.mafia42discordproject.job.evil.list.Spy
import kotlin.reflect.KClass

class UnwrittenRule : Ability, JobSpecificExtraAbility {
    override val name: String = "불문율"
    override val description: String = "자신의 능력으로 정체가 처음으로 공개된 날 밤에는 자신을 능력 사용의 대상으로 지목할 수 없게 된다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484611865853300871/fc204241b21858c4.png?ex=69bedc0b&is=69bd8a8b&hm=b451d38cb41f79fcae222bdf84ffe0b7135b1f5cbfd1af98444c765226c68cc2&"
    override val targetJob: List<KClass<out Job>> = listOf(Politician::class, Judge::class)
}