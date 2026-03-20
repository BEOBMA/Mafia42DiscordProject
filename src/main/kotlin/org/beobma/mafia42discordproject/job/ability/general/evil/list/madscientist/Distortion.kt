package org.beobma.mafia42discordproject.job.ability.general.evil.list.madscientist

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.AssistanceCommonAbility
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.MadScientist
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import org.beobma.mafia42discordproject.job.evil.list.Spy
import kotlin.reflect.KClass

class Distortion : Ability, JobSpecificExtraAbility {
    override val name: String = "왜곡"
    override val description: String = "재생 능력을 사용한 다음날에 그 사실이 알려진다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484618453079036035/b94f286daa01358c.png?ex=69bee22e&is=69bd90ae&hm=eabbc21bd62d7a205ed780f7e68c9d2dd2e9d0b89cfe5d4983c74682e90f0a7a&"
    override val targetJob: List<KClass<out Job>> = listOf(MadScientist::class)
}