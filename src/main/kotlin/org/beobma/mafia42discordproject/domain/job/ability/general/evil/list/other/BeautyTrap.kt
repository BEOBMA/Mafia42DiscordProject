package org.beobma.mafia42discordproject.job.ability.general.evil.list.other

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.Spy
import org.beobma.mafia42discordproject.job.evil.list.Swindler
import kotlin.reflect.KClass

class BeautyTrap : Ability, JobSpecificExtraAbility {
    override val name: String = "미인계"
    override val description: String = "시민팀 플레이어의 능력 대상이 될 경우, 해당 플레이어의 직업을 알아낼 수 있다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484622549949087796/5b91ab2026b43567.png?ex=69bee5fe&is=69bd947e&hm=7036668f765747b85d7500f3c25504fb527046b858a3235749806ca6d71d84af&"
    override val targetJob: List<KClass<out Job>> = listOf(Spy::class, Swindler::class)
}