package org.beobma.mafia42discordproject.job.ability.general.definition.list.shaman

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import kotlin.reflect.KClass

class Manifesto : Ability, JobSpecificExtraAbility {
    override val name: String = "강령"
    override val description: String = "성불 상태인 플레이어의 대화를 들을 수 있다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(195).webp"
    override val targetJob: List<KClass<out Job>> = listOf(Shaman::class)
}