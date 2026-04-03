package org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.AssistanceCommonAbility
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import kotlin.reflect.KClass

class Concealment : Ability, JobSpecificExtraAbility {
    override val name: String = "은폐"
    override val description: String = "대상을 처형하는 데에 실패했을 경우, '조용한 밤'으로 진행된다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(174).webp"
    override val targetJob: List<KClass<out Job>> = listOf(Mafia::class)
}