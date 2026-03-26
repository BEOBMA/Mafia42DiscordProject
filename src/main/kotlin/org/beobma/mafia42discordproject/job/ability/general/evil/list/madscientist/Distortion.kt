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
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(189).webp"
    override val targetJob: List<KClass<out Job>> = listOf(MadScientist::class)
}