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
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(116).webp"
    override val targetJob: List<KClass<out Job>> = listOf(Mafia::class)
}