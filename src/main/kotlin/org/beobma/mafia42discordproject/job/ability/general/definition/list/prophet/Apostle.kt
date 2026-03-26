package org.beobma.mafia42discordproject.job.ability.general.definition.list.prophet

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Prophet
import kotlin.reflect.KClass

class Apostle : Ability, JobSpecificExtraAbility {
    override val name: String = "사도"
    override val description: String = "시민팀 플레이어 중 마지막 생존자가 된다면 즉시 '계시' 능력이 발동된다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(152).webp"
    override val targetJob: List<KClass<out Job>> = listOf(Prophet::class)
}