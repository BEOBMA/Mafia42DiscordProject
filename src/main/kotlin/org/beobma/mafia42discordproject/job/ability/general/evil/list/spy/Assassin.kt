package org.beobma.mafia42discordproject.job.ability.general.evil.list.spy

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.AssistanceCommonAbility
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import org.beobma.mafia42discordproject.job.evil.list.Spy
import kotlin.reflect.KClass

class Assassin : Ability, JobSpecificExtraAbility {
    override val name: String = "자객"
    override val description: String = "마피아가 모두 죽고 혼자 남을 경우, 직후에 조사한 대상을 처형한다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(161).webp"
    override val targetJob: List<KClass<out Job>> = listOf(Spy::class)
}