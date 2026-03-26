package org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.AssistanceCommonAbility
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import kotlin.reflect.KClass

class Poisoning : Ability, JobSpecificExtraAbility {
    override val name: String = "독살"
    override val description: String = "밤에 시민팀 처형에 실패한 경우, 중독 상태로 만들어 하루 뒤에 죽게 한다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(165).webp"
    override val targetJob: List<KClass<out Job>> = listOf(Mafia::class)
}