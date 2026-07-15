package org.beobma.mafia42discordproject.job.ability.general.definition.list.magician

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Magician
import kotlin.reflect.KClass

class Assistant : Ability, JobSpecificExtraAbility {
    override val name: String = "조수"
    override val description: String = "'트릭' 능력이 발동하고 바꿔치기에 성공한 경우 자신이 바꿔치기한 대상에게 투표하는 찬반 투표 수가 2개로 집계된다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/magician_ability_assistant.webp"
    override val targetJob: List<KClass<out Job>> = listOf(Magician::class)
}
